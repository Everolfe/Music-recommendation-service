package com.github.everolfe.database.dao;

import com.github.everolfe.database.DatabaseConnection;
import com.github.everolfe.model.Recommendation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class RecommendationDAO {
    private static final Logger logger = LoggerFactory.getLogger(RecommendationDAO.class);

    public Optional<Recommendation> findById(Long id) {
        String sql = "SELECT r.*, t.title as track_title, a.name as artist_name, al.title as album_title " +
                "FROM recommendations r " +
                "JOIN tracks t ON r.track_id = t.id " +
                "JOIN artists a ON t.artist_id = a.id " +
                "LEFT JOIN albums al ON t.album_id = al.id " +
                "WHERE r.id = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setLong(1, id);
            ResultSet rs = stmt.executeQuery();

            if (rs.next()) {
                return Optional.of(mapResultSetToRecommendation(rs));
            }
        } catch (SQLException e) {
            logger.error("Error finding recommendation by id: {}", id, e);
        }
        return Optional.empty();
    }

    public List<Recommendation> findByUserId(Long userId) {
        List<Recommendation> recommendations = new ArrayList<>();
        String sql = "SELECT r.*, t.title as track_title, a.name as artist_name, al.title as album_title " +
                "FROM recommendations r " +
                "JOIN tracks t ON r.track_id = t.id " +
                "JOIN artists a ON t.artist_id = a.id " +
                "LEFT JOIN albums al ON t.album_id = al.id " +
                "WHERE r.user_id = ? " +
                "ORDER BY r.score DESC, r.created_at DESC";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setLong(1, userId);
            ResultSet rs = stmt.executeQuery();

            while (rs.next()) {
                recommendations.add(mapResultSetToRecommendation(rs));
            }
        } catch (SQLException e) {
            logger.error("Error finding recommendations by user id: {}", userId, e);
        }
        return recommendations;
    }

    public List<Recommendation> findUnviewedByUserId(Long userId) {
        List<Recommendation> recommendations = new ArrayList<>();
        String sql = "SELECT r.*, t.title as track_title, a.name as artist_name, al.title as album_title " +
                "FROM recommendations r " +
                "JOIN tracks t ON r.track_id = t.id " +
                "JOIN artists a ON t.artist_id = a.id " +
                "LEFT JOIN albums al ON t.album_id = al.id " +
                "WHERE r.user_id = ? AND r.is_viewed = false " +
                "ORDER BY r.score DESC";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setLong(1, userId);
            ResultSet rs = stmt.executeQuery();

            while (rs.next()) {
                recommendations.add(mapResultSetToRecommendation(rs));
            }
        } catch (SQLException e) {
            logger.error("Error finding unviewed recommendations by user id: {}", userId, e);
        }
        return recommendations;
    }

    public List<Recommendation> findByTypeAndUserId(String recommendationType, Long userId) {
        List<Recommendation> recommendations = new ArrayList<>();
        String sql = "SELECT r.*, t.title as track_title, a.name as artist_name, al.title as album_title " +
                "FROM recommendations r " +
                "JOIN tracks t ON r.track_id = t.id " +
                "JOIN artists a ON t.artist_id = a.id " +
                "LEFT JOIN albums al ON t.album_id = al.id " +
                "WHERE r.user_id = ? AND r.recommendation_type = ? " +
                "ORDER BY r.score DESC";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setLong(1, userId);
            stmt.setString(2, recommendationType);
            ResultSet rs = stmt.executeQuery();

            while (rs.next()) {
                recommendations.add(mapResultSetToRecommendation(rs));
            }
        } catch (SQLException e) {
            logger.error("Error finding recommendations by type: type={}, userId={}", recommendationType, userId, e);
        }
        return recommendations;
    }

    public boolean save(Recommendation recommendation) {
        if (recommendation.getId() == null) {
            return insert(recommendation);
        } else {
            return update(recommendation);
        }
    }

    private boolean insert(Recommendation recommendation) {
        String sql = "INSERT INTO recommendations (user_id, track_id, recommendation_type, score, is_viewed) " +
                "VALUES (?, ?, ?, ?, ?)";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

            stmt.setLong(1, recommendation.getUserId());
            stmt.setLong(2, recommendation.getTrackId());
            stmt.setString(3, recommendation.getRecommendationType());
            stmt.setDouble(4, recommendation.getScore());
            stmt.setBoolean(5, recommendation.getIsViewed());

            int affectedRows = stmt.executeUpdate();
            if (affectedRows > 0) {
                try (ResultSet generatedKeys = stmt.getGeneratedKeys()) {
                    if (generatedKeys.next()) {
                        recommendation.setId(generatedKeys.getLong(1));
                    }
                }
                return true;
            }
        } catch (SQLException e) {
            logger.error("Error inserting recommendation: userId={}, trackId={}",
                    recommendation.getUserId(), recommendation.getTrackId(), e);
        }
        return false;
    }

    private boolean update(Recommendation recommendation) {
        String sql = "UPDATE recommendations SET user_id = ?, track_id = ?, recommendation_type = ?, " +
                "score = ?, is_viewed = ? WHERE id = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setLong(1, recommendation.getUserId());
            stmt.setLong(2, recommendation.getTrackId());
            stmt.setString(3, recommendation.getRecommendationType());
            stmt.setDouble(4, recommendation.getScore());
            stmt.setBoolean(5, recommendation.getIsViewed());
            stmt.setLong(6, recommendation.getId());

            return stmt.executeUpdate() > 0;
        } catch (SQLException e) {
            logger.error("Error updating recommendation: {}", recommendation.getId(), e);
        }
        return false;
    }

    public boolean markAsViewed(Long recommendationId) {
        String sql = "UPDATE recommendations SET is_viewed = true WHERE id = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setLong(1, recommendationId);
            return stmt.executeUpdate() > 0;
        } catch (SQLException e) {
            logger.error("Error marking recommendation as viewed: {}", recommendationId, e);
        }
        return false;
    }

    public boolean markAllAsViewed(Long userId) {
        String sql = "UPDATE recommendations SET is_viewed = true WHERE user_id = ? AND is_viewed = false";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setLong(1, userId);
            return stmt.executeUpdate() > 0;
        } catch (SQLException e) {
            logger.error("Error marking all recommendations as viewed for user: {}", userId, e);
        }
        return false;
    }

    public boolean delete(Long id) {
        String sql = "DELETE FROM recommendations WHERE id = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setLong(1, id);
            return stmt.executeUpdate() > 0;
        } catch (SQLException e) {
            logger.error("Error deleting recommendation: {}", id, e);
        }
        return false;
    }

    public boolean deleteOldRecommendations(int daysOld) {
        String sql = "DELETE FROM recommendations WHERE created_at < CURRENT_TIMESTAMP - INTERVAL '? days'";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, daysOld);
            return stmt.executeUpdate() > 0;
        } catch (SQLException e) {
            logger.error("Error deleting old recommendations: {} days old", daysOld, e);
        }
        return false;
    }

    private Recommendation mapResultSetToRecommendation(ResultSet rs) throws SQLException {
        Recommendation recommendation = new Recommendation();
        recommendation.setId(rs.getLong("id"));
        recommendation.setUserId(rs.getLong("user_id"));
        recommendation.setTrackId(rs.getLong("track_id"));
        recommendation.setRecommendationType(rs.getString("recommendation_type"));
        recommendation.setScore(rs.getDouble("score"));
        recommendation.setCreatedAt(rs.getTimestamp("created_at").toLocalDateTime());
        recommendation.setIsViewed(rs.getBoolean("is_viewed"));

        // Display fields
        recommendation.setTrackTitle(rs.getString("track_title"));
        recommendation.setArtistName(rs.getString("artist_name"));
        recommendation.setAlbumTitle(rs.getString("album_title"));

        return recommendation;
    }
}