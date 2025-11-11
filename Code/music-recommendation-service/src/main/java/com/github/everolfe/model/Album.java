package com.github.everolfe.model;

import java.time.LocalDateTime;

public class Album {
    private Long id;
    private String title;
    private Long artistId;
    private Integer releaseYear;
    private String lastFmId;
    private String genre;
    private String coverUrl;
    private LocalDateTime createdAt;

    public Album() {}

    public Album(String title, Long artistId) {
        this.title = title;
        this.artistId = artistId;
    }

    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public Long getArtistId() { return artistId; }
    public void setArtistId(Long artistId) { this.artistId = artistId; }

    public Integer getReleaseYear() { return releaseYear; }
    public void setReleaseYear(Integer releaseYear) { this.releaseYear = releaseYear; }

    public String getGenre() { return genre; }
    public void setGenre(String genre) { this.genre = genre; }

    public String getLastFmId() { return lastFmId; }
    public void setLastFmId(String lastFmId) { this.lastFmId = lastFmId; }

    public String getCoverUrl() { return coverUrl; }
    public void setCoverUrl(String coverUrl) { this.coverUrl = coverUrl; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

}