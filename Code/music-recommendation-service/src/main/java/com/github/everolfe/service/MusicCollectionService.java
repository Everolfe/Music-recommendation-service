package com.github.everolfe.service;

import com.github.everolfe.database.dao.UserPreferenceDAO;
import com.github.everolfe.model.Track;
import com.github.everolfe.model.UserPreference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

public class MusicCollectionService {
    private static final Logger logger = LoggerFactory.getLogger(MusicCollectionService.class);
    private final UserPreferenceDAO userPreferenceDAO;
    private final TrackService trackService;

    public MusicCollectionService() {
        this.userPreferenceDAO = new UserPreferenceDAO();
        this.trackService = new TrackService();
    }

    // Основной метод добавления трека в коллекцию
    public AddTrackResult addTrackToCollection(Long userId, Track track, Integer rating) {
        try {
            Long trackId = track.getId();

            // 🔥 ИСПРАВЛЕННОЕ УСЛОВИЕ: сначала проверяем на null, потом на значение
            if (trackId == null || trackId <= 0) {
                Track existingTrack = findExistingTrack(track);
                if (existingTrack != null) {
                    trackId = existingTrack.getId();
                    logger.info("Found existing track in database: {} - {} (ID: {})",
                            track.getArtistName(), track.getTitle(), trackId);
                } else {
                    Track savedTrack = saveApiTrackToDatabase(track);
                    if (savedTrack != null) {
                        trackId = savedTrack.getId();
                        logger.info("API track saved to database with ID: {}", trackId);
                    } else {
                        logger.error("Failed to save API track to database: {}", track.getTitle());
                        return new AddTrackResult(false, "Ошибка сохранения трека в базу данных");
                    }
                }
            }

            // Проверяем, не добавлен ли уже трек в коллекцию пользователя
            if (isTrackInCollection(userId, trackId)) {
                logger.info("Track already in user collection: userId={}, trackId={}", userId, trackId);
                return new AddTrackResult(false, "Трек уже добавлен в коллекцию");
            }

            // 🔥 ДОБАВЛЯЕМ с указанным рейтингом вместо значения по умолчанию
            UserPreference preference = new UserPreference(userId, trackId, rating);
            preference.setIsFavorite(false);
            boolean success = userPreferenceDAO.save(preference);

            if (success) {
                logger.info("Track added to collection with rating: userId={}, trackId={}, title={}, rating={}",
                        userId, trackId, track.getTitle(), rating);
                return new AddTrackResult(true, "Трек успешно добавлен в коллекцию с оценкой " + rating);
            } else {
                return new AddTrackResult(false, "Ошибка при добавлении трека в коллекцию");
            }
        } catch (Exception e) {
            logger.error("Error adding track to collection: userId={}, trackId={}", userId, track.getId(), e);
            return new AddTrackResult(false, "Произошла ошибка при добавлении трека");
        }
    }

    private void triggerRecommendationUpdate(Long userId) {
        new Thread(() -> {
            try {
                // Небольшая задержка чтобы гарантировать сохранение трека
                Thread.sleep(500);

                RecommendationService recService = new RecommendationService();
                recService.generateNewRecommendations(userId);

                logger.info("Recommendations updated after adding new track for user: {}", userId);
            } catch (Exception e) {
                logger.error("Error triggering recommendation update for user: {}", userId, e);
            }
        }).start();
    }

    private Track findExistingTrack(Track apiTrack) {
        try {
            // Простой поиск по имени артиста и названию трека
            List<Track> artistTracks = trackService.getTracksByArtist(apiTrack.getArtistName());
            return artistTracks.stream()
                    .filter(track -> track.getTitle().equalsIgnoreCase(apiTrack.getTitle()))
                    .findFirst()
                    .orElse(null);
        } catch (Exception e) {
            logger.error("Error finding existing track: {} - {}", apiTrack.getArtistName(), apiTrack.getTitle(), e);
            return null;
        }
    }



