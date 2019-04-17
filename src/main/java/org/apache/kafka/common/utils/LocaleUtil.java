package org.apache.kafka.common.utils;

import sun.security.action.GetPropertyAction;
import sun.util.locale.*;

import java.io.*;
import java.security.AccessController;
import java.text.MessageFormat;
import java.util.Collections;
import java.util.PropertyPermission;
import java.util.Set;

public class LocaleUtil implements Cloneable, Serializable {
    static private final LocaleUtil.Cache LOCALECACHE = new LocaleUtil.Cache();

    /** Useful constant for language.
     */
    static public final LocaleUtil ENGLISH = createConstant("en", "");

    /** Useful constant for language.
     */
    static public final LocaleUtil FRENCH = createConstant("fr", "");

    /** Useful constant for language.
     */
    static public final LocaleUtil GERMAN = createConstant("de", "");

    /** Useful constant for language.
     */
    static public final LocaleUtil ITALIAN = createConstant("it", "");

    /** Useful constant for language.
     */
    static public final LocaleUtil JAPANESE = createConstant("ja", "");

    /** Useful constant for language.
     */
    static public final LocaleUtil KOREAN = createConstant("ko", "");

    /** Useful constant for language.
     */
    static public final LocaleUtil CHINESE = createConstant("zh", "");

    /** Useful constant for language.
     */
    static public final LocaleUtil SIMPLIFIED_CHINESE = createConstant("zh", "CN");

    /** Useful constant for language.
     */
    static public final LocaleUtil TRADITIONAL_CHINESE = createConstant("zh", "TW");

    /** Useful constant for country.
     */
    static public final LocaleUtil FRANCE = createConstant("fr", "FR");

    /** Useful constant for country.
     */
    static public final LocaleUtil GERMANY = createConstant("de", "DE");

    /** Useful constant for country.
     */
    static public final LocaleUtil ITALY = createConstant("it", "IT");

    /** Useful constant for country.
     */
    static public final LocaleUtil JAPAN = createConstant("ja", "JP");

    /** Useful constant for country.
     */
    static public final LocaleUtil KOREA = createConstant("ko", "KR");

    /** Useful constant for country.
     */
    static public final LocaleUtil CHINA = SIMPLIFIED_CHINESE;

    /** Useful constant for country.
     */
    static public final LocaleUtil PRC = SIMPLIFIED_CHINESE;

    /** Useful constant for country.
     */
    static public final LocaleUtil TAIWAN = TRADITIONAL_CHINESE;

    /** Useful constant for country.
     */
    static public final LocaleUtil UK = createConstant("en", "GB");

    /** Useful constant for country.
     */
    static public final LocaleUtil US = createConstant("en", "US");

    /** Useful constant for country.
     */
    static public final LocaleUtil CANADA = createConstant("en", "CA");

    /** Useful constant for country.
     */
    static public final LocaleUtil CANADA_FRENCH = createConstant("fr", "CA");

    /**
     * Useful constant for the root locale.  The root locale is the locale whose
     * language, country, and variant are empty ("") strings.  This is regarded
     * as the base locale of all locales, and is used as the language/country
     * neutral locale for the locale sensitive operations.
     *
     * @since 1.5
     */
    static public final LocaleUtil ROOT = createConstant("", "");

    /**
     * The key for the private use extension ('x').
     *
     * @see #getExtension(char)
     * @see LocaleUtil.Builder#setExtension(char, String)
     * @since 1.5
     */
    static public final char PRIVATE_USE_EXTENSION = 'x';

    /**
     * The key for Unicode locale extension ('u').
     *
     * @see #getExtension(char)
     * @see LocaleUtil.Builder#setExtension(char, String)
     * @since 1.5
     */
    static public final char UNICODE_LOCALE_EXTENSION = 'u';

    /** serialization ID
     */
    static final long serialVersionUID = 9149081749638150636L;

    /**
     * Display types for retrieving localized names from the name providers.
     */
    private static final int DISPLAY_LANGUAGE = 0;
    private static final int DISPLAY_COUNTRY  = 1;
    private static final int DISPLAY_VARIANT  = 2;
    private static final int DISPLAY_SCRIPT   = 3;

    /**
     * Private constructor used by getInstance method
     */
    private LocaleUtil(BaseLocale baseLocale, LocaleExtensions extensions) {
        this.baseLocale = baseLocale;
        this.localeExtensions = extensions;
    }

    /**
     * Construct a locale from language, country and variant.
     * This constructor normalizes the language value to lowercase and
     * the country value to uppercase.
     * <p>
     * <b>Note:</b>
     * <ul>
     * <li>ISO 639 is not a stable standard; some of the language codes it defines
     * (specifically "iw", "ji", and "in") have changed.  This constructor accepts both the
     * old codes ("iw", "ji", and "in") and the new codes ("he", "yi", and "id"), but all other
     * API on LocaleUtil will return only the OLD codes.
     * <li>For backward compatibility reasons, this constructor does not make
     * any syntactic checks on the input.
     * <li>The two cases ("ja", "JP", "JP") and ("th", "TH", "TH") are handled specially,
     * see <a href="#special_cases_constructor">Special Cases</a> for more information.
     * </ul>
     *
     * @param language An ISO 639 alpha-2 or alpha-3 language code, or a language subtag
     * up to 8 characters in length.  See the <code>LocaleUtil</code> class description about
     * valid language values.
     * @param country An ISO 3166 alpha-2 country code or a UN M.49 numeric-3 area code.
     * See the <code>LocaleUtil</code> class description about valid country values.
     * @param variant Any arbitrary value used to indicate a variation of a <code>LocaleUtil</code>.
     * See the <code>LocaleUtil</code> class description for the details.
     * @exception NullPointerException thrown if any argument is null.
     */
    public LocaleUtil(String language, String country, String variant) {
        if (language== null || country == null || variant == null) {
            throw new NullPointerException();
        }
        baseLocale = BaseLocale.getInstance(convertOldISOCodes(language), "", country, variant);
        localeExtensions = getCompatibilityExtensions(language, "", country, variant);
    }

    /**
     * Construct a locale from language and country.
     * This constructor normalizes the language value to lowercase and
     * the country value to uppercase.
     * <p>
     * <b>Note:</b>
     * <ul>
     * <li>ISO 639 is not a stable standard; some of the language codes it defines
     * (specifically "iw", "ji", and "in") have changed.  This constructor accepts both the
     * old codes ("iw", "ji", and "in") and the new codes ("he", "yi", and "id"), but all other
     * API on LocaleUtil will return only the OLD codes.
     * <li>For backward compatibility reasons, this constructor does not make
     * any syntactic checks on the input.
     * </ul>
     *
     * @param language An ISO 639 alpha-2 or alpha-3 language code, or a language subtag
     * up to 8 characters in length.  See the <code>LocaleUtil</code> class description about
     * valid language values.
     * @param country An ISO 3166 alpha-2 country code or a UN M.49 numeric-3 area code.
     * See the <code>LocaleUtil</code> class description about valid country values.
     * @exception NullPointerException thrown if either argument is null.
     */
    public LocaleUtil(String language, String country) {
        this(language, country, "");
    }

