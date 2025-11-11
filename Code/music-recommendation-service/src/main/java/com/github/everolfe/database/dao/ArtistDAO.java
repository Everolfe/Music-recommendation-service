package com.github.everolfe.database.dao;

import com.github.everolfe.database.DatabaseConnection;
import com.github.everolfe.model.Artist;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class ArtistDAO {
    private static final Logger logger = LoggerFactory.getLogger(ArtistDAO.class);

    public Optional<Artist> findById(Long id) {
        String sql = "SELECT * FROM artists WHERE id = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setLong(1, id);
            ResultSet rs = stmt.executeQuery();

            if (rs.next()) {
                return Optional.of(mapResultSetToArtist(rs));
            }
        } catch (SQLException e) {
            logger.error("Error finding artist by id: {}", id, e);
        }
        return Optional.empty();
    }

    public Optional<Artist> findByName(String name) {
        String sql = "SELECT * FROM artists WHERE name = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, name);
            ResultSet rs = stmt.executeQuery();

            if (rs.next()) {
                return Optional.of(mapResultSetToArtist(rs));
            }
        } catch (SQLException e) {
            logger.error("Error finding artist by name: {}", name, e);
        }
        return Optional.empty();
    }

    public List<Artist> findAll() {
        List<Artist> artists = new ArrayList<>();
        String sql = "SELECT * FROM artists ORDER BY name";

        try (Connection conn = DatabaseConnection.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {

            while (rs.next()) {
                artists.add(mapResultSetToArtist(rs));
            }
        } catch (SQLException e) {
            logger.error("Error finding all artists", e);
        }
        return artists;
    }

    public List<Artist> findByNameContaining(String name) {
        List<Artist> artists = new ArrayList<>();
        String sql = "SELECT * FROM artists WHERE name ILIKE ? ORDER BY name";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, "%" + name + "%");
            ResultSet rs = stmt.executeQuery();

            while (rs.next()) {
                artists.add(mapResultSetToArtist(rs));
            }
        } catch (SQLException e) {
            logger.error("Error finding artists by name: {}", name, e);
        }
        return artists;
    }

    public boolean save(Artist artist) {
        if (artist.getId() == null) {
            return insert(artist);
        } else {
            return update(artist);
        }
    }

    private boolean insert(Artist artist) {
        String sql = "INSERT INTO artists (name, last_fm_id, bio, image_url) VALUES (?, ?, ?, ?)";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

            stmt.setString(1, artist.getName());
            stmt.setString(2, artist.getLastFmId());
            stmt.setString(3, artist.getBio());
            stmt.setString(4, artist.getImageUrl());

            int affectedRows = stmt.executeUpdate();
            if (affectedRows > 0) {
                try (ResultSet generatedKeys = stmt.getGeneratedKeys()) {
                    if (generatedKeys.next()) {
                        artist.setId(generatedKeys.getLong(1));
                    }
                }
                return true;
            }
        } catch (SQLException e) {
            logger.error("Error inserting artist: {}", artist.getName(), e);
        }
        return false;
    }

    private boolean update(Artist artist) {
        String sql = "UPDATE artists SET name = ?, last_fm_id = ?, bio = ?, image_url = ? WHERE id = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, artist.getName());
            stmt.setString(2, artist.getLastFmId());
            stmt.setString(3, artist.getBio());
            stmt.setString(4, artist.getImageUrl());
            stmt.setLong(5, artist.getId());

            return stmt.executeUpdate() > 0;
        } catch (SQLException e) {
            logger.error("Error updating artist: {}", artist.getId(), e);
        }
        return false;
    }

    public boolean delete(Long id) {
        String sql = "DELETE FROM artists WHERE id = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setLong(1, id);
            return stmt.executeUpdate() > 0;
        } catch (SQLException e) {
            logger.error("Error deleting artist: {}", id, e);
        }
        return false;
    }

    private Artist mapResultSetToArtist(ResultSet rs) throws SQLException {
        Artist artist = new Artist();
        artist.setId(rs.getLong("id"));
        artist.setName(rs.getString("name"));
        artist.setLastFmId(rs.getString("last_fm_id"));
        artist.setBio(rs.getString("bio"));
        artist.setImageUrl(rs.getString("image_url"));
        artist.setCreatedAt(rs.getTimestamp("created_at").toLocalDateTime());
        return artist;
    }
}