/**
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.kafka.common.security.scram;

import org.apache.kafka.common.errors.IllegalSaslStateException;
import org.apache.kafka.common.security.scram.ScramMessages.ClientFinalMessage;
import org.apache.kafka.common.security.scram.ScramMessages.ClientFirstMessage;
import org.apache.kafka.common.security.scram.ScramMessages.ServerFinalMessage;
import org.apache.kafka.common.security.scram.ScramMessages.ServerFirstMessage;
import org.apache.kafka.common.utils.ArraysUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.security.auth.callback.Callback;
import javax.security.auth.callback.CallbackHandler;
import javax.security.auth.callback.NameCallback;
import javax.security.auth.callback.UnsupportedCallbackException;
import javax.security.sasl.SaslException;
import javax.security.sasl.SaslServer;
import javax.security.sasl.SaslServerFactory;
import java.io.IOException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;
import java.util.Collection;
import java.util.Map;

/**
 * SaslServer implementation for SASL/SCRAM. This server is configured with a callback
 * handler for integration with a credential manager. Kafka brokers provide callbacks
 * based on a Zookeeper-based password store.
 *
 * @see <a href="https://tools.ietf.org/html/rfc5802">RFC 5802</a>
 */
public class ScramSaslServer implements SaslServer {

    private static final Logger log = LoggerFactory.getLogger(ScramSaslServer.class);

    enum State {
        RECEIVE_CLIENT_FIRST_MESSAGE,
        RECEIVE_CLIENT_FINAL_MESSAGE,
        COMPLETE,
        FAILED
    };

    private final ScramMechanism mechanism;
    private final ScramFormatter formatter;
    private final CallbackHandler callbackHandler;
    private State state;
    private String username;
    private ClientFirstMessage clientFirstMessage;
    private ServerFirstMessage serverFirstMessage;
    private String serverNonce;
    private ScramCredential scramCredential;

    public ScramSaslServer(ScramMechanism mechanism, Map<String, ?> props, CallbackHandler callbackHandler) throws NoSuchAlgorithmException {
        this.mechanism = mechanism;
        this.formatter = new ScramFormatter(mechanism);
        this.callbackHandler = callbackHandler;
        setState(State.RECEIVE_CLIENT_FIRST_MESSAGE);
    }

    public byte[] evaluateResponse(byte[] response) throws SaslException {
        try {
            switch (state) {
                case RECEIVE_CLIENT_FIRST_MESSAGE:
                    this.clientFirstMessage = new ClientFirstMessage(response);
                    serverNonce = formatter.secureRandomString();
                    try {
                        String saslName = clientFirstMessage.saslName();
                        this.username = formatter.username(saslName);
                        NameCallback nameCallback = new NameCallback("username", username);
                        ScramCredentialCallback credentialCallback = new ScramCredentialCallback();
                        callbackHandler.handle(new Callback[]{nameCallback, credentialCallback});
                        this.scramCredential = credentialCallback.scramCredential();
                        if (scramCredential == null)
                            throw new SaslException("Authentication failed: Invalid user credentials");
                        if (scramCredential.iterations() < mechanism.minIterations())
                            throw new SaslException("Iterations " + scramCredential.iterations() +  " is less than the minimum " + mechanism.minIterations() + " for " + mechanism);
                        this.serverFirstMessage = new ServerFirstMessage(clientFirstMessage.nonce(),
                                serverNonce,
                                scramCredential.salt(),
                                scramCredential.iterations());
                        setState(State.RECEIVE_CLIENT_FINAL_MESSAGE);
                        return serverFirstMessage.toBytes();
                    } catch (Exception e) {
                        throw new SaslException("Authentication failed: Credentials could not be obtained", e);
                    }

                case RECEIVE_CLIENT_FINAL_MESSAGE:
                    try {
                        ClientFinalMessage clientFinalMessage = new ClientFinalMessage(response);
                        verifyClientProof(clientFinalMessage);
                        byte[] serverKey = scramCredential.serverKey();
                        byte[] serverSignature = formatter.serverSignature(serverKey, clientFirstMessage, serverFirstMessage, clientFinalMessage);
                        ServerFinalMessage serverFinalMessage = new ServerFinalMessage(null, serverSignature);
                        setState(State.COMPLETE);
                        return serverFinalMessage.toBytes();
                    } catch (InvalidKeyException e) {
                        throw new SaslException("Authentication failed: Invalid client final message", e);
                    }

                default:
                    throw new IllegalSaslStateException("Unexpected challenge in Sasl server state " + state);
            }
        } catch (SaslException e) {
            setState(State.FAILED);
            throw e;
        }
    }

    public String getAuthorizationID() {
        if (!isComplete())
            throw new IllegalStateException("Authentication exchange has not completed");
        String authzId = clientFirstMessage.authorizationId();
        return authzId == null || authzId.length() == 0 ? username : authzId;
    }

    public String getMechanismName() {
        return mechanism.mechanismName();
    }

    public Object getNegotiatedProperty(String propName) {
        if (!isComplete())
            throw new IllegalStateException("Authentication exchange has not completed");
        return null;
    }

    public boolean isComplete() {
        return state == State.COMPLETE;
    }

    public byte[] unwrap(byte[] incoming, int offset, int len) throws SaslException {
        if (!isComplete())
            throw new IllegalStateException("Authentication exchange has not completed");
        return ArraysUtil.copyOfRange(incoming, offset, offset + len);
    }

    public byte[] wrap(byte[] outgoing, int offset, int len) throws SaslException {
        if (!isComplete())
            throw new IllegalStateException("Authentication exchange has not completed");
        return ArraysUtil.copyOfRange(outgoing, offset, offset + len);
    }

    public void dispose() throws SaslException {
    }

    private void setState(State state) {
        log.debug("Setting SASL/{} server state to {}", mechanism, state);
        this.state = state;
    }

    private void verifyClientProof(ClientFinalMessage clientFinalMessage) throws SaslException {
        try {
            byte[] expectedStoredKey = scramCredential.storedKey();
            byte[] clientSignature = formatter.clientSignature(expectedStoredKey, clientFirstMessage, serverFirstMessage, clientFinalMessage);
            byte[] computedStoredKey = formatter.storedKey(clientSignature, clientFinalMessage.proof());
            if (!Arrays.equals(computedStoredKey, expectedStoredKey))
                throw new SaslException("Invalid client credentials");
        } catch (InvalidKeyException e) {
            throw new SaslException("Sasl client verification failed", e);
        }
    }

    public static class ScramSaslServerFactory implements SaslServerFactory {

        public SaslServer createSaslServer(String mechanism, String protocol, String serverName, Map<String, ?> props, CallbackHandler cbh)
            throws SaslException {

            if (!ScramMechanism.isScram(mechanism)) {
                throw new SaslException(String.format("Requested mechanism '%s' is not supported. Supported mechanisms are '%s'.",
                        mechanism, ScramMechanism.mechanismNames()));
            }
            try {
                return new ScramSaslServer(ScramMechanism.forMechanismName(mechanism), props, cbh);
            } catch (NoSuchAlgorithmException e) {
                throw new SaslException("Hash algorithm not supported for mechanism " + mechanism, e);
            }
        }

        public String[] getMechanismNames(Map<String, ?> props) {
            Collection<String> mechanisms = ScramMechanism.mechanismNames();
            return mechanisms.toArray(new String[mechanisms.size()]);
        }
    }
}
