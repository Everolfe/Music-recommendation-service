package com.github.everolfe.service;

import com.github.everolfe.api.LastFmService;
import com.github.everolfe.database.dao.RecommendationDAO;
import com.github.everolfe.database.dao.UserPreferenceDAO;
import com.github.everolfe.model.Recommendation;
import com.github.everolfe.model.Track;
import com.github.everolfe.model.UserPreference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.stream.Collectors;

public class RecommendationService {
    private static final Logger logger = LoggerFactory.getLogger(RecommendationService.class);
    private final RecommendationDAO recommendationDAO;
    private final UserPreferenceDAO userPreferenceDAO;
    private final TrackService trackService;
    private final LastFmService lastFmService;

    private static final int TOTAL_RECOMMENDATIONS_LIMIT = 25; // Было 15
    private static final int CONTENT_BASED_PER_TRACK = 5; // Было 3
    private static final int LASTFM_RECOMMENDATIONS_LIMIT = 8; // Было 5
    private static final int POPULAR_RECOMMENDATIONS_LIMIT = 7; // Было 5
    private static final int RECENT_BASED_LIMIT = 5; // Новый тип рекомендаций

    // Веса для алгоритма рекомендаций
    private static final double GENRE_WEIGHT = 0.4;
    private static final double ARTIST_WEIGHT = 0.3;
    private static final double DURATION_WEIGHT = 0.15;
    private static final double POPULARITY_WEIGHT = 0.15;

    public RecommendationService() {
        this.recommendationDAO = new RecommendationDAO();
        this.userPreferenceDAO = new UserPreferenceDAO();
        this.trackService = new TrackService();
        this.lastFmService = new LastFmService();
    }

    public List<Recommendation> getRecommendationsForUser(Long userId) {
        try {
            List<Recommendation> unviewed = generateEnhancedRecommendations(userId);

            if (unviewed.isEmpty()) {
                unviewed = generateEnhancedRecommendations(userId);
            } else {
                // 🔥 Фильтруем существующие рекомендации от невалидных треков
                unviewed = enrichRecommendationsWithTrackInfo(unviewed);
            }

            logger.info("Returning {} valid recommendations for user: {}", unviewed.size(), userId);
            return unviewed;

        } catch (Exception e) {
            logger.error("Error getting recommendations for user: {}", userId, e);
            return Collections.emptyList();
        }
    }

    private List<Recommendation> generateEnhancedRecommendations(Long userId) {
        List<Recommendation> recommendations = new ArrayList<>();

        // 1. Рекомендации на основе контента (похожие треки) - БОЛЬШЕ рекомендаций
        recommendations.addAll(generateContentBasedRecommendations(userId));

        // 2. Рекомендации через Last.fm API - БОЛЬШЕ рекомендаций
        recommendations.addAll(generateLastFmBasedRecommendations(userId));

        // 3. Популярные треки - БОЛЬШЕ рекомендаций
        recommendations.addAll(generatePopularRecommendations(userId));

        // 4. Рекомендации на основе новых добавлений
        recommendations.addAll(generateRecentBasedRecommendations(userId));

        // Заполняем информацию о треках
        recommendations = enrichRecommendationsWithTrackInfo(recommendations);

        // 🔥 УВЕЛИЧИВАЕМ общий лимит
        List<Recommendation> finalRecommendations = recommendations.stream()
                .sorted((r1, r2) -> Double.compare(r2.getScore(), r1.getScore()))
                .distinct()
                .limit(TOTAL_RECOMMENDATIONS_LIMIT) // Теперь 25 вместо 15
                .collect(Collectors.toList());

        logger.info("Generated {} total recommendations for user: {}", finalRecommendations.size(), userId);

        // Сохраняем рекомендации в БД
        for (Recommendation rec : finalRecommendations) {
            recommendationDAO.save(rec);
        }

        return finalRecommendations;
    }



