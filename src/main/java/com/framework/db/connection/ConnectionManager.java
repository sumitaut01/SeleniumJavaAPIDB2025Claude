package com.framework.db.connection;

import com.framework.config.ConfigManager;
import com.framework.enums.DbType;
import com.framework.models.EnvConfig;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Manages HikariCP connection pools — one pool per DbType.
 *
 * Design decisions:
 *  - Pools are created lazily on first use and cached in a ConcurrentHashMap
 *  - Each pool is keyed by DbType so POSTGRES and ORACLE are completely independent
 *  - Connections are obtained from the pool per-test and returned via close()
 *    (use try-with-resources — Connection.close() returns to pool, doesn't close it)
 *  - Pools are shut down in a JVM shutdown hook (safe for CI pipelines)
 *
 * Usage:
 *   try (Connection conn = ConnectionManager.getConnection(DbType.POSTGRES)) {
 *       // execute queries
 *   }
 *   // connection automatically returned to pool
 */
public class ConnectionManager {

    private static final Logger log = LogManager.getLogger(ConnectionManager.class);

    /** One DataSource per DbType. ConcurrentHashMap for thread-safe lazy init. */
    private static final Map<DbType, HikariDataSource> pools = new ConcurrentHashMap<>();

    static {
        Runtime.getRuntime().addShutdownHook(new Thread(ConnectionManager::closeAll,
                "db-pool-shutdown"));
    }

    private ConnectionManager() {}

    // ── Public API ────────────────────────────────────────────────────────────

    /**
     * Borrows a connection from the pool for the given DbType.
     * Creates the pool on first call.
     * Always use in try-with-resources.
     *
     * @throws IllegalStateException if no DB config exists for the requested type
     */
    public static Connection getConnection(DbType type) throws SQLException {
        HikariDataSource ds = pools.computeIfAbsent(type, ConnectionManager::createPool);
        return ds.getConnection();
    }

    /**
     * True if the given DbType is configured for the active env + country.
     * Use this as a guard before calling getConnection() in optional DB steps.
     */
    public static boolean isAvailable(DbType type) {
        return ConfigManager.getInstance().hasDb(type);
    }

    /** Shuts down all active pools. Called by JVM shutdown hook. */
    public static void closeAll() {
        pools.forEach((type, ds) -> {
            if (!ds.isClosed()) {
                log.info("Closing HikariCP pool for: {}", type);
                ds.close();
            }
        });
        pools.clear();
    }

    // ── Pool factory ──────────────────────────────────────────────────────────

    private static HikariDataSource createPool(DbType type) {
        ConfigManager cfg    = ConfigManager.getInstance();
        EnvConfig.DbConfig db = cfg.getDbConfig(type);

        if (db == null || db.getUrl() == null || db.getUrl().isBlank()) {
            throw new IllegalStateException(
                    "No DB config for type=" + type
                    + " | env=" + cfg.getEnvironment()
                    + " | country=" + cfg.getCountry());
        }

        log.info("Creating HikariCP pool → type={}, url={}, poolSize={}",
                type, db.getUrl(), db.getPoolSize());

        HikariConfig hikari = new HikariConfig();
        hikari.setJdbcUrl(db.getUrl());
        hikari.setUsername(db.getUsername());
        hikari.setPassword(db.getPassword());
        hikari.setDriverClassName(type.getDriverClass());

        // Pool sizing
        hikari.setMaximumPoolSize(Math.max(db.getPoolSize(), 2));
        hikari.setMinimumIdle(1);

        // Timeouts (tuned for test automation — fail fast)
        hikari.setConnectionTimeout(15_000);   // 15s to get a connection from pool
        hikari.setIdleTimeout(300_000);         // 5min idle before eviction
        hikari.setMaxLifetime(600_000);         // 10min max connection lifetime
        hikari.setConnectionTestQuery(type == DbType.ORACLE ? "SELECT 1 FROM DUAL" : "SELECT 1");

        hikari.setPoolName("Framework-" + type.name() + "-Pool");

        // Oracle-specific schema
        if (type == DbType.ORACLE && db.getSchema() != null && !db.getSchema().isBlank()) {
            hikari.addDataSourceProperty("currentSchema", db.getSchema());
        }

        // Postgres-specific schema search path
        if (type == DbType.POSTGRES && db.getSchema() != null && !db.getSchema().isBlank()) {
            hikari.setConnectionInitSql("SET search_path TO " + db.getSchema());
        }

        return new HikariDataSource(hikari);
    }
}
