package com.github.everolfe.service;

import com.github.everolfe.api.LastFmService;
import com.github.everolfe.database.dao.TrackDAO;
import com.github.everolfe.model.Track;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

public class TrackService {
    private static final Logger logger = LoggerFactory.getLogger(TrackService.class);
    private final TrackDAO trackDAO;
    private final LastFmService lastFmService;

    public TrackService() {
        this.trackDAO = new TrackDAO();
        this.lastFmService = new LastFmService();
    }

    public List<Track> getAllTracks() {
        List<Track> allTracks = new ArrayList<>();

        try {
            // 1. Получаем треки из локальной базы данных
            List<Track> localTracks = trackDAO.findAll();
            allTracks.addAll(localTracks);
            logger.info("Loaded {} tracks from database", localTracks.size());

            // 2. Получаем популярные треки из API для демонстрации
            List<Track> apiTracks = getPopularTracksFromAPI();
            allTracks.addAll(apiTracks);
            logger.info("Loaded {} tracks from API", apiTracks.size());

        } catch (Exception e) {
            logger.error("Error getting all tracks", e);
            // В случае ошибки возвращаем хотя бы мок-данные
            allTracks.addAll(getFallbackTracks(""));
        }

        // Убираем дубликаты по названию и исполнителю
        return removeDuplicates(allTracks);
    }

    public List<Track> searchTracks(String query) {
        if (query == null || query.trim().isEmpty()) {
            return getAllTracks();
        }

        String searchTerm = query.trim();
        List<Track> allResults = new ArrayList<>();

        try {
            // 1. Сначала ищем в локальной базе данных
            List<Track> localResults = trackDAO.findByTitle(searchTerm);
            localResults.addAll(trackDAO.findByArtist(searchTerm));
            allResults.addAll(localResults);

            logger.info("Found {} local results for: {}", localResults.size(), searchTerm);

            // 2. Затем ищем через Last.fm API
            List<LastFmService.TrackInfo> apiResults = lastFmService.searchTracks(searchTerm);
            List<Track> apiTracks = convertApiTracksToModel(apiResults);
            allResults.addAll(apiTracks);

            logger.info("Found {} API results for: {}", apiTracks.size(), searchTerm);

            // 3. Если результатов мало, добавляем популярные треки
            if (allResults.size() < 5) {
                List<Track> popularTracks = getPopularTracksFromAPI();
                // Фильтруем популярные треки по запросу
                List<Track> filteredPopular = popularTracks.stream()
                        .filter(track -> track.getTitle().toLowerCase().contains(searchTerm.toLowerCase()) ||
                                track.getArtistName().toLowerCase().contains(searchTerm.toLowerCase()))
                        .collect(Collectors.toList());
                allResults.addAll(filteredPopular);
            }

        } catch (Exception e) {
            logger.error("Error during track search: {}", searchTerm, e);
            // В случае ошибки возвращаем хотя бы мок-данные
            allResults.addAll(getFallbackTracks(searchTerm));
        }

        // Убираем дубликаты
        return removeDuplicates(allResults);
    }

    private List<Track> getPopularTracksFromAPI() {
        try {
            // Используем метод получения глобальных топ треков
            List<LastFmService.TrackInfo> popularTracks = lastFmService.getGlobalTopTracks();

            if (popularTracks.isEmpty()) {
                // Если основной метод не сработал, пробуем альтернативный
                popularTracks = lastFmService.getPopularTracks();
            }
            return convertApiTracksToModel(popularTracks);
        } catch (Exception e) {
            logger.error("Error getting popular tracks from API", e);
            return getFallbackTracks("");
        }
    }

    private List<Track> convertApiTracksToModel(List<LastFmService.TrackInfo> apiTracks) {
        return apiTracks.stream()
                .map(apiTrack -> {
                    Track track = new Track();
                    track.setTitle(apiTrack.getName() != null ? apiTrack.getName() : "Unknown Track");
                    track.setArtistName(apiTrack.getArtist() != null ? apiTrack.getArtist() : "Unknown Artist");
                    track.setAlbumTitle(apiTrack.getAlbum() != null ? apiTrack.getAlbum() : "Unknown Album");
                    track.setDuration(apiTrack.getDuration() != null ? apiTrack.getDuration() : 0);

                    if (apiTrack.getGenres() != null && !apiTrack.getGenres().isEmpty()) {
                        track.setGenre(String.join(", ", apiTrack.getGenres()));
                    } else {
                        track.setGenre("Various");
                    }

                    // Помечаем, что трек из API (не из локальной БД)
                    track.setId(-1L); // Отрицательный ID для API треков
                    track.setSource("API"); // Добавляем источник

                    return track;
                })
                .collect(Collectors.toList());
    }

    private List<Track> removeDuplicates(List<Track> tracks) {
        Set<String> seen = new HashSet<>();
        return tracks.stream()
                .filter(track -> {
                    String key = (track.getTitle() + "|" + track.getArtistName()).toLowerCase();
                    return seen.add(key);
                })
                .collect(Collectors.toList());
    }