    // Сохраняет трек из API в базу данных
    private Track saveApiTrackToDatabase(Track apiTrack) {
        try {
            // Создаем новый трек с информацией об артисте и альбоме
            Track trackToSave = new Track();
            trackToSave.setTitle(apiTrack.getTitle());
            trackToSave.setArtistName(apiTrack.getArtistName());
            trackToSave.setAlbumTitle(apiTrack.getAlbumTitle());
            trackToSave.setDuration(apiTrack.getDuration());
            trackToSave.setGenre(apiTrack.getGenre());
            trackToSave.setSource("API-Imported");

            // Сохраняем трек в базу данных (создаст артиста и альбом если нужно)
            boolean saved = trackService.addTrack(trackToSave);
            if (saved && trackToSave.getId() != null && trackToSave.getId() > 0) {
                logger.info("API track saved to database: {} - {} (ID: {})",
                        trackToSave.getArtistName(), trackToSave.getTitle(), trackToSave.getId());
                return trackToSave;
            } else {
                logger.error("Failed to save API track to database: {} - {}",
                        apiTrack.getArtistName(), apiTrack.getTitle());
                return null;
            }
        } catch (Exception e) {
            logger.error("Error saving API track to database: {} - {}",
                    apiTrack.getArtistName(), apiTrack.getTitle(), e);
            return null;
        }
    }

    private String generateLastFmId(String artist, String title) {
        return (artist + "_" + title)
                .toLowerCase()
                .replaceAll("[^a-z0-9]", "_")
                .replaceAll("_+", "_");
    }

    // Альтернативный метод с указанием рейтинга
    public boolean addToCollection(Long userId, Long trackId, Integer rating) {
        try {
            // Проверяем, что trackId положительный (существует в базе)
            if (trackId <= 0) {
                logger.error("Cannot add track with invalid ID to collection: {}", trackId);
                return false;
            }

            Optional<UserPreference> existing = userPreferenceDAO.findByUserAndTrack(userId, trackId);

            if (existing.isPresent()) {
                // Обновляем рейтинг
                UserPreference preference = existing.get();
                preference.setRating(rating);
                return userPreferenceDAO.save(preference);
            } else {
                // Добавляем новый трек в коллекцию
                UserPreference preference = new UserPreference(userId, trackId, rating);
                return userPreferenceDAO.save(preference);
            }
        } catch (Exception e) {
            logger.error("Error adding track to collection: userId={}, trackId={}", userId, trackId, e);
            return false;
        }
    }

    public boolean removeFromCollection(Long userId, Long trackId) {
        try {
            Optional<UserPreference> preference = userPreferenceDAO.findByUserAndTrack(userId, trackId);
            if (preference.isPresent()) {
                boolean success = userPreferenceDAO.delete(preference.get().getId());
                if (success) {
                    logger.info("Track removed from collection: userId={}, trackId={}", userId, trackId);
                }
                return success;
            }
            return false;
        } catch (Exception e) {
            logger.error("Error removing track from collection: userId={}, trackId={}", userId, trackId, e);
            return false;
        }
    }



    public boolean toggleFavorite(Long userId, Long trackId) {
        try {
            Optional<UserPreference> preference = userPreferenceDAO.findByUserAndTrack(userId, trackId);

            if (preference.isPresent()) {
                UserPreference userPreference = preference.get();
                userPreference.setIsFavorite(!userPreference.getIsFavorite());
                boolean success = userPreferenceDAO.save(userPreference);
                if (success) {
                    logger.info("Favorite toggled: userId={}, trackId={}, isFavorite={}",
                            userId, trackId, userPreference.getIsFavorite());
                }
                return success;
            } else {
                logger.warn("Cannot toggle favorite - track not in collection: userId={}, trackId={}", userId, trackId);
                return false;
            }
        } catch (Exception e) {
            logger.error("Error toggling favorite: userId={}, trackId={}", userId, trackId, e);
            return false;
        }
    }

