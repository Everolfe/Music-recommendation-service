package com.github.everolfe.database.dao;

import com.github.everolfe.database.DatabaseConnection;
import com.github.everolfe.model.Track;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class TrackDAO {
    private static final Logger logger = LoggerFactory.getLogger(TrackDAO.class);

    public Optional<Track> findById(Long id) {
        String sql = "SELECT t.*, a.name as artist_name, al.title as album_title " +
                "FROM tracks t " +
                "LEFT JOIN artists a ON t.artist_id = a.id " +
                "LEFT JOIN albums al ON t.album_id = al.id " +
                "WHERE t.id = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setLong(1, id);
            ResultSet rs = stmt.executeQuery();

            if (rs.next()) {
                return Optional.of(mapResultSetToTrack(rs));
            }
        } catch (SQLException e) {
            logger.error("Error finding track by id: {}", id, e);
        }
        return Optional.empty();
    }

    public List<Track> findAll() {
        List<Track> tracks = new ArrayList<>();
        String sql = "SELECT t.*, a.name as artist_name, al.title as album_title " +
                "FROM tracks t " +
                "LEFT JOIN artists a ON t.artist_id = a.id " +
                "LEFT JOIN albums al ON t.album_id = al.id " +
                "ORDER BY a.name, t.title";

        try (Connection conn = DatabaseConnection.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {

            while (rs.next()) {
                tracks.add(mapResultSetToTrack(rs));
            }
        } catch (SQLException e) {
            logger.error("Error finding all tracks", e);
        }
        return tracks;
    }

    public List<Track> findByArtist(String artistName) {
        List<Track> tracks = new ArrayList<>();
        String sql = "SELECT t.*, a.name as artist_name, al.title as album_title " +
                "FROM tracks t " +
                "JOIN artists a ON t.artist_id = a.id " +
                "LEFT JOIN albums al ON t.album_id = al.id " +
                "WHERE a.name ILIKE ? " +
                "ORDER BY t.title";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, "%" + artistName + "%");
            ResultSet rs = stmt.executeQuery();

            while (rs.next()) {
                tracks.add(mapResultSetToTrack(rs));
            }
        } catch (SQLException e) {
            logger.error("Error finding tracks by artist: {}", artistName, e);
        }
        return tracks;
    }

    public List<Track> findByAlbum(Long albumId) {
        List<Track> tracks = new ArrayList<>();
        String sql = "SELECT t.*, a.name as artist_name, al.title as album_title " +
                "FROM tracks t " +
                "JOIN artists a ON t.artist_id = a.id " +
                "JOIN albums al ON t.album_id = al.id " +
                "WHERE t.album_id = ? " +
                "ORDER BY t.track_number";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setLong(1, albumId);
            ResultSet rs = stmt.executeQuery();

            while (rs.next()) {
                tracks.add(mapResultSetToTrack(rs));
            }
        } catch (SQLException e) {
            logger.error("Error finding tracks by album: {}", albumId, e);
        }
        return tracks;
    }

    public List<Track> findByTitle(String title) {
        List<Track> tracks = new ArrayList<>();
        String sql = "SELECT t.*, a.name as artist_name, al.title as album_title " +
                "FROM tracks t " +
                "LEFT JOIN artists a ON t.artist_id = a.id " +
                "LEFT JOIN albums al ON t.album_id = al.id " +
                "WHERE t.title ILIKE ? " +
                "ORDER BY a.name, t.title";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, "%" + title + "%");
            ResultSet rs = stmt.executeQuery();

            while (rs.next()) {
                tracks.add(mapResultSetToTrack(rs));
            }
        } catch (SQLException e) {
            logger.error("Error finding tracks by title: {}", title, e);
        }
        return tracks;
    }

    public List<Track> findByGenre(String genreName) {
        List<Track> tracks = new ArrayList<>();
        String sql = "SELECT DISTINCT t.*, a.name as artist_name, al.title as album_title " +
                "FROM tracks t " +
                "JOIN track_genres tg ON t.id = tg.track_id " +
                "JOIN genres g ON tg.genre_id = g.id " +
                "LEFT JOIN artists a ON t.artist_id = a.id " +
                "LEFT JOIN albums al ON t.album_id = al.id " +
                "WHERE g.name ILIKE ? " +
                "ORDER BY a.name, t.title";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, "%" + genreName + "%");
            ResultSet rs = stmt.executeQuery();

            while (rs.next()) {
                tracks.add(mapResultSetToTrack(rs));
            }
        } catch (SQLException e) {
            logger.error("Error finding tracks by genre: {}", genreName, e);
        }
        return tracks;
    }

    public boolean insertApiTrack(Track track) {
        Connection conn = null;
        try {
            conn = DatabaseConnection.getConnection();
            conn.setAutoCommit(false); // Начинаем транзакцию

            // 1. Создаем или находим артиста
            Long artistId = findOrCreateArtist(conn, track.getArtistName());

            // 2. Создаем или находим альбом
            Long albumId = findOrCreateAlbum(conn, track.getAlbumTitle(), artistId);

            // 3. Создаем трек
            String sql = "INSERT INTO tracks (title, artist_id, album_id, duration, last_fm_id, created_at) " +
                    "VALUES (?, ?, ?, ?, ?, CURRENT_TIMESTAMP)";

            try (PreparedStatement stmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
                stmt.setString(1, track.getTitle());
                stmt.setLong(2, artistId);
                stmt.setLong(3, albumId);
                stmt.setInt(4, track.getDuration());
                stmt.setString(5, generateLastFmId(track.getArtistName(), track.getTitle()));

                int affectedRows = stmt.executeUpdate();
                if (affectedRows > 0) {
                    try (ResultSet generatedKeys = stmt.getGeneratedKeys()) {
                        if (generatedKeys.next()) {
                            track.setId(generatedKeys.getLong(1));
                            track.setArtistId(artistId);
                            track.setAlbumId(albumId);

                            conn.commit(); // Подтверждаем транзакцию
                            return true;
                        }
                    }
                }
            }

            conn.rollback(); // Откатываем в случае ошибки
            return false;

        } catch (SQLException e) {
            if (conn != null) {
                try {
                    conn.rollback();
                } catch (SQLException ex) {
                    logger.error("Error rolling back transaction", ex);
                }
            }
            logger.error("Error inserting API track: {}", track.getTitle(), e);
            return false;
        } finally {
            if (conn != null) {
                try {
                    conn.setAutoCommit(true);
                    conn.close();
                } catch (SQLException e) {
                    logger.error("Error closing connection", e);
                }
            }
        }
    }

    /**
     * Находит или создает артиста
     */
    private Long findOrCreateArtist(Connection conn, String artistName) throws SQLException {
        // Сначала ищем существующего артиста
        String findSql = "SELECT id FROM artists WHERE name ILIKE ?";
        try (PreparedStatement stmt = conn.prepareStatement(findSql)) {
            stmt.setString(1, artistName);
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                return rs.getLong("id");
            }
        }

        // Если не нашли, создаем нового
        String insertSql = "INSERT INTO artists (name, created_at) VALUES (?, CURRENT_TIMESTAMP)";
        try (PreparedStatement stmt = conn.prepareStatement(insertSql, Statement.RETURN_GENERATED_KEYS)) {
            stmt.setString(1, artistName);
            int affectedRows = stmt.executeUpdate();
            if (affectedRows > 0) {
                try (ResultSet generatedKeys = stmt.getGeneratedKeys()) {
                    if (generatedKeys.next()) {
                        return generatedKeys.getLong(1);
                    }
                }
            }
        }
        throw new SQLException("Failed to create artist: " + artistName);
    }

    /**
     * Находит или создает альбом
     */
    private Long findOrCreateAlbum(Connection conn, String albumTitle, Long artistId) throws SQLException {
        // Сначала ищем существующий альбом
        String findSql = "SELECT id FROM albums WHERE title ILIKE ? AND artist_id = ?";
        try (PreparedStatement stmt = conn.prepareStatement(findSql)) {
            stmt.setString(1, albumTitle);
            stmt.setLong(2, artistId);
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                return rs.getLong("id");
            }
        }

        // Если не нашли, создаем новый
        String insertSql = "INSERT INTO albums (title, artist_id, created_at) VALUES (?, ?, CURRENT_TIMESTAMP)";
        try (PreparedStatement stmt = conn.prepareStatement(insertSql, Statement.RETURN_GENERATED_KEYS)) {
            stmt.setString(1, albumTitle);
            stmt.setLong(2, artistId);
            int affectedRows = stmt.executeUpdate();
            if (affectedRows > 0) {
                try (ResultSet generatedKeys = stmt.getGeneratedKeys()) {
                    if (generatedKeys.next()) {
                        return generatedKeys.getLong(1);
                    }
                }
            }
        }
        throw new SQLException("Failed to create album: " + albumTitle);
    }

    private String generateLastFmId(String artist, String title) {
        return (artist + "_" + title)
                .toLowerCase()
                .replaceAll("[^a-z0-9]", "_")
                .replaceAll("_+", "_");
    }

    // Обновляем метод save
    public boolean save(Track track) {
        try {
            if (track.getId() == null || track.getId() <= 0) {
                // Для API треков используем специальный метод
                if (track.getArtistId() == null && track.getArtistName() != null) {
                    return insertApiTrack(track);
                } else {
                    // Для обычных треков с artist_id
                    return insert(track);
                }
            } else {
                // Обновление существующего трека
                return update(track);
            }
        } catch (Exception e) {
            logger.error("Error saving track: {}", track.getTitle(), e);
            return false;
        }
    }


    private boolean insert(Track track) {
        String sql = "INSERT INTO tracks (title, artist_id, album_id, duration, track_number, " +
                "last_fm_id, acousticness, danceability, energy, instrumentalness, " +
                "liveness, loudness, speechiness, tempo, valence) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

            stmt.setString(1, track.getTitle());
            stmt.setLong(2, track.getArtistId());
            stmt.setLong(3, track.getAlbumId());
            stmt.setInt(4, track.getDuration());
            stmt.setInt(5, track.getTrackNumber());
            stmt.setString(6, track.getLastFmId());
            stmt.setDouble(7, track.getAcousticness());
            stmt.setDouble(8, track.getDanceability());
            stmt.setDouble(9, track.getEnergy());
            stmt.setDouble(10, track.getInstrumentalness());
            stmt.setDouble(11, track.getLiveness());
            stmt.setDouble(12, track.getLoudness());
            stmt.setDouble(13, track.getSpeechiness());
            stmt.setDouble(14, track.getTempo());
            stmt.setDouble(15, track.getValence());

            int affectedRows = stmt.executeUpdate();
            if (affectedRows > 0) {
                try (ResultSet generatedKeys = stmt.getGeneratedKeys()) {
                    if (generatedKeys.next()) {
                        track.setId(generatedKeys.getLong(1));
                    }
                }
                return true;
            }
        } catch (SQLException e) {
            logger.error("Error inserting track: {}", track.getTitle(), e);
        }
        return false;
    }

    private boolean update(Track track) {
        String sql = "UPDATE tracks SET title = ?, artist_id = ?, album_id = ?, duration = ?, " +
                "track_number = ?, last_fm_id = ?, acousticness = ?, danceability = ?, " +
                "energy = ?, instrumentalness = ?, liveness = ?, loudness = ?, " +
                "speechiness = ?, tempo = ?, valence = ? WHERE id = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, track.getTitle());
            stmt.setLong(2, track.getArtistId());
            stmt.setLong(3, track.getAlbumId());
            stmt.setInt(4, track.getDuration());
            stmt.setInt(5, track.getTrackNumber());
            stmt.setString(6, track.getLastFmId());
            stmt.setDouble(7, track.getAcousticness());
            stmt.setDouble(8, track.getDanceability());
            stmt.setDouble(9, track.getEnergy());
            stmt.setDouble(10, track.getInstrumentalness());
            stmt.setDouble(11, track.getLiveness());
            stmt.setDouble(12, track.getLoudness());
            stmt.setDouble(13, track.getSpeechiness());
            stmt.setDouble(14, track.getTempo());
            stmt.setDouble(15, track.getValence());
            stmt.setLong(16, track.getId());

            return stmt.executeUpdate() > 0;
        } catch (SQLException e) {
            logger.error("Error updating track: {}", track.getId(), e);
        }
        return false;
    }

    public boolean delete(Long id) {
        String sql = "DELETE FROM tracks WHERE id = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setLong(1, id);
            return stmt.executeUpdate() > 0;
        } catch (SQLException e) {
            logger.error("Error deleting track: {}", id, e);
        }
        return false;
    }

    private Track mapResultSetToTrack(ResultSet rs) throws SQLException {
        Track track = new Track();
        track.setId(rs.getLong("id"));
        track.setTitle(rs.getString("title"));
        track.setArtistId(rs.getLong("artist_id"));
        track.setAlbumId(rs.getLong("album_id"));
        track.setDuration(rs.getInt("duration"));
        track.setTrackNumber(rs.getInt("track_number"));
        track.setLastFmId(rs.getString("last_fm_id"));

        // Audio features
        track.setAcousticness(rs.getDouble("acousticness"));
        track.setDanceability(rs.getDouble("danceability"));
        track.setEnergy(rs.getDouble("energy"));
        track.setInstrumentalness(rs.getDouble("instrumentalness"));
        track.setLiveness(rs.getDouble("liveness"));
        track.setLoudness(rs.getDouble("loudness"));
        track.setSpeechiness(rs.getDouble("speechiness"));
        track.setTempo(rs.getDouble("tempo"));
        track.setValence(rs.getDouble("valence"));

        track.setCreatedAt(rs.getTimestamp("created_at").toLocalDateTime());

        // Display fields
        track.setArtistName(rs.getString("artist_name"));
        track.setAlbumTitle(rs.getString("album_title"));

        return track;
    }
}