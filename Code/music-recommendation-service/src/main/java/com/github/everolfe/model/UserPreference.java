package com.github.everolfe.model;

import java.time.LocalDateTime;

public class UserPreference {
    private Long id;
    private Long userId;
    private Long trackId;
    private Integer rating;
    private Integer listenedCount;
    private LocalDateTime lastListened;
    private Boolean isFavorite;
    private LocalDateTime createdAt;

    // Для отображения
    private String trackTitle;
    private String artistName;

    public UserPreference() {}

    public UserPreference(Long userId, Long trackId, Integer rating) {
        this.userId = userId;
        this.trackId = trackId;
        this.rating = rating;
        this.listenedCount = 1;
        this.lastListened = LocalDateTime.now();
        this.isFavorite = false;
        this.createdAt = LocalDateTime.now();
    }

    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }

    public Long getTrackId() { return trackId; }
    public void setTrackId(Long trackId) { this.trackId = trackId; }

    public Integer getRating() { return rating; }
    public void setRating(Integer rating) { this.rating = rating; }

    public Integer getListenedCount() { return listenedCount; }
    public void setListenedCount(Integer listenedCount) { this.listenedCount = listenedCount; }

    public LocalDateTime getLastListened() { return lastListened; }
    public void setLastListened(LocalDateTime lastListened) { this.lastListened = lastListened; }

    public Boolean getIsFavorite() { return isFavorite; }
    public void setIsFavorite(Boolean isFavorite) { this.isFavorite = isFavorite; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    // Display fields
    public String getTrackTitle() { return trackTitle; }
    public void setTrackTitle(String trackTitle) { this.trackTitle = trackTitle; }

    public String getArtistName() { return artistName; }
    public void setArtistName(String artistName) { this.artistName = artistName; }

    @Override
    public String toString() {
        return "Rating: " + rating + " - " + trackTitle;
    }
}