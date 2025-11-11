package com.github.everolfe.model;

import java.time.LocalDateTime;

public class Recommendation {
    private Long id;
    private Long userId;
    private Long trackId;
    private String recommendationType;
    private Double score;
    private LocalDateTime createdAt;
    private Boolean isViewed;

    // Для отображения
    private String trackTitle;
    private String artistName;
    private String albumTitle;

    public Recommendation() {}

    public Recommendation(Long userId, Long trackId, String recommendationType, Double score) {
        this.userId = userId;
        this.trackId = trackId;
        this.recommendationType = recommendationType;
        this.score = score;
        this.createdAt = LocalDateTime.now();
        this.isViewed = false;
    }

    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }

    public Long getTrackId() { return trackId; }
    public void setTrackId(Long trackId) { this.trackId = trackId; }

    public String getRecommendationType() { return recommendationType; }
    public void setRecommendationType(String recommendationType) { this.recommendationType = recommendationType; }

    public Double getScore() { return score; }
    public void setScore(Double score) { this.score = score; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public Boolean getIsViewed() { return isViewed; }
    public void setIsViewed(Boolean isViewed) { this.isViewed = isViewed; }

    // Display fields
    public String getTrackTitle() { return trackTitle; }
    public void setTrackTitle(String trackTitle) { this.trackTitle = trackTitle; }

    public String getArtistName() { return artistName; }
    public void setArtistName(String artistName) { this.artistName = artistName; }

    public String getAlbumTitle() { return albumTitle; }
    public void setAlbumTitle(String albumTitle) { this.albumTitle = albumTitle; }

    @Override
    public String toString() {
        return recommendationType + " recommendation: " + trackTitle + " (" + String.format("%.2f", score) + ")";
    }
}