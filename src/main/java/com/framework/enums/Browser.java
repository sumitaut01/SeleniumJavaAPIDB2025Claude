package com.framework.enums;

/**
 * Supported browsers.
 * Passed via Maven property: -Dbrowser=chrome | firefox | edge
 */
public enum Browser {
    CHROME, FIREFOX, EDGE;

    public static Browser fromString(String value) {
        try {
            return Browser.valueOf(value.toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new RuntimeException("Unknown browser: '" + value + "'. Valid: chrome, firefox, edge");
        }
    }
}
