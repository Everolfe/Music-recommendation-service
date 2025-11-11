package com.github.everolfe.model;

public class SearchCriteria {
    private String query;        // Общий поисковый запрос
    private String artist;       // Фильтр по артисту
    private String album;        // Фильтр по альбому
    private String genre;        // Фильтр по жанру


    public SearchCriteria() {}

    // Getters and Setters
    public String getQuery() { return query; }
    public void setQuery(String query) { this.query = query; }

    public String getArtist() { return artist; }
    public void setArtist(String artist) { this.artist = artist; }

    public String getAlbum() { return album; }
    public void setAlbum(String album) { this.album = album; }

    public String getGenre() { return genre; }
    public void setGenre(String genre) { this.genre = genre; }


    @Override
    public String toString() {
        return "SearchCriteria{" +
                "query='" + query + '\'' +
                ", artist='" + artist + '\'' +
                ", album='" + album + '\'' +
                ", genre='" + genre + '\'' +
                '}';
    }
}