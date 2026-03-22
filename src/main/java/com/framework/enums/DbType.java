package com.framework.enums;

/**
 * Supported database types in the framework.
 * Each maps to a distinct JDBC driver and connection pool config.
 */
public enum DbType {

    POSTGRES("org.postgresql.Driver",          "jdbc:postgresql"),
    ORACLE  ("oracle.jdbc.OracleDriver",        "jdbc:oracle:thin");

    private final String driverClass;
    private final String jdbcPrefix;

    DbType(String driverClass, String jdbcPrefix) {
        this.driverClass = driverClass;
        this.jdbcPrefix  = jdbcPrefix;
    }

    public String getDriverClass() { return driverClass; }
    public String getJdbcPrefix()  { return jdbcPrefix;  }

    public static DbType fromUrl(String jdbcUrl) {
        if (jdbcUrl == null) return null;
        if (jdbcUrl.startsWith("jdbc:postgresql")) return POSTGRES;
        if (jdbcUrl.startsWith("jdbc:oracle"))    return ORACLE;
        throw new IllegalArgumentException("Cannot determine DbType from URL: " + jdbcUrl);
    }
}