    /**
     * Принудительно генерирует новые рекомендации (игнорируя существующие)
     */
    public List<Recommendation> generateNewRecommendations(Long userId) {
        try {
            logger.info("Generating new recommendations for user: {}", userId);

            // Генерируем совершенно новые рекомендации
            List<Recommendation> newRecommendations = generateEnhancedRecommendations(userId);

            // 🔥 УДАЛЯЕМ ДУБЛИКАТЫ используя Stream API
            List<Recommendation> uniqueRecommendations = newRecommendations.stream()
                    .collect(Collectors.toMap(
                            Recommendation::getTrackId, // Ключ - trackId
                            rec -> rec,                 // Значение - сама рекомендация
                            (rec1, rec2) -> {           // При конфликте выбираем с более высоким score
                                return rec1.getScore() >= rec2.getScore() ? rec1 : rec2;
                            },
                            LinkedHashMap::new          // Сохраняем порядок
                    ))
                    .values().stream()
                    .collect(Collectors.toList());

            logger.info("Generated {} new recommendations ({} unique) for user: {}",
                    newRecommendations.size(), uniqueRecommendations.size(), userId);

            return uniqueRecommendations;

        } catch (Exception e) {
            logger.error("Error generating new recommendations for user: {}", userId, e);
            return Collections.emptyList();
        }
    }

    /**
     * Удаляет непросмотренные рекомендации пользователя
     */


    /**
     * 🔥 НОВЫЙ МЕТОД: Заполняет информацию о треках для отображения
     */
    private List<Recommendation> enrichRecommendationsWithTrackInfo(List<Recommendation> recommendations) {
        List<Recommendation> validRecommendations = new ArrayList<>();

        for (Recommendation rec : recommendations) {
            Track track = trackService.getTrackById(rec.getTrackId());
            if (track != null && isValidTrack(track)) {
                // Заполняем информацию только для валидных треков
                rec.setTrackTitle(track.getTitle());
                rec.setArtistName(track.getArtistName());
                rec.setAlbumTitle(track.getAlbumTitle());
                validRecommendations.add(rec);
            } else {
                logger.debug("Filtered out invalid track from recommendations: trackId={}", rec.getTrackId());
            }
        }

        return validRecommendations;
    }

    /**
     * Проверяет, является ли трек валидным для рекомендаций
     */
    private boolean isValidTrack(Track track) {
        if (track == null) {
            return false;
        }

        // Проверяем наличие обязательных полей
        boolean hasValidTitle = track.getTitle() != null &&
                !track.getTitle().trim().isEmpty() &&
                !track.getTitle().equalsIgnoreCase("unknown") &&
                !track.getTitle().equalsIgnoreCase("неизвестный трек");

        boolean hasValidArtist = track.getArtistName() != null &&
                !track.getArtistName().trim().isEmpty() &&
                !track.getArtistName().equalsIgnoreCase("unknown") &&
                !track.getArtistName().equalsIgnoreCase("неизвестный исполнитель");

        // Трек валиден если есть название и исполнитель
        return hasValidTitle && hasValidArtist;
    }

    private List<Recommendation> generateContentBasedRecommendations(Long userId) {
        List<Recommendation> recommendations = new ArrayList<>();

        // Получаем высоко оцененные треки пользователя
        List<UserPreference> highRated = userPreferenceDAO.findHighRatedByUserId(userId, 3);

        if (highRated.isEmpty()) {
            return recommendations;
        }

        // Получаем все треки для поиска похожих
        List<Track> allTracks = trackService.getAllTracks().stream()
                .filter(this::isValidTrack)
                .collect(Collectors.toList());

        // 🔥 УВЕЛИЧИВАЕМ количество похожих треков для каждого исходного
        for (UserPreference preference : highRated) {
            Track sourceTrack = trackService.getTrackById(preference.getTrackId());
            if (sourceTrack == null || !isValidTrack(sourceTrack)) continue;

            List<Track> similarTracks = findSimilarTracksEnhanced(sourceTrack, allTracks, CONTENT_BASED_PER_TRACK);

            for (Track similarTrack : similarTracks) {
                if (!isTrackInUserCollection(userId, similarTrack.getId())) {
                    double similarityScore = calculateEnhancedSimilarityScore(sourceTrack, similarTrack);
                    Recommendation rec = new Recommendation(
                            userId,
                            similarTrack.getId(),
                            "content_based",
                            similarityScore * 0.9
                    );
                    recommendations.add(rec);
                }
            }

            // 🔥 Ограничиваем количество исходных треков для производительности
            if (recommendations.size() >= 15) break;
        }

        logger.debug("Generated {} content-based recommendations", recommendations.size());
        return recommendations;
    }

