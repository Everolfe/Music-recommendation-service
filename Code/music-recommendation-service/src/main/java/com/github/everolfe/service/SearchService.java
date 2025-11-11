package com.github.everolfe.service;

import com.github.everolfe.api.LastFmService;
import com.github.everolfe.model.SearchCriteria;
import com.github.everolfe.model.Track;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class SearchService {
    private static final Logger logger = LoggerFactory.getLogger(SearchService.class);

    private final LastFmService lastFmService;
    private final TrackService trackService;

    public SearchService() {
        this.lastFmService = new LastFmService();
        this.trackService = new TrackService();
    }

    /**
     * Поиск треков с фильтрацией по артисту, альбому или названию
     */
    public List<Track> searchTracks(SearchCriteria criteria) {
        logger.info("Searching tracks with criteria: {}", criteria);

        List<Track> allTracks = new ArrayList<>();

        try {
            // Поиск в Last.fm API
            if (criteria.getQuery() != null && !criteria.getQuery().trim().isEmpty()) {
                List<LastFmService.TrackInfo> apiResults = lastFmService.searchTracks(criteria.getQuery());
                allTracks.addAll(convertToTracks(apiResults));
            }

            // Поиск в локальной базе
            List<Track> localTracks = trackService.searchTracks(criteria.getQuery());
            allTracks.addAll(localTracks);

            // Применяем фильтры
            List<Track> filteredTracks = applyTrackFilters(allTracks, criteria);

            // Убираем дубликаты
            List<Track> uniqueTracks = removeDuplicates(filteredTracks);

            logger.info("Found {} unique tracks after filtering", uniqueTracks.size());
            return uniqueTracks;

        } catch (Exception e) {
            logger.error("Error during track search", e);
            return new ArrayList<>();
        }
    }

    /**
     * Применяем фильтры к трекам
     */
    private List<Track> applyTrackFilters(List<Track> tracks, SearchCriteria criteria) {
        return tracks.stream()
                .filter(track -> {
                    // Фильтр по артисту
                    if (criteria.getArtist() != null && !criteria.getArtist().isEmpty()) {
                        if (track.getArtistName() == null ||
                                !track.getArtistName().toLowerCase().contains(criteria.getArtist().toLowerCase())) {
                            return false;
                        }
                    }

                    // Фильтр по альбому
                    if (criteria.getAlbum() != null && !criteria.getAlbum().isEmpty()) {
                        if (track.getAlbumTitle() == null ||
                                !track.getAlbumTitle().toLowerCase().contains(criteria.getAlbum().toLowerCase())) {
                            return false;
                        }
                    }

                    // Фильтр по жанру
                    if (criteria.getGenre() != null && !criteria.getGenre().isEmpty()) {
                        if (track.getGenre() == null ||
                                !track.getGenre().toLowerCase().contains(criteria.getGenre().toLowerCase())) {
                            return false;
                        }
                    }

                    return true;
                })
                .collect(Collectors.toList());
    }

    /**
     * Удаляем дубликаты треков (по названию и исполнителю)
     */
    private List<Track> removeDuplicates(List<Track> tracks) {
        return tracks.stream()
                .collect(Collectors.collectingAndThen(
                        Collectors.toMap(
                                track -> (track.getTitle() + "|" + track.getArtistName()).toLowerCase(),
                                track -> track,
                                (existing, replacement) -> existing // При дубликатах берем существующий
                        ),
                        map -> new ArrayList<>(map.values())
                ));
    }

    /**
     * Быстрый поиск треков по запросу (без фильтров)
     */
    public List<Track> quickSearch(String query) {
        SearchCriteria criteria = new SearchCriteria();
        criteria.setQuery(query);
        return searchTracks(criteria);
    }

    /**
     * Поиск треков конкретного артиста
     */
    public List<Track> searchByArtist(String artist) {
        SearchCriteria criteria = new SearchCriteria();
        criteria.setArtist(artist);
        return searchTracks(criteria);
    }

    /**
     * Поиск треков из конкретного альбома
     */
    public List<Track> searchByAlbum(String album) {
        SearchCriteria criteria = new SearchCriteria();
        criteria.setAlbum(album);
        return searchTracks(criteria);
    }

    /**
     * Поиск треков по жанру
     */
    public List<Track> searchByGenre(String genre) {
        SearchCriteria criteria = new SearchCriteria();
        criteria.setGenre(genre);
        return searchTracks(criteria);
    }

    private List<Track> convertToTracks(List<LastFmService.TrackInfo> trackInfos) {
        return trackInfos.stream()
                .map(this::convertToTrack)
                .collect(Collectors.toList());
    }

    private Track convertToTrack(LastFmService.TrackInfo trackInfo) {
        Track track = new Track();
        track.setTitle(trackInfo.getName());
        track.setArtistName(trackInfo.getArtist());
        track.setAlbumTitle(trackInfo.getAlbum());
        track.setDuration(trackInfo.getDuration());
        track.setGenre(trackInfo.getGenres() != null ?
                String.join(", ", trackInfo.getGenres()) : "Unknown");
        track.setPlayCount(trackInfo.getPlayCount());
        track.setSource("LastFM-Search");
        return track;
    }
}