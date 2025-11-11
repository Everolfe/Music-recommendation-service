package com.github.everolfe.database.dao;

import com.github.everolfe.database.DatabaseConnection;
import com.github.everolfe.model.Album;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class AlbumDAO {
    private static final Logger logger = LoggerFactory.getLogger(AlbumDAO.class);

    public Optional<Album> findById(Long id) {
        String sql = "SELECT a.*, ar.name as artist_name FROM albums a " +
                "LEFT JOIN artists ar ON a.artist_id = ar.id " +
                "WHERE a.id = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setLong(1, id);
            ResultSet rs = stmt.executeQuery();

            if (rs.next()) {
                return Optional.of(mapResultSetToAlbum(rs));
            }
        } catch (SQLException e) {
            logger.error("Error finding album by id: {}", id, e);
        }
        return Optional.empty();
    }

    public List<Album> findByArtistId(Long artistId) {
        List<Album> albums = new ArrayList<>();
        String sql = "SELECT a.*, ar.name as artist_name FROM albums a " +
                "LEFT JOIN artists ar ON a.artist_id = ar.id " +
                "WHERE a.artist_id = ? ORDER BY a.release_year DESC";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setLong(1, artistId);
            ResultSet rs = stmt.executeQuery();

            while (rs.next()) {
                albums.add(mapResultSetToAlbum(rs));
            }
        } catch (SQLException e) {
            logger.error("Error finding albums by artist id: {}", artistId, e);
        }
        return albums;
    }

    public List<Album> findByTitle(String title) {
        List<Album> albums = new ArrayList<>();
        String sql = "SELECT a.*, ar.name as artist_name FROM albums a " +
                "LEFT JOIN artists ar ON a.artist_id = ar.id " +
                "WHERE a.title ILIKE ? ORDER BY a.title";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, "%" + title + "%");
            ResultSet rs = stmt.executeQuery();

            while (rs.next()) {
                albums.add(mapResultSetToAlbum(rs));
            }
        } catch (SQLException e) {
            logger.error("Error finding albums by title: {}", title, e);
        }
        return albums;
    }

    public List<Album> findAll() {
        List<Album> albums = new ArrayList<>();
        String sql = "SELECT a.*, ar.name as artist_name FROM albums a " +
                "LEFT JOIN artists ar ON a.artist_id = ar.id " +
                "ORDER BY a.title";

        try (Connection conn = DatabaseConnection.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {

            while (rs.next()) {
                albums.add(mapResultSetToAlbum(rs));
            }
        } catch (SQLException e) {
            logger.error("Error finding all albums", e);
        }
        return albums;
    }

    public boolean save(Album album) {
        if (album.getId() == null) {
            return insert(album);
        } else {
            return update(album);
        }
    }

    private boolean insert(Album album) {
        String sql = "INSERT INTO albums (title, artist_id, release_year, last_fm_id, cover_url) VALUES (?, ?, ?, ?, ?)";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

            stmt.setString(1, album.getTitle());
            stmt.setLong(2, album.getArtistId());
            stmt.setInt(3, album.getReleaseYear());
            stmt.setString(4, album.getLastFmId());
            stmt.setString(5, album.getCoverUrl());

            int affectedRows = stmt.executeUpdate();
            if (affectedRows > 0) {
                try (ResultSet generatedKeys = stmt.getGeneratedKeys()) {
                    if (generatedKeys.next()) {
                        album.setId(generatedKeys.getLong(1));
                    }
                }
                return true;
            }
        } catch (SQLException e) {
            logger.error("Error inserting album: {}", album.getTitle(), e);
        }
        return false;
    }

    private boolean update(Album album) {
        String sql = "UPDATE albums SET title = ?, artist_id = ?, release_year = ?, last_fm_id = ?, cover_url = ? WHERE id = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, album.getTitle());
            stmt.setLong(2, album.getArtistId());
            stmt.setInt(3, album.getReleaseYear());
            stmt.setString(4, album.getLastFmId());
            stmt.setString(5, album.getCoverUrl());
            stmt.setLong(6, album.getId());

            return stmt.executeUpdate() > 0;
        } catch (SQLException e) {
            logger.error("Error updating album: {}", album.getId(), e);
        }
        return false;
    }

    public boolean delete(Long id) {
        String sql = "DELETE FROM albums WHERE id = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setLong(1, id);
            return stmt.executeUpdate() > 0;
        } catch (SQLException e) {
            logger.error("Error deleting album: {}", id, e);
        }
        return false;
    }

    private Album mapResultSetToAlbum(ResultSet rs) throws SQLException {
        Album album = new Album();
        album.setId(rs.getLong("id"));
        album.setTitle(rs.getString("title"));
        album.setArtistId(rs.getLong("artist_id"));
        album.setReleaseYear(rs.getInt("release_year"));
        album.setLastFmId(rs.getString("last_fm_id"));
        album.setCoverUrl(rs.getString("cover_url"));
        album.setCreatedAt(rs.getTimestamp("created_at").toLocalDateTime());
        return album;
    }
}