    private List<Recommendation> generateLastFmBasedRecommendations(Long userId) {
        List<Recommendation> recommendations = new ArrayList<>();

        // Получаем избранные треки пользователя
        List<UserPreference> favorites = userPreferenceDAO.findFavoritesByUserId(userId);

        // 🔥 УВЕЛИЧИВАЕМ количество треков для анализа
        int tracksToProcess = Math.min(favorites.size(), 8); // Обрабатываем до 8 избранных треков

        for (int i = 0; i < tracksToProcess; i++) {
            UserPreference favorite = favorites.get(i);
            Track favoriteTrack = trackService.getTrackById(favorite.getTrackId());
            if (favoriteTrack == null || !isValidTrack(favoriteTrack)) continue;

            // 🔥 УВЕЛИЧИВАЕМ количество похожих треков от Last.fm
            List<LastFmService.TrackInfo> similarTracks = lastFmService.getSimilarTracks(
                    favoriteTrack.getArtistName(),
                    favoriteTrack.getTitle(),
                    6 // Было 5
            );

            for (LastFmService.TrackInfo similarTrackInfo : similarTracks) {
                Track similarTrack = findOrCreateTrack(similarTrackInfo);
                if (similarTrack != null && !isTrackInUserCollection(userId, similarTrack.getId())) {
                    Recommendation rec = new Recommendation(
                            userId,
                            similarTrack.getId(),
                            "lastfm_similar",
                            0.85
                    );
                    recommendations.add(rec);
                }

                // 🔥 Ограничиваем общее количество Last.fm рекомендаций
                if (recommendations.size() >= LASTFM_RECOMMENDATIONS_LIMIT) break;
            }

            if (recommendations.size() >= LASTFM_RECOMMENDATIONS_LIMIT) break;
        }

        logger.debug("Generated {} Last.fm based recommendations", recommendations.size());
        return recommendations;
    }

    private List<Recommendation> generatePopularRecommendations(Long userId) {
        List<Recommendation> recommendations = new ArrayList<>();

        // Используем Last.fm для получения популярных треков
        List<LastFmService.TrackInfo> popularTracks = lastFmService.getGlobalTopTracks().stream()
                .filter(this::isValidTrackInfo)
                .collect(Collectors.toList());

        // 🔥 УВЕЛИЧИВАЕМ количество популярных треков
        List<LastFmService.TrackInfo> topPopular = popularTracks.stream()
                .limit(POPULAR_RECOMMENDATIONS_LIMIT) // Теперь 7 вместо 5
                .collect(Collectors.toList());

        for (LastFmService.TrackInfo popularTrackInfo : topPopular) {
            Track popularTrack = findOrCreateTrack(popularTrackInfo);
            if (popularTrack != null && !isTrackInUserCollection(userId, popularTrack.getId())) {
                Recommendation rec = new Recommendation(
                        userId,
                        popularTrack.getId(),
                        "popular",
                        0.7
                );
                recommendations.add(rec);
            }
        }

        logger.debug("Generated {} popular recommendations", recommendations.size());
        return recommendations;
    }

