package com.github.everolfe.database;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.Connection;
import java.sql.Statement;

public class DatabaseInitializer {
    private static final Logger logger = LoggerFactory.getLogger(DatabaseInitializer.class);

    public static void initialize() {
        createTables();
        insertSampleData();
    }

    private static void createTables() {
        String[] createTables = {
                // Пользователи
                "CREATE TABLE IF NOT EXISTS users (" +
                        "id SERIAL PRIMARY KEY," +
                        "username VARCHAR(100) UNIQUE NOT NULL," +
                        "email VARCHAR(255) UNIQUE NOT NULL," +
                        "password_hash VARCHAR(255)," +
                        "created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP," +
                        "is_active BOOLEAN DEFAULT true" +
                        ")",

                // Исполнители
                "CREATE TABLE IF NOT EXISTS artists (" +
                        "id SERIAL PRIMARY KEY," +
                        "name VARCHAR(255) NOT NULL," +
                        "last_fm_id VARCHAR(100)," +
                        "bio TEXT," +
                        "image_url VARCHAR(500)," +
                        "created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP" +
                        ")",

                // Альбомы
                "CREATE TABLE IF NOT EXISTS albums (" +
                        "id SERIAL PRIMARY KEY," +
                        "title VARCHAR(255) NOT NULL," +
                        "artist_id INTEGER REFERENCES artists(id)," +
                        "release_year INTEGER," +
                        "last_fm_id VARCHAR(100)," +
                        "cover_url VARCHAR(500)," +
                        "created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP" +
                        ")",

                // Жанры
                "CREATE TABLE IF NOT EXISTS genres (" +
                        "id SERIAL PRIMARY KEY," +
                        "name VARCHAR(100) UNIQUE NOT NULL," +
                        "description TEXT" +
                        ")",

                // Треки
                "CREATE TABLE IF NOT EXISTS tracks (" +
                        "id SERIAL PRIMARY KEY," +
                        "title VARCHAR(255) NOT NULL," +
                        "artist_id INTEGER REFERENCES artists(id)," +
                        "album_id INTEGER REFERENCES albums(id)," +
                        "duration INTEGER," +
                        "track_number INTEGER," +
                        "last_fm_id VARCHAR(100)," +
                        "acousticness DOUBLE PRECISION," +
                        "danceability DOUBLE PRECISION," +
                        "energy DOUBLE PRECISION," +
                        "instrumentalness DOUBLE PRECISION," +
                        "liveness DOUBLE PRECISION," +
                        "loudness DOUBLE PRECISION," +
                        "speechiness DOUBLE PRECISION," +
                        "tempo DOUBLE PRECISION," +
                        "valence DOUBLE PRECISION," +
                        "created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP" +
                        ")",

                // Связь треков и жанров
                "CREATE TABLE IF NOT EXISTS track_genres (" +
                        "track_id INTEGER REFERENCES tracks(id)," +
                        "genre_id INTEGER REFERENCES genres(id)," +
                        "PRIMARY KEY (track_id, genre_id)" +
                        ")",

                // Пользовательские предпочтения
                "CREATE TABLE IF NOT EXISTS user_preferences (" +
                        "id SERIAL PRIMARY KEY," +
                        "user_id INTEGER REFERENCES users(id)," +
                        "track_id INTEGER REFERENCES tracks(id)," +
                        "rating INTEGER CHECK (rating >= 1 AND rating <= 5)," +
                        "listened_count INTEGER DEFAULT 0," +
                        "last_listened TIMESTAMP," +
                        "is_favorite BOOLEAN DEFAULT false," +
                        "created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP," +
                        "UNIQUE(user_id, track_id)" +
                        ")",

                // Рекомендации
                "CREATE TABLE IF NOT EXISTS recommendations (" +
                        "id SERIAL PRIMARY KEY," +
                        "user_id INTEGER REFERENCES users(id)," +
                        "track_id INTEGER REFERENCES tracks(id)," +
                        "recommendation_type VARCHAR(50)," +
                        "score DOUBLE PRECISION," +
                        "created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP," +
                        "is_viewed BOOLEAN DEFAULT false" +
                        ")"
        };

        try (Connection conn = DatabaseConnection.getConnection();
             Statement stmt = conn.createStatement()) {

            for (String sql : createTables) {
                stmt.execute(sql);
            }
            logger.info("Все таблицы созданы успешно");

        } catch (Exception e) {
            logger.error("Ошибка при создании таблиц: {}", e.getMessage());
        }
    }

