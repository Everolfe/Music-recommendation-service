package com.github.everolfe.database.dao;

import com.github.everolfe.database.DatabaseConnection;
import com.github.everolfe.model.UserPreference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class UserPreferenceDAO {
    private static final Logger logger = LoggerFactory.getLogger(UserPreferenceDAO.class);

    public Optional<UserPreference> findByUserAndTrack(Long userId, Long trackId) {
        String sql = "SELECT up.*, t.title as track_title, a.name as artist_name " +
                "FROM user_preferences up " +
                "JOIN tracks t ON up.track_id = t.id " +
                "JOIN artists a ON t.artist_id = a.id " +
                "WHERE up.user_id = ? AND up.track_id = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setLong(1, userId);
            stmt.setLong(2, trackId);
            ResultSet rs = stmt.executeQuery();

            if (rs.next()) {
                return Optional.of(mapResultSetToUserPreference(rs));
            }
        } catch (SQLException e) {
            logger.error("Error finding user preference: userId={}, trackId={}", userId, trackId, e);
        }
        return Optional.empty();
    }

    public List<UserPreference> findByUserId(Long userId) {
        List<UserPreference> preferences = new ArrayList<>();
        String sql = "SELECT up.*, t.title as track_title, a.name as artist_name " +
                "FROM user_preferences up " +
                "JOIN tracks t ON up.track_id = t.id " +
                "JOIN artists a ON t.artist_id = a.id " +
                "WHERE up.user_id = ? " +
                "ORDER BY up.last_listened DESC";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setLong(1, userId);
            ResultSet rs = stmt.executeQuery();

            while (rs.next()) {
                preferences.add(mapResultSetToUserPreference(rs));
            }
        } catch (SQLException e) {
            logger.error("Error finding user preferences by user id: {}", userId, e);
        }
        return preferences;
    }

    public List<UserPreference> findFavoritesByUserId(Long userId) {
        List<UserPreference> favorites = new ArrayList<>();
        String sql = "SELECT up.*, t.title as track_title, a.name as artist_name " +
                "FROM user_preferences up " +
                "JOIN tracks t ON up.track_id = t.id " +
                "JOIN artists a ON t.artist_id = a.id " +
                "WHERE up.user_id = ? "  +
                "ORDER BY up.last_listened DESC";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setLong(1, userId);
            ResultSet rs = stmt.executeQuery();

            while (rs.next()) {
                favorites.add(mapResultSetToUserPreference(rs));
            }
        } catch (SQLException e) {
            logger.error("Error finding favorites by user id: {}", userId, e);
        }
        return favorites;
    }

    public List<UserPreference> findHighRatedByUserId(Long userId, int minRating) {
        List<UserPreference> highRated = new ArrayList<>();
        String sql = "SELECT up.*, t.title as track_title, a.name as artist_name " +
                "FROM user_preferences up " +
                "JOIN tracks t ON up.track_id = t.id " +
                "JOIN artists a ON t.artist_id = a.id " +
                "WHERE up.user_id = ? AND up.rating >= ? " +
                "ORDER BY up.rating DESC, up.last_listened DESC";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setLong(1, userId);
            stmt.setInt(2, minRating);
            ResultSet rs = stmt.executeQuery();

            while (rs.next()) {
                highRated.add(mapResultSetToUserPreference(rs));
            }
        } catch (SQLException e) {
            logger.error("Error finding high rated tracks by user id: {}", userId, e);
        }
        return highRated;
    }

    public boolean save(UserPreference preference) {
        if (preference.getId() == null) {
            return insert(preference);
        } else {
            return update(preference);
        }
    }

    private boolean insert(UserPreference preference) {
        String sql = "INSERT INTO user_preferences (user_id, track_id, rating, listened_count, last_listened, is_favorite) " +
                "VALUES (?, ?, ?, ?, ?, ?)";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

            stmt.setLong(1, preference.getUserId());
            stmt.setLong(2, preference.getTrackId());
            stmt.setInt(3, preference.getRating());
            stmt.setInt(4, preference.getListenedCount());
            stmt.setTimestamp(5, Timestamp.valueOf(preference.getLastListened()));
            stmt.setBoolean(6, preference.getIsFavorite());

            int affectedRows = stmt.executeUpdate();
            if (affectedRows > 0) {
                try (ResultSet generatedKeys = stmt.getGeneratedKeys()) {
                    if (generatedKeys.next()) {
                        preference.setId(generatedKeys.getLong(1));
                    }
                }
                return true;
            }
        } catch (SQLException e) {
            logger.error("Error inserting user preference: userId={}, trackId={}",
                    preference.getUserId(), preference.getTrackId(), e);
        }
        return false;
    }

    private boolean update(UserPreference preference) {
        String sql = "UPDATE user_preferences SET rating = ?, listened_count = ?, " +
                "last_listened = ?, is_favorite = ? WHERE id = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, preference.getRating());
            stmt.setInt(2, preference.getListenedCount());
            stmt.setTimestamp(3, Timestamp.valueOf(preference.getLastListened()));
            stmt.setBoolean(4, preference.getIsFavorite());
            stmt.setLong(5, preference.getId());

            return stmt.executeUpdate() > 0;
        } catch (SQLException e) {
            logger.error("Error updating user preference: {}", preference.getId(), e);
        }
        return false;
    }

    public boolean incrementListenCount(Long userId, Long trackId) {
        String sql = "INSERT INTO user_preferences (user_id, track_id, rating, listened_count, last_listened, is_favorite) " +
                "VALUES (?, ?, 3, 1, CURRENT_TIMESTAMP, false) " +
                "ON CONFLICT (user_id, track_id) DO UPDATE SET " +
                "listened_count = user_preferences.listened_count + 1, " +
                "last_listened = CURRENT_TIMESTAMP";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setLong(1, userId);
            stmt.setLong(2, trackId);

            return stmt.executeUpdate() > 0;
        } catch (SQLException e) {
            logger.error("Error incrementing listen count: userId={}, trackId={}", userId, trackId, e);
        }
        return false;
    }

    public boolean delete(Long id) {
        String sql = "DELETE FROM user_preferences WHERE id = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setLong(1, id);
            return stmt.executeUpdate() > 0;
        } catch (SQLException e) {
            logger.error("Error deleting user preference: {}", id, e);
        }
        return false;
    }

    private UserPreference mapResultSetToUserPreference(ResultSet rs) throws SQLException {
        UserPreference preference = new UserPreference();
        preference.setId(rs.getLong("id"));
        preference.setUserId(rs.getLong("user_id"));
        preference.setTrackId(rs.getLong("track_id"));
        preference.setRating(rs.getInt("rating"));
        preference.setListenedCount(rs.getInt("listened_count"));
        preference.setLastListened(rs.getTimestamp("last_listened").toLocalDateTime());
        preference.setIsFavorite(rs.getBoolean("is_favorite"));
        preference.setCreatedAt(rs.getTimestamp("created_at").toLocalDateTime());

        // Display fields
        preference.setTrackTitle(rs.getString("track_title"));
        preference.setArtistName(rs.getString("artist_name"));

        return preference;
    }
    public List<UserPreference> findRecentByUserId(Long userId, int limit) {
        List<UserPreference> preferences = new ArrayList<>();
        // Предполагаем, что у UserPreference есть поле created_at
        String sql = "SELECT * FROM user_preferences WHERE user_id = ? ORDER BY created_at DESC LIMIT ?";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setLong(1, userId);
            stmt.setInt(2, limit);
            ResultSet rs = stmt.executeQuery();

            while (rs.next()) {
                preferences.add(mapResultSetToUserPreference(rs));
            }
        } catch (SQLException e) {
            logger.error("Error finding recent preferences for user: {}", userId, e);
        }
        return preferences;
    }
}