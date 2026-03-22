package com.framework.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.framework.constants.FrameworkConstants;
import com.framework.enums.Browser;
import com.framework.enums.Country;
import com.framework.enums.DbType;
import com.framework.enums.Environment;
import com.framework.enums.ExecutionType;
import com.framework.models.EnvConfig;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.File;
import java.io.IOException;

/**
 * Singleton — resolves all runtime parameters.
 *
 * Sources:
 *  1. System properties  : -Denv, -Dcountry, -Dexecution, -Dbrowser
 *  2. env-config.json    : URLs + DB credentials per env + country
 *
 * DB accessors:
 *   getDbConfig(DbType.POSTGRES)  →  EnvConfig.DbConfig for postgres
 *   getDbConfig(DbType.ORACLE)    →  EnvConfig.DbConfig for oracle
 */
public class ConfigManager {

    private static final Logger log = LogManager.getLogger(ConfigManager.class);
    private static volatile ConfigManager instance;

    private final Environment          environment;
    private final ExecutionType        executionType;
    private final Country              country;
    private final Browser              browser;
    private final EnvConfig.CountryConfig countryConfig;

    private ConfigManager() {
        this.environment   = Environment.fromString(System.getProperty("env",       "qa"));
        this.executionType = ExecutionType.fromString(System.getProperty("execution","local"));
        this.country       = Country.fromString(System.getProperty("country",       "IN"));
        this.browser       = Browser.fromString(System.getProperty("browser",       "chrome"));

        log.info("Framework init → env={}, execution={}, country={}, browser={}",
                environment, executionType, country, browser);

        this.countryConfig = loadCountryConfig();
    }

    public static ConfigManager getInstance() {
        if (instance == null) {
            synchronized (ConfigManager.class) {
                if (instance == null) instance = new ConfigManager();
            }
        }
        return instance;
    }

    // ── Web / API ─────────────────────────────────────────────────────────────
    public Environment   getEnvironment()   { return environment;   }
    public ExecutionType getExecutionType() { return executionType; }
    public Country       getCountry()       { return country;       }
    public Browser       getBrowser()       { return browser;       }
    public String        getBaseUrl()       { return countryConfig.getBaseUrl(); }
    public String        getApiUrl()        { return countryConfig.getApiUrl();  }

    // ── Database ──────────────────────────────────────────────────────────────
    /**
     * Returns the DbConfig for the given DbType (POSTGRES or ORACLE).
     * Returns null if that DB is not configured for the active env+country
     * (so callers can skip DB steps gracefully rather than crashing).
     */
    public EnvConfig.DbConfig getDbConfig(DbType type) {
        return switch (type) {
            case POSTGRES -> countryConfig.getPostgres();
            case ORACLE   -> countryConfig.getOracle();
        };
    }

    /** Convenience: true if a DB config block exists for the given type. */
    public boolean hasDb(DbType type) {
        EnvConfig.DbConfig cfg = getDbConfig(type);
        return cfg != null && cfg.getUrl() != null && !cfg.getUrl().isBlank();
    }

    // ── Grid ──────────────────────────────────────────────────────────────────
    public String getGridUrl() {
        String url = System.getenv(FrameworkConstants.REMOTE_URL_KEY);
        return (url != null && !url.isBlank()) ? url : FrameworkConstants.DEFAULT_GRID_URL;
    }

    // ── Private ───────────────────────────────────────────────────────────────
    private EnvConfig.CountryConfig loadCountryConfig() {
        try {
            File configFile = new File(FrameworkConstants.ENV_CONFIG_FILE);
            if (!configFile.exists()) {
                throw new RuntimeException("env-config.json not found at: "
                        + configFile.getAbsolutePath());
            }
            EnvConfig envConfig = new ObjectMapper().readValue(configFile, EnvConfig.class);

            String envKey     = environment.name().toLowerCase();
            String countryKey = country.name();

            EnvConfig.CountryConfig cfg = envConfig.getEnvironments()
                    .getOrDefault(envKey, java.util.Collections.emptyMap())
                    .get(countryKey);

            if (cfg == null) {
                throw new RuntimeException(
                        "No config found for env=" + envKey + " + country=" + countryKey);
            }
            log.info("Config loaded → baseUrl={}", cfg.getBaseUrl());
            return cfg;

        } catch (IOException e) {
            throw new RuntimeException("Failed to load env-config.json", e);
        }
    }
}