    public static void insertSampleData() {
        String[] sampleData = {
                // Жанры
                "INSERT INTO genres (name, description) VALUES " +
                        "('Rock', 'Rock music')," +
                        "('Pop', 'Popular music')," +
                        "('Jazz', 'Jazz music')," +
                        "('Classical', 'Classical music')," +
                        "('Hip-Hop', 'Hip-Hop music')," +
                        "('Electronic', 'Electronic music')," +
                        "('R&B', 'Rhythm and Blues')" +
                        "ON CONFLICT (name) DO NOTHING",

                // Исполнители
                "INSERT INTO artists (name) VALUES " +
                        "('Queen')," +
                        "('The Weeknd')," +
                        "('Ed Sheeran')," +
                        "('Led Zeppelin')," +
                        "('Billie Eilish')," +
                        "('Daft Punk')," +
                        "('Adele')" +
                        "ON CONFLICT DO NOTHING",

                // Альбомы
                "INSERT INTO albums (title, artist_id, release_year) VALUES " +
                        "('A Night at the Opera', 1, 1975)," +
                        "('After Hours', 2, 2020)," +
                        "('÷', 3, 2017)," +
                        "('Led Zeppelin IV', 4, 1971)," +
                        "('When We All Fall Asleep', 5, 2019)," +
                        "('Random Access Memories', 6, 2013)," +
                        "('21', 7, 2011)" +
                        "ON CONFLICT DO NOTHING",

                // Треки
                "INSERT INTO tracks (title, artist_id, album_id, duration, track_number) VALUES " +
                        "('Bohemian Rhapsody', 1, 1, 354, 1)," +
                        "('Blinding Lights', 2, 2, 200, 1)," +
                        "('Shape of You', 3, 3, 233, 1)," +
                        "('Stairway to Heaven', 4, 4, 482, 1)," +
                        "('Bad Guy', 5, 5, 194, 1)," +
                        "('Get Lucky', 6, 6, 248, 1)," +
                        "('Rolling in the Deep', 7, 7, 228, 1)" +
                        "ON CONFLICT DO NOTHING",

                // Связи треков и жанров
                "INSERT INTO track_genres (track_id, genre_id) VALUES " +
                        "(1, 1), " + // Bohemian Rhapsody -> Rock
                        "(2, 2), " + // Blinding Lights -> Pop
                        "(3, 2), " + // Shape of You -> Pop
                        "(4, 1), " + // Stairway to Heaven -> Rock
                        "(5, 2), " + // Bad Guy -> Pop
                        "(6, 6), " + // Get Lucky -> Electronic
                        "(7, 2)" +   // Rolling in the Deep -> Pop
                        "ON CONFLICT DO NOTHING",

                // Пользователи
                "INSERT INTO users (username, email) VALUES " +
                        "('admin', 'admin@music.com')," +
                        "('user1', 'user1@music.com')," +
                        "('guest', 'guest@music.com')" +
                        "ON CONFLICT (username) DO NOTHING",

                // Пользовательские предпочтения
                "INSERT INTO user_preferences (user_id, track_id, rating, is_favorite) VALUES " +
                        "(1, 1, 5, true)," +
                        "(1, 2, 4, false)," +
                        "(2, 3, 5, true)," +
                        "(2, 4, 3, false)," +
                        "(3, 5, 4, true)" +
                        "ON CONFLICT DO NOTHING"
        };

        try (Connection conn = DatabaseConnection.getConnection();
             Statement stmt = conn.createStatement()) {

            for (String sql : sampleData) {
                stmt.execute(sql);
            }
            logger.info("Тестовые данные добавлены");

        } catch (Exception e) {
            logger.error("Ошибка при добавлении тестовых данных: {}", e.getMessage());
        }
    }

    public static boolean testConnection() {
        try (Connection conn = DatabaseConnection.getConnection()) {
            return conn != null && !conn.isClosed();
        } catch (Exception e) {
            logger.error("Ошибка подключения к БД: {}", e.getMessage());
            return false;
        }
    }
}