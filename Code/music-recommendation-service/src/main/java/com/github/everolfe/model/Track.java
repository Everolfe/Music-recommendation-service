package com.github.everolfe.model;

import java.time.LocalDateTime;

public class Track {
    private Long id;
    private String title;
    private Long artistId;
    private Long albumId;
    private Integer duration;
    private Integer trackNumber;
    private String lastFmId;
    private LocalDateTime createdAt;
    private String source;
    // Аудио характеристики
    private Double acousticness;
    private Double danceability;
    private Double energy;
    private Double instrumentalness;
    private Double liveness;
    private Double loudness;
    private Double speechiness;
    private Double tempo;
    private Double valence;
    private String coverUrl;

    private Integer rating;
    private Boolean favorite;
    private Integer playCount;

    // Для отображения (не хранятся в БД)
    private String artistName;
    private String albumTitle;
    private String genre;

    public Track() {}

    public Track(String title, String artistName, String albumTitle, Integer duration, String genre) {
        this.title = title;
        this.artistName = artistName;
        this.albumTitle = albumTitle;
        this.duration = duration;
        this.genre = genre;
        this.createdAt = LocalDateTime.now();
        this.source = "API-Imported";
    }

    public Track(String title, Long artistId, Long albumId, Integer duration, Integer trackNumber) {
        this.title = title;
        this.artistId = artistId;
        this.albumId = albumId;
        this.duration = duration;
        this.trackNumber = trackNumber;
        this.createdAt = LocalDateTime.now();
    }

    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public Long getArtistId() { return artistId; }
    public void setArtistId(Long artistId) { this.artistId = artistId; }

    public Long getAlbumId() { return albumId; }
    public void setAlbumId(Long albumId) { this.albumId = albumId; }

    public Integer getDuration() { return duration; }
    public void setDuration(Integer duration) { this.duration = duration; }

    public Integer getTrackNumber() { return trackNumber; }
    public void setTrackNumber(Integer trackNumber) { this.trackNumber = trackNumber; }

    public String getLastFmId() { return lastFmId; }
    public void setLastFmId(String lastFmId) { this.lastFmId = lastFmId; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    // Audio features
    public Double getAcousticness() { return acousticness; }
    public void setAcousticness(Double acousticness) { this.acousticness = acousticness; }

    public Double getDanceability() { return danceability; }
    public void setDanceability(Double danceability) { this.danceability = danceability; }

    public Double getEnergy() { return energy; }
    public void setEnergy(Double energy) { this.energy = energy; }

    public Double getInstrumentalness() { return instrumentalness; }
    public void setInstrumentalness(Double instrumentalness) { this.instrumentalness = instrumentalness; }

    public Double getLiveness() { return liveness; }
    public void setLiveness(Double liveness) { this.liveness = liveness; }

    public Double getLoudness() { return loudness; }
    public void setLoudness(Double loudness) { this.loudness = loudness; }

    public Double getSpeechiness() { return speechiness; }
    public void setSpeechiness(Double speechiness) { this.speechiness = speechiness; }

    public Double getTempo() { return tempo; }
    public void setTempo(Double tempo) { this.tempo = tempo; }

    public Double getValence() { return valence; }
    public void setValence(Double valence) { this.valence = valence; }

    // Display fields
    public String getArtistName() { return artistName; }
    public void setArtistName(String artistName) { this.artistName = artistName; }

    public String getAlbumTitle() { return albumTitle; }
    public void setAlbumTitle(String albumTitle) { this.albumTitle = albumTitle; }

    public String getGenre() { return genre; }
    public void setGenre(String genre) { this.genre = genre; }

    public String getCoverUrl() { return coverUrl; }
    public void setCoverUrl(String coverUrl) { this.coverUrl = coverUrl; }
    public String getSource() {
        return source;
    }

    public Integer getRating() {
        return rating;
    }

    public void setRating(Integer rating) {
        this.rating = rating;
    }

    public Boolean getFavorite() {
        return favorite;
    }

    public void setFavorite(Boolean favorite) {
        this.favorite = favorite;
    }

    public Integer getPlayCount() {
        return playCount;
    }

    public void setPlayCount(Integer playCount) {
        this.playCount = playCount;
    }

    public void setSource(String source) {
        this.source = source;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        Track track = (Track) o;
        return title.equalsIgnoreCase(track.title) &&
                artistName.equalsIgnoreCase(track.artistName);
    }

    @Override
    public int hashCode() {
        return (title.toLowerCase() + "|" + artistName.toLowerCase()).hashCode();
    }

    @Override
    public String toString() {
        return String.format("%s - %s [%s]", artistName, title, source != null ? source : "DB");
    }
}