    /**
     * Construct a locale from a language code.
     * This constructor normalizes the language value to lowercase.
     * <p>
     * <b>Note:</b>
     * <ul>
     * <li>ISO 639 is not a stable standard; some of the language codes it defines
     * (specifically "iw", "ji", and "in") have changed.  This constructor accepts both the
     * old codes ("iw", "ji", and "in") and the new codes ("he", "yi", and "id"), but all other
     * API on LocaleUtil will return only the OLD codes.
     * <li>For backward compatibility reasons, this constructor does not make
     * any syntactic checks on the input.
     * </ul>
     *
     * @param language An ISO 639 alpha-2 or alpha-3 language code, or a language subtag
     * up to 8 characters in length.  See the <code>LocaleUtil</code> class description about
     * valid language values.
     * @exception NullPointerException thrown if argument is null.
     * @since 1.4
     */
    public LocaleUtil(String language) {
        this(language, "", "");
    }

    /**
     * This method must be called only for creating the LocaleUtil.*
     * constants due to making shortcuts.
     */
    private static LocaleUtil createConstant(String lang, String country) {
        BaseLocale base = BaseLocale.createInstance(lang, country);
        return getInstance(base, null);
    }

    /**
     * Returns a <code>LocaleUtil</code> constructed from the given
     * <code>language</code>, <code>country</code> and
     * <code>variant</code>. If the same <code>LocaleUtil</code> instance
     * is available in the cache, then that instance is
     * returned. Otherwise, a new <code>LocaleUtil</code> instance is
     * created and cached.
     *
     * @param language lowercase 2 to 8 language code.
     * @param country uppercase two-letter ISO-3166 code and numric-3 UN M.49 area code.
     * @param variant vendor and browser specific code. See class description.
     * @return the <code>LocaleUtil</code> instance requested
     * @exception NullPointerException if any argument is null.
     */
    static LocaleUtil getInstance(String language, String country, String variant) {
        return getInstance(language, "", country, variant, null);
    }

    static LocaleUtil getInstance(String language, String script, String country,
                              String variant, LocaleExtensions extensions) {
        if (language== null || script == null || country == null || variant == null) {
            throw new NullPointerException();
        }

        if (extensions == null) {
            extensions = getCompatibilityExtensions(language, script, country, variant);
        }

        BaseLocale baseloc = BaseLocale.getInstance(language, script, country, variant);
        return getInstance(baseloc, extensions);
    }

    static LocaleUtil getInstance(BaseLocale baseloc, LocaleExtensions extensions) {
        LocaleUtil.LocaleKey key = new LocaleUtil.LocaleKey(baseloc, extensions);
        return LOCALECACHE.get(key);
    }

    private static class Cache extends LocaleObjectCache<LocaleUtil.LocaleKey, LocaleUtil> {
        private Cache() {
        }

        @Override
        protected LocaleUtil createObject(LocaleUtil.LocaleKey key) {
            return new LocaleUtil(key.base, key.exts);
        }
    }

    private static final class LocaleKey {
        private final BaseLocale base;
        private final LocaleExtensions exts;
        private final int hash;

        private LocaleKey(BaseLocale baseLocale, LocaleExtensions extensions) {
            base = baseLocale;
            exts = extensions;

            // Calculate the hash value here because it's always used.
            int h = base.hashCode();
            if (exts != null) {
                h ^= exts.hashCode();
            }
            hash = h;
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj) {
                return true;
            }
            if (!(obj instanceof LocaleUtil.LocaleKey)) {
                return false;
            }
            LocaleUtil.LocaleKey other = (LocaleUtil.LocaleKey)obj;
            if (hash != other.hash || !base.equals(other.base)) {
                return false;
            }
            if (exts == null) {
                return other.exts == null;
            }
            return exts.equals(other.exts);
        }