    private List<Recommendation> generateRecentBasedRecommendations(Long userId) {
        List<Recommendation> recommendations = new ArrayList<>();

        try {
            // 🔥 УВЕЛИЧИВАЕМ количество недавних треков для анализа
            List<UserPreference> recentPreferences = userPreferenceDAO.findRecentByUserId(userId, 8); // Было 5

            for (UserPreference preference : recentPreferences) {
                Track recentTrack = trackService.getTrackById(preference.getTrackId());
                if (recentTrack == null || !isValidTrack(recentTrack)) continue;

                List<Track> allTracks = trackService.getAllTracks().stream()
                        .filter(this::isValidTrack)
                        .collect(Collectors.toList());

                // 🔥 УВЕЛИЧИВАЕМ количество похожих для недавних треков
                List<Track> similarTracks = findSimilarTracksEnhanced(recentTrack, allTracks, 4); // Было 3

                for (Track similarTrack : similarTracks) {
                    if (!isTrackInUserCollection(userId, similarTrack.getId())) {
                        double similarityScore = calculateEnhancedSimilarityScore(recentTrack, similarTrack);
                        Recommendation rec = new Recommendation(
                                userId,
                                similarTrack.getId(),
                                "recent_based",
                                similarityScore * 0.7
                        );
                        recommendations.add(rec);
                    }
                }

                // Ограничиваем общее количество
                if (recommendations.size() >= RECENT_BASED_LIMIT) break;
            }
        } catch (Exception e) {
            logger.error("Error generating recent-based recommendations", e);
        }

        logger.debug("Generated {} recent-based recommendations", recommendations.size());
        return recommendations;
    }

    private List<Track> findSimilarTracksEnhanced(Track sourceTrack, List<Track> allTracks, int limit) {
        return allTracks.stream()
                .filter(track -> !track.getId().equals(sourceTrack.getId()))
                .filter(track -> !track.getArtistName().equalsIgnoreCase(sourceTrack.getArtistName()))
                .filter(this::isValidTrack)
                .sorted((t1, t2) -> Double.compare(
                        calculateEnhancedSimilarityScore(sourceTrack, t2),
                        calculateEnhancedSimilarityScore(sourceTrack, t1)
                ))
                .limit(limit) // 🔥 Этот лимит теперь больше (5 вместо 3)
                .collect(Collectors.toList());
    }
    /**
     * РЕАЛЬНЫЙ алгоритм схожести вместо Math.random()
     */
    private double calculateEnhancedSimilarityScore(Track track1, Track track2) {
        double totalScore = 0.0;

        // 1. Схожесть по жанрам (40%)
        totalScore += calculateGenreSimilarity(track1, track2) * GENRE_WEIGHT;

        // 2. Схожесть по исполнителю (30%)
        totalScore += calculateArtistSimilarity(track1, track2) * ARTIST_WEIGHT;

        // 3. Схожесть по длительности (15%)
        totalScore += calculateDurationSimilarity(track1, track2) * DURATION_WEIGHT;

        // 4. Схожесть по популярности (15%)
        totalScore += calculatePopularitySimilarity(track1, track2) * POPULARITY_WEIGHT;

        return Math.min(1.0, Math.max(0.0, totalScore));
    }

    private double calculateGenreSimilarity(Track track1, Track track2) {
        List<String> genres1 = getTrackGenres(track1);
        List<String> genres2 = getTrackGenres(track2);

        if (genres1.isEmpty() || genres2.isEmpty()) {
            return 0.3; // Базовая схожесть если жанры неизвестны
        }

        long commonGenres = genres1.stream()
                .filter(genres2::contains)
                .count();

        return (double) commonGenres / Math.max(genres1.size(), genres2.size());
    }

    private double calculateArtistSimilarity(Track track1, Track track2) {
        // Пока просто проверяем того же исполнителя
        // В будущем можно добавить логику для похожих исполнителей
        return track1.getArtistName().equalsIgnoreCase(track2.getArtistName()) ? 1.0 : 0.0;
    }

