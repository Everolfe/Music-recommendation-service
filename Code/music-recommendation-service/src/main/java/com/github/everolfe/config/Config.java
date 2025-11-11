package com.github.everolfe.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

public class Config {
    private static final Logger logger = LoggerFactory.getLogger(Config.class);
    private static final Properties properties = new Properties();

    static {
        loadProperties();
    }

    private static void loadProperties() {
        try (InputStream input = Config.class.getClassLoader().getResourceAsStream("config/application.properties")) {
            if (input != null) {
                properties.load(input);
                logger.info("Конфигурация загружена успешно");
            } else {
                logger.warn("Файл конфигурации application.properties не найден, используются значения по умолчанию");
                setDefaultProperties();
            }
        } catch (IOException e) {
            logger.error("Ошибка при загрузке конфигурации", e);
            setDefaultProperties();
        }
    }

    private static void setDefaultProperties() {
        // Database configuration
        properties.setProperty("db.url", "jdbc:postgresql://localhost:5432/music-recommendation");
        properties.setProperty("db.username", "postgres");
        properties.setProperty("db.password", "0916310");
        properties.setProperty("db.pool.size", "10");

        // Last.fm API configuration
        properties.setProperty("lastfm.api.key", "fbc9ee44e9e0fee6ace0f3a8f3273e17");
        properties.setProperty("lastfm.api.secret", "b029df9b5e450b4248327ad36c1f2722");
        properties.setProperty("lastfm.base.url", "https://ws.audioscrobbler.com/2.0/");

        // Application settings
        properties.setProperty("app.name", "Music Recommendation Service");
        properties.setProperty("app.version", "1.0.0");

        // Development settings
        //properties.setProperty("demo.mode", "true");
        properties.setProperty("api.mock.enabled", "false");
    }

    // Database configuration
    public static String getDbUrl() {
        return properties.getProperty("db.url");
    }

    public static String getDbUsername() {
        return properties.getProperty("db.username");
    }

    public static String getDbPassword() {
        return properties.getProperty("db.password");
    }

    public static int getDbPoolSize() {
        return Integer.parseInt(properties.getProperty("db.pool.size", "10"));
    }

    // Last.fm API configuration
    public static String getLastFmApiKey() {
        return properties.getProperty("lastfm.api.key");
    }

    public static String getLastFmApiSecret() {
        return properties.getProperty("lastfm.api.secret");
    }

    public static String getLastFmBaseUrl() {
        return properties.getProperty("lastfm.base.url");
    }

    // Application settings
    public static String getAppName() {
        return properties.getProperty("app.name");
    }

    public static String getAppVersion() {
        return properties.getProperty("app.version");
    }

    // Development settings
    public static boolean isDemoMode() {
        return Boolean.parseBoolean(properties.getProperty("demo.mode", "true"));
    }

    public static boolean isApiMockEnabled() {
        return Boolean.parseBoolean(properties.getProperty("api.mock.enabled", "true"));
    }

    // Method to reload configuration if needed
    public static void reload() {
        properties.clear();
        loadProperties();
    }
}