        @Override
        public int hashCode() {
            return hash;
        }
    }

    /**
     * Gets the current value of the default locale for this instance
     * of the Java Virtual Machine.
     * <p>
     * The Java Virtual Machine sets the default locale during startup
     * based on the host environment. It is used by many locale-sensitive
     * methods if no locale is explicitly specified.
     * It can be changed using the
     *
     * @return the default locale for this instance of the Java Virtual Machine
     */
    public static LocaleUtil getDefault() {
        // do not synchronize this method - see 4071298
        return defaultLocale;
    }

    /**
     * Gets the current value of the default locale for the specified Category
     * for this instance of the Java Virtual Machine.
     * <p>
     * The Java Virtual Machine sets the default locale during startup based
     * on the host environment. It is used by many locale-sensitive methods
     * if no locale is explicitly specified. It can be changed using the
     * setDefault(LocaleUtil.Category, LocaleUtil) method.
     *
     * @param category - the specified category to get the default locale
     * @throws NullPointerException - if category is null
     * @return the default locale for the specified Category for this instance
     *     of the Java Virtual Machine
     * @see #setDefault(LocaleUtil.Category, LocaleUtil)
     * @since 1.5
     */
    public static LocaleUtil getDefault(LocaleUtil.Category category) {
        // do not synchronize this method - see 4071298
        switch (category) {
            case DISPLAY:
                if (defaultDisplayLocale == null) {
                    synchronized(LocaleUtil.class) {
                        if (defaultDisplayLocale == null) {
                            defaultDisplayLocale = initDefault(category);
                        }
                    }
                }
                return defaultDisplayLocale;
            case FORMAT:
                if (defaultFormatLocale == null) {
                    synchronized(LocaleUtil.class) {
                        if (defaultFormatLocale == null) {
                            defaultFormatLocale = initDefault(category);
                        }
                    }
                }
                return defaultFormatLocale;
            default:
                assert false: "Unknown Category";
        }
        return getDefault();
    }

    private static LocaleUtil initDefault() {
        String language, region, script, country, variant;
        language = AccessController.doPrivileged(
                new GetPropertyAction("user.language", "en"));
        // for compatibility, check for old user.region property
        region = AccessController.doPrivileged(
                new GetPropertyAction("user.region"));
        if (region != null) {
            // region can be of form country, country_variant, or _variant
            int i = region.indexOf('_');
            if (i >= 0) {
                country = region.substring(0, i);
                variant = region.substring(i + 1);
            } else {
                country = region;
                variant = "";
            }
            script = "";
        } else {
            script = AccessController.doPrivileged(
                    new GetPropertyAction("user.script", ""));
            country = AccessController.doPrivileged(
                    new GetPropertyAction("user.country", ""));
            variant = AccessController.doPrivileged(
                    new GetPropertyAction("user.variant", ""));
        }

        return getInstance(language, script, country, variant, null);
    }

    private static LocaleUtil initDefault(LocaleUtil.Category category) {
        return getInstance(
                AccessController.doPrivileged(
                        new GetPropertyAction(category.languageKey, defaultLocale.getLanguage())),
                AccessController.doPrivileged(
                        new GetPropertyAction(category.scriptKey, defaultLocale.getScript())),
                AccessController.doPrivileged(
                        new GetPropertyAction(category.countryKey, defaultLocale.getCountry())),
                AccessController.doPrivileged(
                        new GetPropertyAction(category.variantKey, defaultLocale.getVariant())),
                null);
    }

    /**
     * Sets the default locale for this instance of the Java Virtual Machine.
     * This does not affect the host locale.
     * <p>
     * If there is a security manager, its <code>checkPermission</code>
     * method is called with a <code>PropertyPermission("user.language", "write")</code>
     * permission before the default locale is changed.
     * <p>
     * The Java Virtual Machine sets the default locale during startup
     * based on the host environment. It is used by many locale-sensitive
     * methods if no locale is explicitly specified.
     * <p>
     * Since changing the default locale may affect many different areas
     * of functionality, this method should only be used if the caller
     * is prepared to reinitialize locale-sensitive code running
     * within the same Java Virtual Machine.
     * <p>
     * By setting the default locale with this method, all of the default
     * locales for each Category are also set to the specified default locale.
     *
     * @throws SecurityException
     *        if a security manager exists and its
     *        <code>checkPermission</code> method doesn't allow the operation.
     * @throws NullPointerException if <code>newLocale</code> is null
     * @param newLocale the new default locale
     * @see SecurityManager#checkPermission
     * @see java.util.PropertyPermission
     */
    public static synchronized void setDefault(LocaleUtil newLocale) {
        setDefault(LocaleUtil.Category.DISPLAY, newLocale);
        setDefault(LocaleUtil.Category.FORMAT, newLocale);
        defaultLocale = newLocale;
    }

    /**
     * Sets the default locale for the specified Category for this instance
     * of the Java Virtual Machine. This does not affect the host locale.
     * <p>
     * If there is a security manager, its checkPermission method is called
     * with a PropertyPermission("user.language", "write") permission before
     * the default locale is changed.
     * <p>
     * The Java Virtual Machine sets the default locale during startup based
     * on the host environment. It is used by many locale-sensitive methods
     * if no locale is explicitly specified.
     * <p>
     * Since changing the default locale may affect many different areas of
     * functionality, this method should only be used if the caller is
     * prepared to reinitialize locale-sensitive code running within the
     * same Java Virtual Machine.
     * <p>
     *
     * @param category - the specified category to set the default locale
     * @param newLocale - the new default locale
     * @throws SecurityException - if a security manager exists and its
     *     checkPermission method doesn't allow the operation.
     * @throws NullPointerException - if category and/or newLocale is null
     * @see SecurityManager#checkPermission(java.security.Permission)
     * @see PropertyPermission
     * @see #getDefault(LocaleUtil.Category)
     * @since 1.5
     */
    public static synchronized void setDefault(LocaleUtil.Category category,
                                               LocaleUtil newLocale) {
        if (category == null)
            throw new NullPointerException("Category cannot be NULL");
        if (newLocale == null)
            throw new NullPointerException("Can't set default locale to NULL");

        SecurityManager sm = System.getSecurityManager();
        if (sm != null) sm.checkPermission(new PropertyPermission
                ("user.language", "write"));
        switch (category) {
            case DISPLAY:
                defaultDisplayLocale = newLocale;
                break;
            case FORMAT:
                defaultFormatLocale = newLocale;
                break;
            default:
                assert false: "Unknown Category";
        }
    }

    private static String[] getISO2Table(String table) {
        int len = table.length() / 5;
        String[] isoTable = new String[len];
        for (int i = 0, j = 0; i < len; i++, j += 5) {
            isoTable[i] = table.substring(j, j + 2);
        }
        return isoTable;
    }

    /**
     * Returns the language code of this LocaleUtil.
     *
     * <p><b>Note:</b> ISO 639 is not a stable standard&mdash; some languages' codes have changed.
     * LocaleUtil's constructor recognizes both the new and the old codes for the languages
     * whose codes have changed, but this function always returns the old code.  If you
     * want to check for a specific language whose code has changed, don't do
     * <pre>
     * if (locale.getLanguage().equals("he")) // BAD!
     *    ...
     * </pre>
     * Instead, do
     * <pre>
     * if (locale.getLanguage().equals(new LocaleUtil("he").getLanguage()))
     *    ...
     * </pre>
     * @return The language code, or the empty string if none is defined.
     */
    public String getLanguage() {
        return baseLocale.getLanguage();
    }

    /**
     * Returns the script for this locale, which should
     * either be the empty string or an ISO 15924 4-letter script
     * code. The first letter is uppercase and the rest are
     * lowercase, for example, 'Latn', 'Cyrl'.
     *
     * @return The script code, or the empty string if none is defined.
     * @since 1.5
     */
    public String getScript() {
        return baseLocale.getScript();
    }

    /**
     * Returns the country/region code for this locale, which should
     * either be the empty string, an uppercase ISO 3166 2-letter code,
     * or a UN M.49 3-digit code.
     *
     * @return The country/region code, or the empty string if none is defined.
     */
    public String getCountry() {
        return baseLocale.getRegion();
    }

    /**
     * Returns the variant code for this locale.
     *
     * @return The variant code, or the empty string if none is defined.
     */
    public String getVariant() {
        return baseLocale.getVariant();
    }

    /**
     * Returns {@code true} if this {@code LocaleUtil} has any <a href="#def_extensions">
     * extensions</a>.
     *
     * @return {@code true} if this {@code LocaleUtil} has any extensions
     * @since 1.5
     */
    public boolean hasExtensions() {
        return localeExtensions != null;
    }

    /**
     * Returns a copy of this {@code LocaleUtil} with no <a href="#def_extensions">
     * extensions</a>. If this {@code LocaleUtil} has no extensions, this {@code LocaleUtil}
     * is returned.
     *
     * @return a copy of this {@code LocaleUtil} with no extensions, or {@code this}
     *         if {@code this} has no extensions
     * @since 1.5
     */
    public LocaleUtil stripExtensions() {
        return hasExtensions() ? LocaleUtil.getInstance(baseLocale, null) : this;
    }

    /**
     * Returns the extension (or private use) value associated with
     * the specified key, or null if there is no extension
     * associated with the key. To be well-formed, the key must be one
     * of <code>[0-9A-Za-z]</code>. Keys are case-insensitive, so
     * for example 'z' and 'Z' represent the same extension.
     *
     * @param key the extension key
     * @return The extension, or null if this locale defines no
     * extension for the specified key.
     * @throws IllegalArgumentException if key is not well-formed
     * @see #PRIVATE_USE_EXTENSION
     * @see #UNICODE_LOCALE_EXTENSION
     * @since 1.5
     */
    public String getExtension(char key) {
        if (!LocaleExtensions.isValidKey(key)) {
            throw new IllegalArgumentException("Ill-formed extension key: " + key);
        }
        return hasExtensions() ? localeExtensions.getExtensionValue(key) : null;
    }

    /**
     * Returns the set of extension keys associated with this locale, or the
     * empty set if it has no extensions. The returned set is unmodifiable.
     * The keys will all be lower-case.
     *
     * @return The set of extension keys, or the empty set if this locale has
     * no extensions.
     * @since 1.5
     */
    public Set<Character> getExtensionKeys() {
        if (!hasExtensions()) {
            return Collections.emptySet();
        }
        return localeExtensions.getKeys();
    }

    /**
     * Returns the set of unicode locale attributes associated with
     * this locale, or the empty set if it has no attributes. The
     * returned set is unmodifiable.
     *
     * @return The set of attributes.
     * @since 1.5
     */
    public Set<String> getUnicodeLocaleAttributes() {
        if (!hasExtensions()) {
            return Collections.emptySet();
        }
        return localeExtensions.getUnicodeLocaleAttributes();
    }

    /**
     * Returns the Unicode locale type associated with the specified Unicode locale key
     * for this locale. Returns the empty string for keys that are defined with no type.
     * Returns null if the key is not defined. Keys are case-insensitive. The key must
     * be two alphanumeric characters ([0-9a-zA-Z]), or an IllegalArgumentException is
     * thrown.
     *
     * @param key the Unicode locale key
     * @return The Unicode locale type associated with the key, or null if the
     * locale does not define the key.
     * @throws IllegalArgumentException if the key is not well-formed
     * @throws NullPointerException if <code>key</code> is null
     * @since 1.5
     */
    public String getUnicodeLocaleType(String key) {
        if (!isUnicodeExtensionKey(key)) {
            throw new IllegalArgumentException("Ill-formed Unicode locale key: " + key);
        }
        return hasExtensions() ? localeExtensions.getUnicodeLocaleType(key) : null;
    }

    /**
     * Returns the set of Unicode locale keys defined by this locale, or the empty set if
     * this locale has none.  The returned set is immutable.  Keys are all lower case.
     *
     * @return The set of Unicode locale keys, or the empty set if this locale has
     * no Unicode locale keywords.
     * @since 1.5
     */
    public Set<String> getUnicodeLocaleKeys() {
        if (localeExtensions == null) {
            return Collections.emptySet();
        }
        return localeExtensions.getUnicodeLocaleKeys();
    }

    /**
     * Package locale method returning the LocaleUtil's BaseLocale,
     * used by ResourceBundle
     * @return base locale of this LocaleUtil
     */
    BaseLocale getBaseLocale() {
        return baseLocale;
    }

    /**
     * Package private method returning the LocaleUtil's LocaleExtensions,
     * used by ResourceBundle.
     * @return locale exnteions of this LocaleUtil,
     *         or {@code null} if no extensions are defined
     */
    LocaleExtensions getLocaleExtensions() {
        return localeExtensions;
    }

    /**
     * Returns a string representation of this <code>LocaleUtil</code>
     * object, consisting of language, country, variant, script,
     * and extensions as below:
     * <blockquote>
     * language + "_" + country + "_" + (variant + "_#" | "#") + script + "-" + extensions
     * </blockquote>
     *
     * Language is always lower case, country is always upper case, script is always title
     * case, and extensions are always lower case.  Extensions and private use subtags
     *
     * <p>When the locale has neither script nor extensions, the result is the same as in
     * Java 6 and prior.
     *
     * <p>If both the language and country fields are missing, this function will return
     * the empty string, even if the variant, script, or extensions field is present (you
     * can't have a locale with just a variant, the variant must accompany a well-formed
     * language or country code).
     *
     * <p>If script or extensions are present and variant is missing, no underscore is
     * added before the "#".
     *
     * <p>This behavior is designed to support debugging and to be compatible with
     * previous uses of <code>toString</code> that expected language, country, and variant
     * fields only.  To represent a LocaleUtil as a String for interchange purposes, use
     *
     * <p>Examples: <ul>
     * <li><tt>en</tt></li>
     * <li><tt>de_DE</tt></li>
     * <li><tt>_GB</tt></li>
     * <li><tt>en_US_WIN</tt></li>
     * <li><tt>de__POSIX</tt></li>
     * <li><tt>zh_CN_#Hans</tt></li>
     * <li><tt>zh_TW_#Hant-x-java</tt></li>
     * <li><tt>th_TH_TH_#u-nu-thai</tt></li></ul>
     *
     * @return A string representation of the LocaleUtil, for debugging.
     */
    @Override
    public final String toString() {
        boolean l = (baseLocale.getLanguage().length() != 0);
        boolean s = (baseLocale.getScript().length() != 0);
        boolean r = (baseLocale.getRegion().length() != 0);
        boolean v = (baseLocale.getVariant().length() != 0);
        boolean e = (localeExtensions != null && localeExtensions.getID().length() != 0);

        StringBuilder result = new StringBuilder(baseLocale.getLanguage());
        if (r || (l && (v || s || e))) {
            result.append('_')
                    .append(baseLocale.getRegion()); // This may just append '_'
        }
        if (v && (l || r)) {
            result.append('_')
                    .append(baseLocale.getVariant());
        }

        if (s && (l || r)) {
            result.append("_#")
                    .append(baseLocale.getScript());
        }

        if (e && (l || r)) {
            result.append('_');
            if (!s) {
                result.append('#');
            }
            result.append(localeExtensions.getID());
        }

        return result.toString();
    }

    private static String getISO3Code(String iso2Code, String table) {
        int codeLength = iso2Code.length();
        if (codeLength == 0) {
            return "";
        }

        int tableLength = table.length();
        int index = tableLength;
        if (codeLength == 2) {
            char c1 = iso2Code.charAt(0);
            char c2 = iso2Code.charAt(1);
            for (index = 0; index < tableLength; index += 5) {
                if (table.charAt(index) == c1
                        && table.charAt(index + 1) == c2) {
                    break;
                }
            }
        }
        return index < tableLength ? table.substring(index + 2, index + 5) : null;
    }

    /**
     * Overrides Cloneable.
     */
    public Object clone()
    {
        try {
            LocaleUtil that = (LocaleUtil)super.clone();
            return that;
        } catch (CloneNotSupportedException e) {
            throw new Error(e);
        }
    }

    /**
     * Override hashCode.
     * Since Locales are often used in hashtables, caches the value
     * for speed.
     */
    @Override
    public int hashCode() {
        int hc = hashCodeValue;
        if (hc == 0) {
            hc = baseLocale.hashCode();
            if (localeExtensions != null) {
                hc ^= localeExtensions.hashCode();
            }
            hashCodeValue = hc;
        }
        return hc;
    }

    // Overrides

    /**
     * Returns true if this LocaleUtil is equal to another object.  A LocaleUtil is
     * deemed equal to another LocaleUtil with identical language, script, country,
     * variant and extensions, and unequal to all other objects.
     *
     * @return true if this LocaleUtil is equal to the specified object.
     */
    @Override
    public boolean equals(Object obj) {
        if (this == obj)                      // quick check
            return true;
        if (!(obj instanceof LocaleUtil))
            return false;
        BaseLocale otherBase = ((LocaleUtil)obj).baseLocale;
        if (!baseLocale.equals(otherBase)) {
            return false;
        }
        if (localeExtensions == null) {
            return ((LocaleUtil)obj).localeExtensions == null;
        }
        return localeExtensions.equals(((LocaleUtil)obj).localeExtensions);
    }

    // ================= privates =====================================

    private transient BaseLocale baseLocale;
    private transient LocaleExtensions localeExtensions;

    /**
     * Calculated hashcode
     */
    private transient volatile int hashCodeValue = 0;

    private volatile static LocaleUtil defaultLocale = initDefault();
    private volatile static LocaleUtil defaultDisplayLocale = null;
    private volatile static LocaleUtil defaultFormatLocale = null;

    private transient volatile String languageTag;


    /**
     * Format a list using given pattern strings.
     * If either of the patterns is null, then a the list is
     * formatted by concatenation with the delimiter ','.
     * @param stringList the list of strings to be formatted.
     * @param listPattern should create a MessageFormat taking 0-3 arguments
     * and formatting them into a list.
     * @param listCompositionPattern should take 2 arguments
     * and is used by composeList.
     * @return a string representing the list.
     */
    private static String formatList(String[] stringList, String listPattern, String listCompositionPattern) {
        // If we have no list patterns, compose the list in a simple,
        // non-localized way.
        if (listPattern == null || listCompositionPattern == null) {
            StringBuilder result = new StringBuilder();
            for (int i = 0; i < stringList.length; ++i) {
                if (i > 0) {
                    result.append(',');
                }
                result.append(stringList[i]);
            }
            return result.toString();
        }

        // Compose the list down to three elements if necessary
        if (stringList.length > 3) {
            MessageFormat format = new MessageFormat(listCompositionPattern);
            stringList = composeList(format, stringList);
        }

        // Rebuild the argument list with the list length as the first element
        Object[] args = new Object[stringList.length + 1];
        System.arraycopy(stringList, 0, args, 1, stringList.length);
        args[0] = new Integer(stringList.length);

        // Format it using the pattern in the resource
        MessageFormat format = new MessageFormat(listPattern);
        return format.format(args);
    }

    /**
     * Given a list of strings, return a list shortened to three elements.
     * Shorten it by applying the given format to the first two elements
     * recursively.
     * @param format a format which takes two arguments
     * @param list a list of strings
     * @return if the list is three elements or shorter, the same list;
     * otherwise, a new list of three elements.
     */
    private static String[] composeList(MessageFormat format, String[] list) {
        if (list.length <= 3) return list;

        // Use the given format to compose the first two elements into one
        String[] listItems = { list[0], list[1] };
        String newItem = format.format(listItems);

        // Form a new list one element shorter
        String[] newList = new String[list.length-1];
        System.arraycopy(list, 2, newList, 1, newList.length-1);
        newList[0] = newItem;

        // Recurse
        return composeList(format, newList);
    }

    // Duplicate of sun.util.locale.UnicodeLocaleExtension.isKey in order to
    // avoid its class loading.
    private static boolean isUnicodeExtensionKey(String s) {
        // 2alphanum
        return (s.length() == 2) && LocaleUtils.isAlphaNumericString(s);
    }

    /**
     * @serialField language    String
     *      language subtag in lower case. (See <a href="java/util/LocaleUtil.html#getLanguage()">getLanguage()</a>)
     * @serialField country     String
     *      country subtag in upper case. (See <a href="java/util/LocaleUtil.html#getCountry()">getCountry()</a>)
     * @serialField variant     String
     *      variant subtags separated by LOWLINE characters. (See <a href="java/util/LocaleUtil.html#getVariant()">getVariant()</a>)
     * @serialField hashcode    int
     *      deprecated, for forward compatibility only
     * @serialField script      String
     *      script subtag in title case (See <a href="java/util/LocaleUtil.html#getScript()">getScript()</a>)
     * @serialField extensions  String
     *      canonical representation of extensions, that is,
     *      BCP47 extensions in alphabetical order followed by
     *      BCP47 private use subtags, all in lower case letters
     *      separated by HYPHEN-MINUS characters.
     *      (See <a href="java/util/LocaleUtil.html#getExtensionKeys()">getExtensionKeys()</a>,
     *      <a href="java/util/LocaleUtil.html#getExtension(char)">getExtension(char)</a>)
     */
    private static final ObjectStreamField[] serialPersistentFields = {
            new ObjectStreamField("language", String.class),
            new ObjectStreamField("country", String.class),
            new ObjectStreamField("variant", String.class),
            new ObjectStreamField("hashcode", int.class),
            new ObjectStreamField("script", String.class),
            new ObjectStreamField("extensions", String.class),
    };

    /**
     * Serializes this <code>LocaleUtil</code> to the specified <code>ObjectOutputStream</code>.
     * @param out the <code>ObjectOutputStream</code> to write
     * @throws IOException
     * @since 1.5
     */
    private void writeObject(ObjectOutputStream out) throws IOException {
        ObjectOutputStream.PutField fields = out.putFields();
        fields.put("language", baseLocale.getLanguage());
        fields.put("script", baseLocale.getScript());
        fields.put("country", baseLocale.getRegion());
        fields.put("variant", baseLocale.getVariant());
        fields.put("extensions", localeExtensions == null ? "" : localeExtensions.getID());
        fields.put("hashcode", -1); // place holder just for backward support
        out.writeFields();
    }

    /**
     * Deserializes this <code>LocaleUtil</code>.
     * @param in the <code>ObjectInputStream</code> to read
     * @throws IOException
     * @throws ClassNotFoundException
     * @since 1.5
     */
    private void readObject(ObjectInputStream in) throws IOException, ClassNotFoundException, LocaleSyntaxException {
        ObjectInputStream.GetField fields = in.readFields();
        String language = (String)fields.get("language", "");
        String script = (String)fields.get("script", "");
        String country = (String)fields.get("country", "");
        String variant = (String)fields.get("variant", "");
        String extStr = (String)fields.get("extensions", "");
        baseLocale = BaseLocale.getInstance(convertOldISOCodes(language), script, country, variant);
        if (extStr.length() > 0) {
            try {
                InternalLocaleBuilder bldr = new InternalLocaleBuilder();
                bldr.setExtensions(extStr);
                localeExtensions = bldr.getLocaleExtensions();
            } catch (LocaleSyntaxException e) {
                throw e;
            }
        } else {
            localeExtensions = null;
        }
    }

    /**
     * Returns a cached <code>LocaleUtil</code> instance equivalent to
     * the deserialized <code>LocaleUtil</code>. When serialized
     * language, country and variant fields read from the object data stream
     * are exactly "ja", "JP", "JP" or "th", "TH", "TH" and script/extensions
     * fields are empty, this method supplies <code>UNICODE_LOCALE_EXTENSION</code>
     * "ca"/"japanese" (calendar type is "japanese") or "nu"/"thai" (number script
     * type is "thai"). See <a href="LocaleUtil.html#special_cases_constructor">Special Cases</a>
     * for more information.
     *
     * @return an instance of <code>LocaleUtil</code> equivalent to
     * the deserialized <code>LocaleUtil</code>.
     * @throws java.io.ObjectStreamException
     */
    private Object readResolve() throws java.io.ObjectStreamException {
        return getInstance(baseLocale.getLanguage(), baseLocale.getScript(),
                baseLocale.getRegion(), baseLocale.getVariant(), localeExtensions);
    }

    private static volatile String[] isoLanguages = null;

    private static volatile String[] isoCountries = null;

    private static String convertOldISOCodes(String language) {
        // we accept both the old and the new ISO codes for the languages whose ISO
        // codes have changed, but we always store the OLD code, for backward compatibility
        language = LocaleUtils.toLowerString(language).intern();
        if (language == "he") {
            return "iw";
        } else if (language == "yi") {
            return "ji";
        } else if (language == "id") {
            return "in";
        } else {
            return language;
        }
    }

    private static LocaleExtensions getCompatibilityExtensions(String language,
                                                               String script,
                                                               String country,
                                                               String variant) {
        LocaleExtensions extensions = null;
        // Special cases for backward compatibility support
        if (LocaleUtils.caseIgnoreMatch(language, "ja")
                && script.length() == 0
                && LocaleUtils.caseIgnoreMatch(country, "jp")
                && "JP".equals(variant)) {
            // ja_JP_JP -> u-ca-japanese (calendar = japanese)
            extensions = LocaleExtensions.CALENDAR_JAPANESE;
        } else if (LocaleUtils.caseIgnoreMatch(language, "th")
                && script.length() == 0
                && LocaleUtils.caseIgnoreMatch(country, "th")
                && "TH".equals(variant)) {
            // th_TH_TH -> u-nu-thai (numbersystem = thai)
            extensions = LocaleExtensions.NUMBER_THAI;
        }
        return extensions;
    }

    /**
     * Enum for locale categories.  These locale categories are used to get/set
     * the default locale for the specific functionality represented by the
     * category.
     *
     * @see #getDefault(LocaleUtil.Category)
     * @see #setDefault(LocaleUtil.Category, LocaleUtil)
     * @since 1.5
     */
    public enum Category {

        /**
         * Category used to represent the default locale for
         * displaying user interfaces.
         */
        DISPLAY("user.language.display",
                "user.script.display",
                "user.country.display",
                "user.variant.display"),

        /**
         * Category used to represent the default locale for
         * formatting dates, numbers, and/or currencies.
         */
        FORMAT("user.language.format",
                "user.script.format",
                "user.country.format",
                "user.variant.format");

        Category(String languageKey, String scriptKey, String countryKey, String variantKey) {
            this.languageKey = languageKey;
            this.scriptKey = scriptKey;
            this.countryKey = countryKey;
            this.variantKey = variantKey;
        }

        final String languageKey;
        final String scriptKey;
        final String countryKey;
        final String variantKey;
    }

    /**
     * <code>Builder</code> is used to build instances of <code>LocaleUtil</code>
     * from values configured by the setters.  Unlike the <code>LocaleUtil</code>
     * constructors, the <code>Builder</code> checks if a value configured by a
     * setter satisfies the syntax requirements defined by the <code>LocaleUtil</code>
     * class.  A <code>LocaleUtil</code> object created by a <code>Builder</code> is
     * well-formed and can be transformed to a well-formed IETF BCP 47 language tag
     * without losing information.
     *
     * <p><b>Note:</b> The <code>LocaleUtil</code> class does not provide any
     * syntactic restrictions on variant, while BCP 47 requires each variant
     * subtag to be 5 to 8 alphanumerics or a single numeric followed by 3
     * alphanumerics.  The method <code>setVariant</code> throws
     * <code>IllformedLocaleException</code> for a variant that does not satisfy
     * this restriction. If it is necessary to support such a variant, use a
     * LocaleUtil constructor.  However, keep in mind that a <code>LocaleUtil</code>
     * object created this way might lose the variant information when
     * transformed to a BCP 47 language tag.
     *
     * <p>The following example shows how to create a <code>LocaleUtil</code> object
     * with the <code>Builder</code>.
     * <blockquote>
     * <pre>
     *     LocaleUtil aLocale = new Builder().setLanguage("sr").setScript("Latn").setRegion("RS").build();
     * </pre>
     * </blockquote>
     *
     * <p>Builders can be reused; <code>clear()</code> resets all
     * fields to their default values.
     *
     * @since 1.5
     */
    public static final class Builder {
        private final InternalLocaleBuilder localeBuilder;

        /**
         * Constructs an empty Builder. The default value of all
         * fields, extensions, and private use information is the
         * empty string.
         */
        public Builder() {
            localeBuilder = new InternalLocaleBuilder();
        }

        /**
         * Resets the <code>Builder</code> to match the provided
         * <code>locale</code>.  Existing state is discarded.
         *
         * <p>All fields of the locale must be well-formed, see {@link LocaleUtil}.
         *
         * <p>Locales with any ill-formed fields cause
         * <code>IllformedLocaleException</code> to be thrown, except for the
         * following three cases which are accepted for compatibility
         * reasons:<ul>
         * <li>LocaleUtil("ja", "JP", "JP") is treated as "ja-JP-u-ca-japanese"
         * <li>LocaleUtil("th", "TH", "TH") is treated as "th-TH-u-nu-thai"
         * <li>LocaleUtil("no", "NO", "NY") is treated as "nn-NO"</ul>
         *
         * @param locale the locale
         * @return This builder.
         * any ill-formed fields.
         * @throws NullPointerException if <code>locale</code> is null.
         */
        public LocaleUtil.Builder setLocale(LocaleUtil locale) throws LocaleSyntaxException {
            try {
                localeBuilder.setLocale(locale.baseLocale, locale.localeExtensions);
            } catch (LocaleSyntaxException e) {
                throw e;
            }
            return this;
        }

        /**
         * Resets the Builder to match the provided IETF BCP 47
         * language tag.  Discards the existing state.  Null and the
         * empty string cause the builder to be reset, like {@link
         * #clear}.  Grandfathered tags (see {@link
         *
         * @param languageTag the language tag
         * @return This builder.
         * @throws IllformedLocaleException if <code>languageTag</code> is ill-formed
         * @see LocaleUtil#forLanguageTag(String)
         */
        public LocaleUtil.Builder setLanguageTag(String languageTag) throws Exception {
            ParseStatus sts = new ParseStatus();
            LanguageTag tag = LanguageTag.parse(languageTag, sts);
            if (sts.isError()) {
                throw new Exception(sts.getErrorMessage());
            }
            localeBuilder.setLanguageTag(tag);
            return this;
        }

        /**
         * Sets the language.  If <code>language</code> is the empty string or
         * null, the language in this <code>Builder</code> is removed.  Otherwise,
         * the language must be <a href="./LocaleUtil.html#def_language">well-formed</a>
         * or an exception is thrown.
         *
         * <p>The typical language value is a two or three-letter language
         * code as defined in ISO639.
         *
         * @param language the language
         * @return This builder.
         */
        public LocaleUtil.Builder setLanguage(String language) throws LocaleSyntaxException {
            try {
                localeBuilder.setLanguage(language);
            } catch (LocaleSyntaxException e) {
                throw e;
            }
            return this;
        }

        /**
         * Sets the script. If <code>script</code> is null or the empty string,
         * the script in this <code>Builder</code> is removed.
         * Otherwise, the script must be <a href="./LocaleUtil.html#def_script">well-formed</a> or an
         * exception is thrown.
         *
         * <p>The typical script value is a four-letter script code as defined by ISO 15924.
         *
         * @param script the script
         * @return This builder.
         */
        public LocaleUtil.Builder setScript(String script) throws LocaleSyntaxException {
            try {
                localeBuilder.setScript(script);
            } catch (LocaleSyntaxException e) {
                throw e;
            }
            return this;
        }

        /**
         * Sets the region.  If region is null or the empty string, the region
         * in this <code>Builder</code> is removed.  Otherwise,
         * the region must be <a href="./LocaleUtil.html#def_region">well-formed</a> or an
         * exception is thrown.
         *
         * <p>The typical region value is a two-letter ISO 3166 code or a
         * three-digit UN M.49 area code.
         *
         * <p>The country value in the <code>LocaleUtil</code> created by the
         * <code>Builder</code> is always normalized to upper case.
         *
         * @param region the region
         * @return This builder.
         */
        public LocaleUtil.Builder setRegion(String region) throws LocaleSyntaxException {
            try {
                localeBuilder.setRegion(region);
            } catch (LocaleSyntaxException e) {
                throw e;
            }
            return this;
        }

        /**
         * Sets the variant.  If variant is null or the empty string, the
         * variant in this <code>Builder</code> is removed.  Otherwise, it
         * must consist of one or more <a href="./LocaleUtil.html#def_variant">well-formed</a>
         * subtags, or an exception is thrown.
         *
         * <p><b>Note:</b> This method checks if <code>variant</code>
         * satisfies the IETF BCP 47 variant subtag's syntax requirements,
         * and normalizes the value to lowercase letters.  However,
         * the <code>LocaleUtil</code> class does not impose any syntactic
         * restriction on variant, and the variant value in
         * <code>LocaleUtil</code> is case sensitive.  To set such a variant,
         * use a LocaleUtil constructor.
         *
         * @param variant the variant
         * @return This builder.
         */
        public LocaleUtil.Builder setVariant(String variant) throws LocaleSyntaxException {
            try {
                localeBuilder.setVariant(variant);
            } catch (LocaleSyntaxException e) {
                throw e;
            }
            return this;
        }

        /**
         * Sets the extension for the given key. If the value is null or the
         * empty string, the extension is removed.  Otherwise, the extension
         * must be <a href="./LocaleUtil.html#def_extensions">well-formed</a> or an exception
         * is thrown.
         *
         * <p><b>Note:</b> The key {@link LocaleUtil#UNICODE_LOCALE_EXTENSION
         * UNICODE_LOCALE_EXTENSION} ('u') is used for the Unicode locale extension.
         * Setting a value for this key replaces any existing Unicode locale key/type
         * pairs with those defined in the extension.
         *
         * <p><b>Note:</b> The key {@link LocaleUtil#PRIVATE_USE_EXTENSION
         * PRIVATE_USE_EXTENSION} ('x') is used for the private use code. To be
         * well-formed, the value for this key needs only to have subtags of one to
         * eight alphanumeric characters, not two to eight as in the general case.
         *
         * @param key the extension key
         * @param value the extension value
         * @return This builder.
         * or <code>value</code> is ill-formed
         * @see #setUnicodeLocaleKeyword(String, String)
         */
        public LocaleUtil.Builder setExtension(char key, String value) throws LocaleSyntaxException {
            try {
                localeBuilder.setExtension(key, value);
            } catch (LocaleSyntaxException e) {
                throw e;
            }
            return this;
        }

        /**
         * Sets the Unicode locale keyword type for the given key.  If the type
         * is null, the Unicode keyword is removed.  Otherwise, the key must be
         * non-null and both key and type must be <a
         * href="./LocaleUtil.html#def_locale_extension">well-formed</a> or an exception
         * is thrown.
         *
         * <p>Keys and types are converted to lower case.
         *
         * <p><b>Note</b>:Setting the 'u' extension via {@link #setExtension}
         * replaces all Unicode locale keywords with those defined in the
         * extension.
         *
         * @param key the Unicode locale key
         * @param type the Unicode locale type
         * @return This builder.
         * is ill-formed
         * @throws NullPointerException if <code>key</code> is null
         * @see #setExtension(char, String)
         */
        public LocaleUtil.Builder setUnicodeLocaleKeyword(String key, String type) throws LocaleSyntaxException {
            try {
                localeBuilder.setUnicodeLocaleKeyword(key, type);
            } catch (LocaleSyntaxException e) {
                throw e;
            }
            return this;
        }

        /**
         * Adds a unicode locale attribute, if not already present, otherwise
         * has no effect.  The attribute must not be null and must be <a
         * href="./LocaleUtil.html#def_locale_extension">well-formed</a> or an exception
         * is thrown.
         *
         * @param attribute the attribute
         * @return This builder.
         * @throws NullPointerException if <code>attribute</code> is null
         * @see #setExtension(char, String)
         */
        public LocaleUtil.Builder addUnicodeLocaleAttribute(String attribute) throws LocaleSyntaxException {
            try {
                localeBuilder.addUnicodeLocaleAttribute(attribute);
            } catch (LocaleSyntaxException e) {
                throw e;
            }
            return this;
        }

        /**
         * Removes a unicode locale attribute, if present, otherwise has no
         * effect.  The attribute must not be null and must be <a
         * href="./LocaleUtil.html#def_locale_extension">well-formed</a> or an exception
         * is thrown.
         *
         * <p>Attribute comparision for removal is case-insensitive.
         *
         * @param attribute the attribute
         * @return This builder.
         * @throws NullPointerException if <code>attribute</code> is null
         * @see #setExtension(char, String)
         */
        public LocaleUtil.Builder removeUnicodeLocaleAttribute(String attribute) throws LocaleSyntaxException {
            try {
                localeBuilder.removeUnicodeLocaleAttribute(attribute);
            } catch (LocaleSyntaxException e) {
                throw e;
            }
            return this;
        }

        /**
         * Resets the builder to its initial, empty state.
         *
         * @return This builder.
         */
        public LocaleUtil.Builder clear() {
            localeBuilder.clear();
            return this;
        }

        /**
         * Resets the extensions to their initial, empty state.
         * Language, script, region and variant are unchanged.
         *
         * @return This builder.
         * @see #setExtension(char, String)
         */
        public LocaleUtil.Builder clearExtensions() {
            localeBuilder.clearExtensions();
            return this;
        }

        /**
         * Returns an instance of <code>LocaleUtil</code> created from the fields set
         * on this builder.
         *
         * when constructing a LocaleUtil. (Grandfathered tags are handled in
         * {@link #setLanguageTag}.)
         *
         * @return A LocaleUtil.
         */
        public LocaleUtil build() {
            BaseLocale baseloc = localeBuilder.getBaseLocale();
            LocaleExtensions extensions = localeBuilder.getLocaleExtensions();
            if (extensions == null && baseloc.getVariant().length() > 0) {
                extensions = getCompatibilityExtensions(baseloc.getLanguage(), baseloc.getScript(),
                        baseloc.getRegion(), baseloc.getVariant());
            }
            return LocaleUtil.getInstance(baseloc, extensions);
        }
    }

    /**
     * This enum provides constants to select a filtering mode for locale
     * matching. Refer to <a href="http://tools.ietf.org/html/rfc4647">RFC 4647
     * Matching of Language Tags</a> for details.
     *
     * <p>As an example, think of two Language Priority Lists each of which
     * includes only one language range and a set of following language tags:
     *
     * <pre>
     *    de (German)
     *    de-DE (German, Germany)
     *    de-Deva (German, in Devanagari script)
     *    de-Deva-DE (German, in Devanagari script, Germany)
     *    de-DE-1996 (German, Germany, orthography of 1996)
     *    de-Latn-DE (German, in Latin script, Germany)
     *    de-Latn-DE-1996 (German, in Latin script, Germany, orthography of 1996)
     * </pre>
     *
     * The filtering method will behave as follows:
     *
     * <table cellpadding=2 summary="Filtering method behavior">
     * <tr>
     * <th>Filtering Mode</th>
     * <th>Language Priority List: {@code "de-DE"}</th>
     * <th>Language Priority List: {@code "de-*-DE"}</th>
     * </tr>
     * <tr>
     * <td valign=top>
     * {@link LocaleUtil.FilteringMode#AUTOSELECT_FILTERING AUTOSELECT_FILTERING}
     * </td>
     * <td valign=top>
     * Performs <em>basic</em> filtering and returns {@code "de-DE"} and
     * {@code "de-DE-1996"}.
     * </td>
     * <td valign=top>
     * Performs <em>extended</em> filtering and returns {@code "de-DE"},
     * {@code "de-Deva-DE"}, {@code "de-DE-1996"}, {@code "de-Latn-DE"}, and
     * {@code "de-Latn-DE-1996"}.
     * </td>
     * </tr>
     * <tr>
     * <td valign=top>
     * {@link LocaleUtil.FilteringMode#EXTENDED_FILTERING EXTENDED_FILTERING}
     * </td>
     * <td valign=top>
     * Performs <em>extended</em> filtering and returns {@code "de-DE"},
     * {@code "de-Deva-DE"}, {@code "de-DE-1996"}, {@code "de-Latn-DE"}, and
     * {@code "de-Latn-DE-1996"}.
     * </td>
     * <td valign=top>Same as above.</td>
     * </tr>
     * <tr>
     * <td valign=top>
     * {@link LocaleUtil.FilteringMode#IGNORE_EXTENDED_RANGES IGNORE_EXTENDED_RANGES}
     * </td>
     * <td valign=top>
     * Performs <em>basic</em> filtering and returns {@code "de-DE"} and
     * {@code "de-DE-1996"}.
     * </td>
     * <td valign=top>
     * Performs <em>basic</em> filtering and returns {@code null} because
     * nothing matches.
     * </td>
     * </tr>
     * <tr>
     * <td valign=top>
     * {@link LocaleUtil.FilteringMode#MAP_EXTENDED_RANGES MAP_EXTENDED_RANGES}
     * </td>
     * <td valign=top>Same as above.</td>
     * <td valign=top>
     * Performs <em>basic</em> filtering and returns {@code "de-DE"} and
     * {@code "de-DE-1996"} because {@code "de-*-DE"} is mapped to
     * {@code "de-DE"}.
     * </td>
     * </tr>
     * <tr>
     * <td valign=top>
     * {@link LocaleUtil.FilteringMode#REJECT_EXTENDED_RANGES REJECT_EXTENDED_RANGES}
     * </td>
     * <td valign=top>Same as above.</td>
     * <td valign=top>
     * Throws {@link IllegalArgumentException} because {@code "de-*-DE"} is
     * not a valid basic language range.
     * </td>
     * </tr>
     * </table>
     *
     *
     * @since 1.5
     */
    public static enum FilteringMode {
        /**
         * Specifies automatic filtering mode based on the given Language
         * Priority List consisting of language ranges. If all of the ranges
         * are basic, basic filtering is selected. Otherwise, extended
         * filtering is selected.
         */
        AUTOSELECT_FILTERING,

        /**
         * Specifies extended filtering.
         */
        EXTENDED_FILTERING,

        /**
         * Specifies basic filtering: Note that any extended language ranges
         * included in the given Language Priority List are ignored.
         */
        IGNORE_EXTENDED_RANGES,

        /**
         * Specifies basic filtering: If any extended language ranges are
         * included in the given Language Priority List, they are mapped to the
         * basic language range. Specifically, a language range starting with a
         * subtag {@code "*"} is treated as a language range {@code "*"}. For
         * example, {@code "*-US"} is treated as {@code "*"}. If {@code "*"} is
         * not the first subtag, {@code "*"} and extra {@code "-"} are removed.
         * For example, {@code "ja-*-JP"} is mapped to {@code "ja-JP"}.
         */
        MAP_EXTENDED_RANGES,

        /**
         * Specifies basic filtering: If any extended language ranges are
         * included in the given Language Priority List, the list is rejected
         * and the filtering method throws {@link IllegalArgumentException}.
         */
        REJECT_EXTENDED_RANGES
    };

    /**
     * This class expresses a <em>Language Range</em> defined in
     * <a href="http://tools.ietf.org/html/rfc4647">RFC 4647 Matching of
     * Language Tags</a>. A language range is an identifier which is used to
     * select language tag(s) meeting specific requirements by using the
     * mechanisms described in <a href="LocaleUtil.html#LocaleMatching">LocaleUtil
     * Matching</a>. A list which represents a user's preferences and consists
     * of language ranges is called a <em>Language Priority List</em>.
     *
     * <p>There are two types of language ranges: basic and extended. In RFC
     * 4647, the syntax of language ranges is expressed in
     * <a href="http://tools.ietf.org/html/rfc4234">ABNF</a> as follows:
     * <blockquote>
     * <pre>
     *     basic-language-range    = (1*8ALPHA *("-" 1*8alphanum)) / "*"
     *     extended-language-range = (1*8ALPHA / "*")
     *                               *("-" (1*8alphanum / "*"))
     *     alphanum                = ALPHA / DIGIT
     * </pre>
     * </blockquote>
     * For example, {@code "en"} (English), {@code "ja-JP"} (Japanese, Japan),
     * {@code "*"} (special language range which matches any language tag) are
     * basic language ranges, whereas {@code "*-CH"} (any languages,
     * Switzerland), {@code "es-*"} (Spanish, any regions), and
     * {@code "zh-Hant-*"} (Traditional Chinese, any regions) are extended
     * language ranges.
     *
     * @since 1.5
     */
    public static final class LanguageRange {

        /**
         * A constant holding the maximum value of weight, 1.0, which indicates
         * that the language range is a good fit for the user.
         */
        public static final double MAX_WEIGHT = 1.0;

        /**
         * A constant holding the minimum value of weight, 0.0, which indicates
         * that the language range is not a good fit for the user.
         */
        public static final double MIN_WEIGHT = 0.0;

        private final String range;
        private final double weight;

        private volatile int hash = 0;

        /**
         * Constructs a {@code LanguageRange} using the given {@code range}.
         * Note that no validation is done against the IANA Language Subtag
         * Registry at time of construction.
         *
         * <p>This is equivalent to {@code LanguageRange(range, MAX_WEIGHT)}.
         *
         * @param range a language range
         * @throws NullPointerException if the given {@code range} is
         *     {@code null}
         */
        public LanguageRange(String range) {
            this(range, MAX_WEIGHT);
        }

        /**
         * Constructs a {@code LanguageRange} using the given {@code range} and
         * {@code weight}. Note that no validation is done against the IANA
         * Language Subtag Registry at time of construction.
         *
         * @param range  a language range
         * @param weight a weight value between {@code MIN_WEIGHT} and
         *     {@code MAX_WEIGHT}
         * @throws NullPointerException if the given {@code range} is
         *     {@code null}
         * @throws IllegalArgumentException if the given {@code weight} is less
         *     than {@code MIN_WEIGHT} or greater than {@code MAX_WEIGHT}
         */
        public LanguageRange(String range, double weight) {
            if (range == null) {
                throw new NullPointerException();
            }
            if (weight < MIN_WEIGHT || weight > MAX_WEIGHT) {
                throw new IllegalArgumentException("weight=" + weight);
            }

            range = range.toLowerCase();

            // Do syntax check.
            boolean isIllFormed = false;
            String[] subtags = range.split("-");
            if (isSubtagIllFormed(subtags[0], true)
                    || range.endsWith("-")) {
                isIllFormed = true;
            } else {
                for (int i = 1; i < subtags.length; i++) {
                    if (isSubtagIllFormed(subtags[i], false)) {
                        isIllFormed = true;
                        break;
                    }
                }
            }
            if (isIllFormed) {
                throw new IllegalArgumentException("range=" + range);
            }

            this.range = range;
            this.weight = weight;
        }

        private static boolean isSubtagIllFormed(String subtag,
                                                 boolean isFirstSubtag) {
            if (subtag.equals("") || subtag.length() > 8) {
                return true;
            } else if (subtag.equals("*")) {
                return false;
            }
            char[] charArray = subtag.toCharArray();
            if (isFirstSubtag) { // ALPHA
                for (char c : charArray) {
                    if (c < 'a' || c > 'z') {
                        return true;
                    }
                }
            } else { // ALPHA / DIGIT
                for (char c : charArray) {
                    if (c < '0' || (c > '9' && c < 'a') || c > 'z') {
                        return true;
                    }
                }
            }
            return false;
        }

        /**
         * Returns the language range of this {@code LanguageRange}.
         *
         * @return the language range.
         */
        public String getRange() {
            return range;
        }

        /**
         * Returns the weight of this {@code LanguageRange}.
         *
         * @return the weight value.
         */
        public double getWeight() {
            return weight;
        }

        /**
         * Returns a hash code value for the object.
         *
         * @return  a hash code value for this object.
         */
        @Override
        public int hashCode() {
            if (hash == 0) {
                int result = 17;
                result = 37*result + range.hashCode();
                long bitsWeight = Double.doubleToLongBits(weight);
                result = 37*result + (int)(bitsWeight ^ (bitsWeight >>> 32));
                hash = result;
            }
            return hash;
        }

        /**
         * Compares this object to the specified object. The result is true if
         * and only if the argument is not {@code null} and is a
         * {@code LanguageRange} object that contains the same {@code range}
         * and {@code weight} values as this object.
         *
         * @param obj the object to compare with
         * @return  {@code true} if this object's {@code range} and
         *     {@code weight} are the same as the {@code obj}'s; {@code false}
         *     otherwise.
         */
        @Override
        public boolean equals(Object obj) {
            if (this == obj) {
                return true;
            }
            if (!(obj instanceof LocaleUtil.LanguageRange)) {
                return false;
            }
            LocaleUtil.LanguageRange other = (LocaleUtil.LanguageRange)obj;
            return hash == other.hash
                    && range.equals(other.range)
                    && weight == other.weight;
        }
    }

}