    private double calculateDurationSimilarity(Track track1, Track track2) {
        int duration1 = track1.getDuration() != null ? track1.getDuration() : 0;
        int duration2 = track2.getDuration() != null ? track2.getDuration() : 0;

        if (duration1 == 0 || duration2 == 0) return 0.5;

        double ratio = (double) Math.min(duration1, duration2) / Math.max(duration1, duration2);
        return ratio > 0.7 ? ratio : 0.0; // Считаем схожими если разница < 30%
    }

    private double calculatePopularitySimilarity(Track track1, Track track2) {
        // Упрощенная логика - оба трека либо популярные, либо нет
        boolean isPopular1 = track1.getPlayCount() != null && track1.getPlayCount() > 1000000;
        boolean isPopular2 = track2.getPlayCount() != null && track2.getPlayCount() > 1000000;

        return isPopular1 == isPopular2 ? 1.0 : 0.0;
    }

    private List<String> getTrackGenres(Track track) {
        if (track.getGenre() != null && !track.getGenre().isEmpty()) {
            return Arrays.asList(track.getGenre().split(",\\s*"));
        }
        return Collections.emptyList();
    }

    private Track findOrCreateTrack(LastFmService.TrackInfo trackInfo) {
        if (trackInfo == null || !isValidTrackInfo(trackInfo)) {
            logger.debug("Invalid TrackInfo, skipping recommendation");
            return null;
        }

        // Сначала ищем существующий трек
        List<Track> existingTracks = trackService.getTracksByArtist(trackInfo.getArtist());
        Track existingTrack = existingTracks.stream()
                .filter(track -> track.getTitle().equalsIgnoreCase(trackInfo.getName()))
                .filter(this::isValidTrack) // 🔥 Фильтруем невалидные треки
                .findFirst()
                .orElse(null);

        if (existingTrack != null) {
            return existingTrack;
        }

        // Создаем новый трек только если данные валидны
        Track newTrack = new Track();
        newTrack.setTitle(trackInfo.getName());
        newTrack.setArtistName(trackInfo.getArtist());
        newTrack.setAlbumTitle(trackInfo.getAlbum());
        newTrack.setDuration(trackInfo.getDuration());
        newTrack.setGenre(trackInfo.getGenres() != null ?
                String.join(", ", trackInfo.getGenres()) : "Unknown");
        newTrack.setSource("LastFM-Recommended");

        // Проверяем валидность перед сохранением
        if (!isValidTrack(newTrack)) {
            logger.debug("Created track is invalid, not saving: {} - {}",
                    trackInfo.getArtist(), trackInfo.getName());
            return null;
        }

        boolean saved = trackService.addTrack(newTrack);
        return saved ? newTrack : null;
    }

    /**
     * Проверяет валидность TrackInfo из Last.fm
     */
    private boolean isValidTrackInfo(LastFmService.TrackInfo trackInfo) {
        if (trackInfo == null) {
            return false;
        }

        boolean hasValidName = trackInfo.getName() != null &&
                !trackInfo.getName().trim().isEmpty() &&
                !trackInfo.getName().equalsIgnoreCase("unknown");

        boolean hasValidArtist = trackInfo.getArtist() != null &&
                !trackInfo.getArtist().trim().isEmpty() &&
                !trackInfo.getArtist().equalsIgnoreCase("unknown");

        return hasValidName && hasValidArtist;
    }

    private boolean isTrackInUserCollection(Long userId, Long trackId) {
        return userPreferenceDAO.findByUserAndTrack(userId, trackId).isPresent();
    }

    // Остальные методы остаются без изменений
    public boolean markRecommendationAsViewed(Long recommendationId) {
        return recommendationDAO.markAsViewed(recommendationId);
    }

    public boolean markAllRecommendationsAsViewed(Long userId) {
        return recommendationDAO.markAllAsViewed(userId);
    }

    public List<Recommendation> getUserRecommendationHistory(Long userId) {
        return recommendationDAO.findByUserId(userId);
    }
}