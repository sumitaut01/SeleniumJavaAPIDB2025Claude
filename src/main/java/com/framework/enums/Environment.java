package com.framework.enums;

/**
 * Supported execution environments.
 * Passed via Maven property: -Denv=qa | uat | dev
 */
public enum Environment {
    DEV, QA, UAT;

    public static Environment fromString(String value) {
        try {
            return Environment.valueOf(value.toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new RuntimeException("Unknown environment: '" + value + "'. Valid values: dev, qa, uat");
        }
    }
}
