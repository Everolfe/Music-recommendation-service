package com.github.everolfe.database.dao;

import com.github.everolfe.database.DatabaseConnection;
import com.github.everolfe.model.TrackGenre;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class TrackGenreDAO {
    private static final Logger logger = LoggerFactory.getLogger(TrackGenreDAO.class);

    public List<TrackGenre> findByTrackId(Long trackId) {
        List<TrackGenre> trackGenres = new ArrayList<>();
        String sql = "SELECT tg.*, t.title as track_title, g.name as genre_name " +
                "FROM track_genres tg " +
                "JOIN tracks t ON tg.track_id = t.id " +
                "JOIN genres g ON tg.genre_id = g.id " +
                "WHERE tg.track_id = ?";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setLong(1, trackId);
            ResultSet rs = stmt.executeQuery();

            while (rs.next()) {
                trackGenres.add(mapResultSetToTrackGenre(rs));
            }
        } catch (SQLException e) {
            logger.error("Error finding track genres by track id: {}", trackId, e);
        }
        return trackGenres;
    }

    public List<TrackGenre> findByGenreId(Long genreId) {
        List<TrackGenre> trackGenres = new ArrayList<>();
        String sql = "SELECT tg.*, t.title as track_title, g.name as genre_name " +
                "FROM track_genres tg " +
                "JOIN tracks t ON tg.track_id = t.id " +
                "JOIN genres g ON tg.genre_id = g.id " +
                "WHERE tg.genre_id = ?";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setLong(1, genreId);
            ResultSet rs = stmt.executeQuery();

            while (rs.next()) {
                trackGenres.add(mapResultSetToTrackGenre(rs));
            }
        } catch (SQLException e) {
            logger.error("Error finding track genres by genre id: {}", genreId, e);
        }
        return trackGenres;
    }

    public boolean addGenreToTrack(Long trackId, Long genreId) {
        String sql = "INSERT INTO track_genres (track_id, genre_id) VALUES (?, ?) " +
                "ON CONFLICT (track_id, genre_id) DO NOTHING";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setLong(1, trackId);
            stmt.setLong(2, genreId);

            return stmt.executeUpdate() > 0;
        } catch (SQLException e) {
            logger.error("Error adding genre to track: trackId={}, genreId={}", trackId, genreId, e);
        }
        return false;
    }

    public boolean removeGenreFromTrack(Long trackId, Long genreId) {
        String sql = "DELETE FROM track_genres WHERE track_id = ? AND genre_id = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setLong(1, trackId);
            stmt.setLong(2, genreId);

            return stmt.executeUpdate() > 0;
        } catch (SQLException e) {
            logger.error("Error removing genre from track: trackId={}, genreId={}", trackId, genreId, e);
        }
        return false;
    }

    public boolean removeAllGenresFromTrack(Long trackId) {
        String sql = "DELETE FROM track_genres WHERE track_id = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setLong(1, trackId);
            return stmt.executeUpdate() > 0;
        } catch (SQLException e) {
            logger.error("Error removing all genres from track: {}", trackId, e);
        }
        return false;
    }

    private TrackGenre mapResultSetToTrackGenre(ResultSet rs) throws SQLException {
        TrackGenre trackGenre = new TrackGenre();
        trackGenre.setTrackId(rs.getLong("track_id"));
        trackGenre.setGenreId(rs.getLong("genre_id"));
        trackGenre.setTrackTitle(rs.getString("track_title"));
        trackGenre.setGenreName(rs.getString("genre_name"));
        return trackGenre;
    }
}