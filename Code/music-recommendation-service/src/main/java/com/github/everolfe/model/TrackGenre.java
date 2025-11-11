package com.github.everolfe.model;

public class TrackGenre {
    private Long trackId;
    private Long genreId;

    // Для отображения
    private String trackTitle;
    private String genreName;

    public TrackGenre() {}

    public TrackGenre(Long trackId, Long genreId) {
        this.trackId = trackId;
        this.genreId = genreId;
    }

    // Getters and Setters
    public Long getTrackId() { return trackId; }
    public void setTrackId(Long trackId) { this.trackId = trackId; }

    public Long getGenreId() { return genreId; }
    public void setGenreId(Long genreId) { this.genreId = genreId; }

    public String getTrackTitle() { return trackTitle; }
    public void setTrackTitle(String trackTitle) { this.trackTitle = trackTitle; }

    public String getGenreName() { return genreName; }
    public void setGenreName(String genreName) { this.genreName = genreName; }

    @Override
    public String toString() {
        return trackTitle + " - " + genreName;
    }
}