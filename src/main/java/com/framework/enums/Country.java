package com.framework.enums;

/**
 * Supported countries for multi-country data execution.
 * Passed via Maven property: -Dcountry=IN | US | UK | AU
 *
 * Each country may have its own:
 *  - base URL
 *  - test data file (Excel sheet / JSON key)
 *  - locale settings
 */
public enum Country {
    IN("India",     "en-IN", "+91"),
    US("USA",       "en-US", "+1"),
    UK("UK",        "en-GB", "+44"),
    AU("Australia", "en-AU", "+61");

    private final String displayName;
    private final String locale;
    private final String dialCode;

    Country(String displayName, String locale, String dialCode) {
        this.displayName = displayName;
        this.locale      = locale;
        this.dialCode    = dialCode;
    }

    public String getDisplayName() { return displayName; }
    public String getLocale()      { return locale; }
    public String getDialCode()    { return dialCode; }

    public static Country fromString(String value) {
        try {
            return Country.valueOf(value.toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new RuntimeException("Unknown country: '" + value + "'. Valid values: IN, US, UK, AU");
        }
    }
}