    private List<Track> getFallbackTracks(String query) {
        // Резервные данные на случай ошибок
        List<Track> fallbackTracks = new ArrayList<>();
        String[][] fallbackData = {
                {"Bohemian Rhapsody", "Queen", "A Night at the Opera", "354", "Rock"},
                {"Blinding Lights", "The Weeknd", "After Hours", "200", "Pop"},
                {"Shape of You", "Ed Sheeran", "÷", "233", "Pop"},
                {"Stairway to Heaven", "Led Zeppelin", "Led Zeppelin IV", "482", "Rock"},
                {"Bad Guy", "Billie Eilish", "When We All Fall Asleep", "194", "Pop"},
                {"Smells Like Teen Spirit", "Nirvana", "Nevermind", "301", "Grunge"},
                {"Hotel California", "Eagles", "Hotel California", "391", "Rock"},
                {"Billie Jean", "Michael Jackson", "Thriller", "294", "Pop"},
                {"Sweet Child O'Mine", "Guns N' Roses", "Appetite for Destruction", "356", "Rock"},
                {"Like a Rolling Stone", "Bob Dylan", "Highway 61 Revisited", "369", "Folk Rock"}
        };

        String lowerQuery = query.toLowerCase();

        for (String[] data : fallbackData) {
            if (query.isEmpty() ||
                    data[0].toLowerCase().contains(lowerQuery) ||
                    data[1].toLowerCase().contains(lowerQuery)) {

                Track track = new Track();
                track.setTitle(data[0]);
                track.setArtistName(data[1]);
                track.setAlbumTitle(data[2]);
                track.setDuration(Integer.parseInt(data[3]));
                track.setGenre(data[4]);
                track.setId(-1L);
                track.setSource("Fallback");
                fallbackTracks.add(track);
            }
        }
        return fallbackTracks;
    }

    // Остальные методы остаются без изменений...
    public Track getEnhancedTrackInfo(String artist, String title) {
        try {
            LastFmService.TrackInfo apiInfo = lastFmService.getTrackInfo(artist, title);

            if (apiInfo != null) {
                Track track = new Track();
                track.setTitle(apiInfo.getName());
                track.setArtistName(apiInfo.getArtist());
                track.setAlbumTitle(apiInfo.getAlbum());
                track.setDuration(apiInfo.getDuration());
                if (apiInfo.getGenres() != null && !apiInfo.getGenres().isEmpty()) {
                    track.setGenre(String.join(", ", apiInfo.getGenres()));
                }
                track.setSource("API");
                return track;
            }
        } catch (Exception e) {
            logger.error("Error getting enhanced track info for: {} - {}", artist, title, e);
        }

        return null;
    }

    public List<Track> getTracksByArtist(String artistName) {
        try {
            return trackDAO.findByArtist(artistName);
        } catch (Exception e) {
            logger.error("Error getting tracks by artist: {}", artistName, e);
            return new ArrayList<>();
        }
    }

    public List<Track> getTracksByAlbum(Long albumId) {
        try {
            return trackDAO.findByAlbum(albumId);
        } catch (Exception e) {
            logger.error("Error getting tracks by album: {}", albumId, e);
            return new ArrayList<>();
        }
    }

    public List<Track> getTracksByGenre(String genreName) {
        try {
            return trackDAO.findByGenre(genreName);
        } catch (Exception e) {
            logger.error("Error getting tracks by genre: {}", genreName, e);
            return new ArrayList<>();
        }
    }

    public boolean addTrack(Track track) {
        try {
            return trackDAO.save(track);
        } catch (Exception e) {
            logger.error("Error adding track: {}", track.getTitle(), e);
            return false;
        }
    }

    // Добавляем метод для поиска по artistName и title
    public Optional<Track> findByArtistAndTitle(String artistName, String title) {
        try {
            // Ищем в локальной БД по комбинации artistName и title
            List<Track> tracks = trackDAO.findByArtist(artistName);
            return tracks.stream()
                    .filter(track -> track.getTitle().equalsIgnoreCase(title))
                    .findFirst();
        } catch (Exception e) {
            logger.error("Error finding track by artist and title: {} - {}", artistName, title, e);
            return Optional.empty();
        }
    }

    public boolean updateTrack(Track track) {
        try {
            return trackDAO.save(track);
        } catch (Exception e) {
            logger.error("Error updating track: {}", track.getId(), e);
            return false;
        }
    }

    public Track getTrackById(Long trackId) {
        try {
            // Для треков из API (отрицательные ID) возвращаем null
            if (trackId <= 0) {
                logger.warn("Cannot get API track by ID: {}", trackId);
                return null;
            }

            // Ищем трек в локальной базе данных
            return trackDAO.findById(trackId).orElse(null);
        } catch (Exception e) {
            logger.error("Error getting track by id: {}", trackId, e);
            return null;
        }
    }

    public boolean deleteTrack(Long trackId) {
        try {
            return trackDAO.delete(trackId);
        } catch (Exception e) {
            logger.error("Error deleting track: {}", trackId, e);
            return false;
        }
    }
}