    // Получаем коллекцию пользователя с полной информацией о треках
    public List<Track> getUserCollection(Long userId) {
        try {
            List<UserPreference> preferences = userPreferenceDAO.findByUserId(userId);
            logger.info("Found {} user preferences for user: {}", preferences.size(), userId);

            List<Track> userTracks = preferences.stream()
                    .map(preference -> {
                        // Получаем полную информацию о треке
                        Track track = trackService.getTrackById(preference.getTrackId());
                        if (track != null) {
                            // Добавляем информацию из UserPreference
                            track.setRating(preference.getRating());
                            track.setFavorite(preference.getIsFavorite());
                            track.setSource("Collection");
                            logger.debug("Loaded track from collection: {} - {}",
                                    track.getArtistName(), track.getTitle());
                        } else {
                            logger.warn("Track not found for preference: trackId={}", preference.getTrackId());
                        }
                        return track;
                    })
                    .filter(track -> track != null)
                    .collect(Collectors.toList());

            logger.info("Successfully loaded {} tracks for user collection", userTracks.size());
            return userTracks;
        } catch (Exception e) {
            logger.error("Error getting user collection: userId={}", userId, e);
            return List.of();
        }
    }

    public List<UserPreference> getUserPreferences(Long userId) {
        return userPreferenceDAO.findByUserId(userId);
    }

    public List<UserPreference> getUserFavorites(Long userId) {
        return userPreferenceDAO.findFavoritesByUserId(userId);
    }

    public List<UserPreference> getUserHighRatedTracks(Long userId, int minRating) {
        return userPreferenceDAO.findHighRatedByUserId(userId, minRating);
    }

    public Optional<Integer> getUserRating(Long userId, Long trackId) {
        Optional<UserPreference> preference = userPreferenceDAO.findByUserAndTrack(userId, trackId);
        return preference.map(UserPreference::getRating);
    }

    public boolean isTrackInCollection(Long userId, Long trackId) {
        return userPreferenceDAO.findByUserAndTrack(userId, trackId).isPresent();
    }

    public boolean isTrackFavorite(Long userId, Long trackId) {
        Optional<UserPreference> preference = userPreferenceDAO.findByUserAndTrack(userId, trackId);
        return preference.map(UserPreference::getIsFavorite).orElse(false);
    }

    // Обновление рейтинга трека
    public boolean updateRating(Long userId, Long trackId, Integer rating) {
        try {
            Optional<UserPreference> preference = userPreferenceDAO.findByUserAndTrack(userId, trackId);

            if (preference.isPresent()) {
                UserPreference userPreference = preference.get();
                userPreference.setRating(rating);
                boolean success = userPreferenceDAO.save(userPreference);
                if (success) {
                    logger.info("Rating updated: userId={}, trackId={}, rating={}", userId, trackId, rating);
                }
                return success;
            } else {
                logger.warn("Cannot update rating - track not in collection: userId={}, trackId={}", userId, trackId);
                return false;
            }
        } catch (Exception e) {
            logger.error("Error updating rating: userId={}, trackId={}, rating={}", userId, trackId, rating, e);
            return false;
        }
    }


    // Получение статистики коллекции
    public CollectionStats getCollectionStats(Long userId) {
        List<UserPreference> preferences = userPreferenceDAO.findByUserId(userId);

        int totalTracks = preferences.size();
        int favoriteTracks = (int) preferences.stream().filter(UserPreference::getIsFavorite).count();
        double averageRating = preferences.stream()
                .mapToInt(UserPreference::getRating)
                .average()
                .orElse(0.0);

        return new CollectionStats(totalTracks, favoriteTracks, averageRating);
    }

    // Класс для статистики коллекции
    public static class CollectionStats {
        private final int totalTracks;
        private final int favoriteTracks;
        private final double averageRating;

        public CollectionStats(int totalTracks, int favoriteTracks, double averageRating) {
            this.totalTracks = totalTracks;
            this.favoriteTracks = favoriteTracks;
            this.averageRating = averageRating;
        }

        // Getters
        public int getTotalTracks() { return totalTracks; }
        public int getFavoriteTracks() { return favoriteTracks; }
        public double getAverageRating() { return averageRating; }
    }
    public static class AddTrackResult {
        private final boolean success;
        private final String message;

        public AddTrackResult(boolean success, String message) {
            this.success = success;
            this.message = message;
        }

        public boolean isSuccess() { return success; }
        public String getMessage() { return message; }
    }
}