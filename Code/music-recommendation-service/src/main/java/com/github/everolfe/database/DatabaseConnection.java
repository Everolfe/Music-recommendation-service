package com.github.everolfe.database;

import com.github.everolfe.config.Config;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.Connection;
import java.sql.SQLException;

public class DatabaseConnection {
    private static final Logger logger = LoggerFactory.getLogger(DatabaseConnection.class);
    private static HikariDataSource dataSource;
    private static boolean initialized = false;

    public static synchronized void initialize() {
        if (initialized) return;

        try {
            HikariConfig config = new HikariConfig();
            config.setJdbcUrl(Config.getDbUrl());
            config.setUsername(Config.getDbUsername());
            config.setPassword(Config.getDbPassword());
            config.setMaximumPoolSize(Config.getDbPoolSize());
            config.setMinimumIdle(2);
            config.setConnectionTimeout(30000);
            config.setIdleTimeout(300000);
            config.setMaxLifetime(600000);

            dataSource = new HikariDataSource(config);
            initialized = true;
            logger.info("Database connection pool initialized successfully");

        } catch (Exception e) {
            logger.error("Failed to initialize database connection pool: {}", e.getMessage());
            dataSource = null;
        }
    }

    public static Connection getConnection() throws SQLException {
        if (!initialized) {
            initialize();
        }
        if (dataSource == null) {
            throw new SQLException("Database connection is not available. Please check your database configuration.");
        }
        return dataSource.getConnection();
    }

    public static boolean isInitialized() {
        return initialized && dataSource != null;
    }

    public static void closeDataSource() {
        if (dataSource != null && !dataSource.isClosed()) {
            dataSource.close();
            logger.info("Database connection pool closed");
        }
    }
}