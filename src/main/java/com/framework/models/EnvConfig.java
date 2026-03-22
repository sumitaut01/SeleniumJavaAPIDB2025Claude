package com.framework.models;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;
import java.util.Map;

/**
 * Maps the full structure of env-config.json.
 *
 * Shape:
 * {
 *   "environments": {
 *     "qa": {
 *       "IN": {
 *         "baseUrl":  "...",
 *         "apiUrl":   "...",
 *         "postgres": { "url": "...", "username": "...", "password": "...", "schema": "..." },
 *         "oracle":   { "url": "...", "username": "...", "password": "...", "schema": "..." }
 *       }
 *     }
 *   }
 * }
 */
@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class EnvConfig {
    private Map<String, Map<String, CountryConfig>> environments;

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class CountryConfig {
        private String   baseUrl;
        private String   apiUrl;
        private DbConfig postgres;   // nullable if not used in this env
        private DbConfig oracle;     // nullable if not used in this env
    }

    /**
     * Database connection config block — shared by both Postgres and Oracle entries.
     */
    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class DbConfig {
        private String url;
        private String username;
        private String password;
        private String schema;        // default schema (postgres) or service name (oracle)
        private int    poolSize = 5;  // HikariCP pool size per test thread group
    }
}
