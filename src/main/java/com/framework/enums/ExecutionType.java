package com.framework.enums;

/**
 * Driver execution mode.
 * Passed via Maven property: -Dexecution=local | remote
 */
public enum ExecutionType {
    LOCAL, REMOTE;

    public static ExecutionType fromString(String value) {
        try {
            return ExecutionType.valueOf(value.toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new RuntimeException("Unknown execution type: '" + value + "'. Valid values: local, remote");
        }
    }
}
