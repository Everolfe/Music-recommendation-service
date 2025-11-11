package com.github.everolfe.model;

import java.time.LocalDateTime;

public class Artist {
    private Long id;
    private String name;
    private String lastFmId;
    private String bio;
    private String imageUrl;
    private LocalDateTime createdAt;

    public Artist() {}

    public Artist(String name) {
        this.name = name;
        this.createdAt = LocalDateTime.now();
    }

    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getLastFmId() { return lastFmId; }
    public void setLastFmId(String lastFmId) { this.lastFmId = lastFmId; }

    public String getBio() { return bio; }
    public void setBio(String bio) { this.bio = bio; }

    public String getImageUrl() { return imageUrl; }
    public void setImageUrl(String imageUrl) { this.imageUrl = imageUrl; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    @Override
    public String toString() {
        return name;
    }
}