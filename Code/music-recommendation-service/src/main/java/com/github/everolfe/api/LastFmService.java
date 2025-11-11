package com.github.everolfe.api;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.everolfe.config.Config;
import java.util.Collections;
import java.util.Map;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class LastFmService {
    private static final Logger logger = LoggerFactory.getLogger(LastFmService.class);
    private final HttpClient httpClient;
    private final ObjectMapper objectMapper;
    private final String apiKey;
    private final String baseUrl;
    private final ExecutorService executorService;

    public LastFmService() {
        this.httpClient = HttpClient.newHttpClient();
        this.objectMapper = new ObjectMapper();
        this.apiKey = Config.getLastFmApiKey();
        this.baseUrl = Config.getLastFmBaseUrl();
        this.executorService = Executors.newFixedThreadPool(3); // Для параллельных запросов
    }

    public List<TrackInfo> searchTracks(String query) {
        if (Config.isApiMockEnabled()) {
            return getMockTracks(query);
        }

        try {
            String encodedQuery = URLEncoder.encode(query, StandardCharsets.UTF_8);
            String url = String.format("%s?method=track.search&track=%s&api_key=%s&format=json",
                    baseUrl, encodedQuery, apiKey);

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .GET()
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() == 200) {
                List<TrackInfo> tracks = parseSearchResults(response.body());

                // Для первых нескольких треков получаем дополнительную информацию об альбомах
                if (!tracks.isEmpty()) {
                    enhanceTracksWithAlbumInfo(tracks);
                }

                return tracks;
            } else {
                logger.error("Last.fm API error: {}", response.statusCode());
                return getMockTracks(query);
            }
        } catch (Exception e) {
            logger.error("Error calling Last.fm API", e);
            return getMockTracks(query);
        }
    }

    /**
     * Улучшает информацию о треках, получая данные об альбомах
     */
    private void enhanceTracksWithAlbumInfo(List<TrackInfo> tracks) {
        try {
            // Ограничиваем количество запросов для производительности
            int limit = Math.min(tracks.size(), 50);
            List<CompletableFuture<Void>> futures = new ArrayList<>();

            for (int i = 0; i < limit; i++) {
                TrackInfo track = tracks.get(i);
                // Пропускаем треки, у которых уже есть информация об альбоме
                if (track.getAlbum() == null || track.getAlbum().isEmpty()) {
                    CompletableFuture<Void> future = CompletableFuture.runAsync(() -> {
                        try {
                            TrackInfo enhancedInfo = getTrackInfo(track.getArtist(), track.getName());
                            if (enhancedInfo != null && enhancedInfo.getAlbum() != null) {
                                track.setAlbum(enhancedInfo.getAlbum());
                                track.setDuration(enhancedInfo.getDuration());
                                if (enhancedInfo.getGenres() != null && !enhancedInfo.getGenres().isEmpty()) {
                                    track.setGenres(enhancedInfo.getGenres());
                                }
                            }
                        } catch (Exception e) {
                            logger.debug("Error enhancing track info for: {} - {}", track.getArtist(), track.getName());
                        }
                    }, executorService);
                    futures.add(future);

                    // Небольшая задержка между запросами чтобы не перегружать API
                    Thread.sleep(100);
                }
            }

            // Ждем завершения всех запросов
            CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).join();

        } catch (Exception e) {
            logger.warn("Error enhancing tracks with album info", e);
        }
    }

    /**
     * Альтернативный метод поиска с получением полной информации
     */
    public List<TrackInfo> searchTracksWithDetails(String query, int limit) {
        if (Config.isApiMockEnabled()) {
            return getMockTracks(query);
        }

        try {
            List<TrackInfo> tracks = new ArrayList<>();

            // Сначала получаем базовые результаты поиска
            String encodedQuery = URLEncoder.encode(query, StandardCharsets.UTF_8);
            String searchUrl = String.format("%s?method=track.search&track=%s&api_key=%s&format=json&limit=%d",
                    baseUrl, encodedQuery, apiKey, limit);

            HttpRequest searchRequest = HttpRequest.newBuilder()
                    .uri(URI.create(searchUrl))
                    .GET()
                    .build();

            HttpResponse<String> searchResponse = httpClient.send(searchRequest, HttpResponse.BodyHandlers.ofString());

            if (searchResponse.statusCode() == 200) {
                List<TrackInfo> searchResults = parseSearchResults(searchResponse.body());

                // Для каждого результата получаем полную информацию
                for (TrackInfo track : searchResults) {
                    try {
                        TrackInfo fullInfo = getTrackInfo(track.getArtist(), track.getName());
                        if (fullInfo != null) {
                            tracks.add(fullInfo);
                        } else {
                            tracks.add(track); // Добавляем базовую информацию если полная недоступна
                        }

                        // Ограничиваем количество запросов
                        if (tracks.size() >= limit) {
                            break;
                        }

                        // Задержка между запросами
                        Thread.sleep(200);
                    } catch (Exception e) {
                        logger.warn("Error getting full info for track: {} - {}", track.getArtist(), track.getName());
                        tracks.add(track); // Добавляем базовую информацию в случае ошибки
                    }
                }
            }

            return tracks;
        } catch (Exception e) {
            logger.error("Error in searchTracksWithDetails for: {}", query, e);
            return getMockTracks(query);
        }
    }

    public TrackInfo getTrackInfo(String artist, String track) {
        if (Config.isApiMockEnabled()) {
            return getMockTrackInfo(artist, track);
        }

        try {
            String encodedArtist = URLEncoder.encode(artist, StandardCharsets.UTF_8);
            String encodedTrack = URLEncoder.encode(track, StandardCharsets.UTF_8);

            String url = String.format("%s?method=track.getInfo&artist=%s&track=%s&api_key=%s&format=json",
                    baseUrl, encodedArtist, encodedTrack, apiKey);

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .GET()
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() == 200) {
                return parseTrackInfo(response.body());
            } else {
                logger.error("Last.fm API error: {}", response.statusCode());
                return getMockTrackInfo(artist, track);
            }
        } catch (Exception e) {
            logger.error("Error calling Last.fm API", e);
            return getMockTrackInfo(artist, track);
        }
    }

    /**
     * Получает глобальные топ треки
     */
    public List<TrackInfo> getGlobalTopTracks() {
        if (Config.isApiMockEnabled()) {
            return getMockPopularTracks().stream()
                    .limit(20) // 🔥 Больше мок-данных
                    .collect(Collectors.toList());
        }

        try {
            // 🔥 Запрашиваем больше популярных треков
            String url = String.format("%s?method=chart.gettoptracks&api_key=%s&format=json&limit=30",
                    baseUrl, apiKey); // Было 50, но 30 достаточно

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .GET()
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() == 200) {
                return parseTopTracks(response.body()).stream()
                        .limit(20) // Ограничиваем для производительности
                        .collect(Collectors.toList());
            } else {
                logger.error("Last.fm API error: {}", response.statusCode());
                return getMockPopularTracks().stream()
                        .limit(20)
                        .collect(Collectors.toList());
            }
        } catch (Exception e) {
            logger.error("Error getting global top tracks", e);
            return getMockPopularTracks().stream()
                    .limit(20)
                    .collect(Collectors.toList());
        }
    }

    /**
     * Получает популярные треки по топовым артистам
     */
    public List<TrackInfo> getPopularTracks() {
        if (Config.isApiMockEnabled()) {
            return getMockPopularTracks();
        }

        try {
            List<TrackInfo> popularTracks = new ArrayList<>();

            // Получаем топ артистов
            List<String> topArtists = getTopArtists();

            // Для каждого топ артиста получаем его популярные треки
            for (String artist : topArtists) {
                try {
                    List<TrackInfo> artistTracks = getArtistTopTracks(artist);
                    popularTracks.addAll(artistTracks);

                    // Ограничим количество треков для производительности
                    if (popularTracks.size() >= 50) {
                        break;
                    }

                    // Небольшая задержка чтобы не перегружать API
                    Thread.sleep(100);
                } catch (Exception e) {
                    logger.warn("Error getting tracks for artist: {}", artist, e);
                }
            }

            return popularTracks;
        } catch (Exception e) {
            logger.error("Error getting popular tracks", e);
            return getMockPopularTracks();
        }
    }

    /**
     * Получает список популярных артистов
     */
    private List<String> getTopArtists() {
        List<String> artists = new ArrayList<>();
        try {
            String url = String.format("%s?method=chart.gettopartists&api_key=%s&format=json&limit=20",
                    baseUrl, apiKey);

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .GET()
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() == 200) {
                JsonNode root = objectMapper.readTree(response.body());
                JsonNode artistsNode = root.path("artists").path("artist");

                for (JsonNode artistNode : artistsNode) {
                    String artistName = artistNode.path("name").asText();
                    if (!artistName.isEmpty()) {
                        artists.add(artistName);
                    }
                }
            }
        } catch (Exception e) {
            logger.error("Error getting top artists", e);
            // Возвращаем мок-данные если API не доступно
            artists.addAll(List.of(
                    "The Weeknd", "Taylor Swift", "Bad Bunny", "Drake", "Ed Sheeran",
                    "Billie Eilish", "Harry Styles", "Dua Lipa", "Ariana Grande", "Post Malone",
                    "Kanye West", "Coldplay", "Maroon 5", "Bruno Mars", "Imagine Dragons",
                    "Metallica", "Queen", "The Beatles", "Michael Jackson", "Madonna"
            ));
        }
        return artists;
    }

    /**
     * Получает топ треки артиста
     */
    private List<TrackInfo> getArtistTopTracks(String artist) {
        List<TrackInfo> tracks = new ArrayList<>();
        try {
            String encodedArtist = URLEncoder.encode(artist, StandardCharsets.UTF_8);
            String url = String.format("%s?method=artist.gettoptracks&artist=%s&api_key=%s&format=json&limit=5",
                    baseUrl, encodedArtist, apiKey);

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .GET()
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() == 200) {
                JsonNode root = objectMapper.readTree(response.body());
                JsonNode tracksNode = root.path("toptracks").path("track");

                List<CompletableFuture<TrackInfo>> futures = new ArrayList<>();

                for (JsonNode trackNode : tracksNode) {
                    CompletableFuture<TrackInfo> future = CompletableFuture.supplyAsync(() -> {
                        try {
                            String trackName = trackNode.path("name").asText();
                            String trackArtist = trackNode.path("artist").path("name").asText();

                            // Сначала создаем базовый трек
                            TrackInfo track = new TrackInfo();
                            track.setName(trackName);
                            track.setArtist(trackArtist);
                            track.setUrl(trackNode.path("url").asText());

                            // Получаем базовую информацию
                            JsonNode durationNode = trackNode.path("duration");
                            if (!durationNode.isMissingNode() && !durationNode.asText().isEmpty()) {
                                try {
                                    int durationMs = durationNode.asInt();
                                    track.setDuration(durationMs / 1000);
                                } catch (Exception e) {
                                    track.setDuration(0);
                                }
                            }

                            JsonNode playCountNode = trackNode.path("playcount");
                            if (!playCountNode.isMissingNode() && !playCountNode.asText().isEmpty()) {
                                try {
                                    track.setPlayCount(playCountNode.asInt());
                                } catch (Exception e) {
                                    logger.debug("Error parsing playcount: {}", playCountNode.asText());
                                }
                            }

                            // Пытаемся получить информацию об альбоме из базовых данных
                            JsonNode albumNode = trackNode.path("album");
                            if (!albumNode.isMissingNode() && !albumNode.isNull()) {
                                String albumTitle = albumNode.path("title").asText();
                                if (!albumTitle.isEmpty() && !albumTitle.equals("null")) {
                                    track.setAlbum(albumTitle);
                                }
                            }

                            // Если альбом не найден в базовых данных, делаем дополнительный запрос
                            if (track.getAlbum() == null || track.getAlbum().isEmpty() || track.getAlbum().equals("Single")) {
                                TrackInfo fullTrackInfo = getTrackInfo(trackArtist, trackName);
                                if (fullTrackInfo != null && fullTrackInfo.getAlbum() != null &&
                                        !fullTrackInfo.getAlbum().isEmpty() && !fullTrackInfo.getAlbum().equals("Single")) {
                                    track.setAlbum(fullTrackInfo.getAlbum());
                                    // Также обновляем другую информацию если нужно
                                    if (track.getDuration() == null || track.getDuration() == 0) {
                                        track.setDuration(fullTrackInfo.getDuration());
                                    }
                                    if (fullTrackInfo.getGenres() != null && !fullTrackInfo.getGenres().isEmpty()) {
                                        track.setGenres(fullTrackInfo.getGenres());
                                    }
                                } else {
                                    track.setAlbum("Single");
                                }
                            }

                            // Если после всех попыток альбом не определен, ставим "Unknown Album"
                            if (track.getAlbum() == null || track.getAlbum().isEmpty()) {
                                track.setAlbum("Unknown Album");
                            }

                            logger.debug("Processed track: {} - {} (Album: {})",
                                    track.getArtist(), track.getName(), track.getAlbum());

                            return track;
                        } catch (Exception e) {
                            logger.error("Error processing track for artist: {}", artist, e);
                            return null;
                        }
                    }, executorService);

                    futures.add(future);

                    // Небольшая задержка между запросами чтобы не перегружать API
                    try {
                        Thread.sleep(200);
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                    }
                }

                // Ждем завершения всех асинхронных задач
                CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).join();

                // Собираем результаты
                for (CompletableFuture<TrackInfo> future : futures) {
                    try {
                        TrackInfo track = future.get();
                        if (track != null) {
                            tracks.add(track);
                        }
                    } catch (Exception e) {
                        logger.error("Error getting track result", e);
                    }
                }

            }
        } catch (Exception e) {
            logger.error("Error getting top tracks for artist: {}", artist, e);
        }
        return tracks;
    }

    private List<TrackInfo> parseSearchResults(String jsonResponse) {
        List<TrackInfo> tracks = new ArrayList<>();
        try {
            JsonNode root = objectMapper.readTree(jsonResponse);
            JsonNode results = root.path("results").path("trackmatches").path("track");

            for (JsonNode trackNode : results) {
                TrackInfo track = new TrackInfo();
                track.setName(trackNode.path("name").asText());
                track.setArtist(trackNode.path("artist").asText());
                track.setUrl(trackNode.path("url").asText());

                // В поисковых результатах обычно нет информации об альбомах
                // Но иногда может быть в поле album
                JsonNode albumNode = trackNode.path("album");
                if (!albumNode.isMissingNode() && !albumNode.asText().isEmpty()) {
                    track.setAlbum(albumNode.asText());
                }

                // Добавляем информацию о прослушиваниях если есть
                JsonNode listenersNode = trackNode.path("listeners");
                if (!listenersNode.isMissingNode()) {
                    track.setPlayCount(listenersNode.asInt());
                }

                tracks.add(track);
            }
        } catch (Exception e) {
            logger.error("Error parsing Last.fm search results", e);
        }
        return tracks;
    }

    private List<TrackInfo> parseTopTracks(String jsonResponse) {
        List<TrackInfo> tracks = new ArrayList<>();
        try {
            JsonNode root = objectMapper.readTree(jsonResponse);
            JsonNode tracksNode = root.path("tracks").path("track");

            for (JsonNode trackNode : tracksNode) {
                TrackInfo track = new TrackInfo();
                track.setName(trackNode.path("name").asText());
                track.setArtist(trackNode.path("artist").path("name").asText());

                // Исправляем парсинг длительности
                JsonNode durationNode = trackNode.path("duration");
                if (!durationNode.isMissingNode() && !durationNode.asText().isEmpty()) {
                    try {
                        int durationMs = durationNode.asInt();
                        track.setDuration(durationMs / 1000); // Конвертируем мс в секунды
                    } catch (Exception e) {
                        track.setDuration(0);
                    }
                } else {
                    track.setDuration(0);
                }

                track.setUrl(trackNode.path("url").asText());

                // Получаем информацию о прослушиваниях
                JsonNode playcountNode = trackNode.path("playcount");
                if (!playcountNode.isMissingNode() && !playcountNode.asText().isEmpty()) {
                    try {
                        track.setPlayCount(playcountNode.asInt());
                    } catch (Exception e) {
                        logger.debug("Error parsing playcount: {}", playcountNode.asText());
                    }
                }

                tracks.add(track);
            }
            enhanceTracksWithAlbumInfo(tracks);
        } catch (Exception e) {
            logger.error("Error parsing top tracks", e);
        }
        return tracks;
    }

    private TrackInfo parseTrackInfo(String jsonResponse) {
        try {
            JsonNode root = objectMapper.readTree(jsonResponse);
            JsonNode trackNode = root.path("track");

            // Проверяем, есть ли информация о треке
            if (trackNode.isMissingNode() || trackNode.path("name").isMissingNode()) {
                logger.warn("Track info not found in response");
                return null;
            }

            TrackInfo track = new TrackInfo();
            track.setName(trackNode.path("name").asText());
            track.setArtist(trackNode.path("artist").path("name").asText());

            // Получаем информацию об альбоме - исправленный код
            JsonNode albumNode = trackNode.path("album");
            if (!albumNode.isMissingNode() && !albumNode.isNull()) {
                String albumTitle = albumNode.path("title").asText();
                if (!albumTitle.isEmpty() && !albumTitle.equals("null")) {
                    track.setAlbum(albumTitle);
                } else {
                    track.setAlbum("Single"); // Если альбом не указан, считаем это синглом
                }
            } else {
                track.setAlbum("Single"); // Если нет узла album, считаем это синглом
            }

            // Исправляем парсинг длительности (в миллисекундах, нужно преобразовать в секунды)
            JsonNode durationNode = trackNode.path("duration");
            if (!durationNode.isMissingNode() && !durationNode.asText().isEmpty()) {
                try {
                    int durationMs = durationNode.asInt();
                    track.setDuration(durationMs / 1000); // Конвертируем мс в секунды
                } catch (Exception e) {
                    logger.debug("Error parsing duration: {}", durationNode.asText());
                    track.setDuration(0);
                }
            } else {
                track.setDuration(0);
            }

            track.setUrl(trackNode.path("url").asText());

            // Получаем счетчик прослушиваний
            JsonNode playcountNode = trackNode.path("playcount");
            if (!playcountNode.isMissingNode() && !playcountNode.asText().isEmpty()) {
                try {
                    track.setPlayCount(playcountNode.asInt());
                } catch (Exception e) {
                    logger.debug("Error parsing playcount: {}", playcountNode.asText());
                }
            }

            // Получаем количество слушателей
            JsonNode listenersNode = trackNode.path("listeners");
            if (!listenersNode.isMissingNode() && !listenersNode.asText().isEmpty()) {
                try {
                    // Можно использовать listeners как альтернативу playcount
                    if (track.getPlayCount() == null) {
                        track.setPlayCount(listenersNode.asInt());
                    }
                } catch (Exception e) {
                    logger.debug("Error parsing listeners: {}", listenersNode.asText());
                }
            }

            // Парсим жанры
            List<String> genres = new ArrayList<>();
            JsonNode tags = trackNode.path("toptags").path("tag");
            for (JsonNode tag : tags) {
                String genreName = tag.path("name").asText();
                if (!genreName.isEmpty() && !genreName.equals("null")) {
                    genres.add(genreName);
                }
            }
            track.setGenres(genres);

            logger.debug("Parsed track: {} - {} (Album: {})",
                    track.getArtist(), track.getName(), track.getAlbum());

            return track;
        } catch (Exception e) {
            logger.error("Error parsing Last.fm track info", e);
            return null;
        }
    }

    // Остальные методы (getMockTracks, getMockPopularTracks, getMockTrackInfo) остаются без изменений...
    // Мок-данные для поиска
    private List<TrackInfo> getMockTracks(String query) {
        List<TrackInfo> mockTracks = new ArrayList<>();
        String[] mockData = {
                "Bohemian Rhapsody|Queen|A Night at the Opera|354|Rock|2500000000",
                "Blinding Lights|The Weeknd|After Hours|200|Pop|2850000000",
                "Shape of You|Ed Sheeran|÷|233|Pop|2720000000",
                "Stairway to Heaven|Led Zeppelin|Led Zeppelin IV|482|Rock|1500000000",
                "Bad Guy|Billie Eilish|When We All Fall Asleep|194|Pop|2200000000"
        };

        for (String data : mockData) {
            String[] parts = data.split("\\|");
            if (parts[0].toLowerCase().contains(query.toLowerCase()) ||
                    parts[1].toLowerCase().contains(query.toLowerCase())) {
                TrackInfo track = new TrackInfo();
                track.setName(parts[0]);
                track.setArtist(parts[1]);
                track.setAlbum(parts[2]);
                track.setDuration(Integer.parseInt(parts[3]));
                track.setGenres(List.of(parts[4]));
                track.setPlayCount(Integer.parseInt(parts[5]));
                track.setUrl("https://www.last.fm/music/" + parts[1] + "/_/" + parts[0]);
                mockTracks.add(track);
            }
        }
        return mockTracks;
    }

    // Мок-данные для популярных треков
    private List<TrackInfo> getMockPopularTracks() {
        List<TrackInfo> mockTracks = new ArrayList<>();
        String[][] mockData = {
                {"Blinding Lights", "The Weeknd", "After Hours", "200", "Pop", "2850000000"},
                {"Shape of You", "Ed Sheeran", "÷", "233", "Pop", "2720000000"},
                {"Dance Monkey", "Tones and I", "The Kids Are Coming", "210", "Pop", "2670000000"},
                {"Someone You Loved", "Lewis Capaldi", "Divinely Uninspired", "182", "Pop", "2400000000"},
                {"Stay", "The Kid LAROI & Justin Bieber", "F*CK LOVE 3", "141", "Pop", "2300000000"},
                {"Bad Guy", "Billie Eilish", "When We All Fall Asleep", "194", "Pop", "2200000000"},
                {"Watermelon Sugar", "Harry Styles", "Fine Line", "174", "Pop", "2100000000"},
                {"Levitating", "Dua Lipa", "Future Nostalgia", "203", "Pop", "2050000000"},
                {"Save Your Tears", "The Weeknd", "After Hours", "215", "Pop", "2000000000"},
                {"Flowers", "Miley Cyrus", "Endless Summer Vacation", "200", "Pop", "1950000000"},
                {"As It Was", "Harry Styles", "Harry's House", "167", "Pop", "1900000000"},
                {"Heat Waves", "Glass Animals", "Dreamland", "238", "Indie", "1850000000"},
                {"Easy On Me", "Adele", "30", "224", "Pop", "1800000000"},
                {"Shallow", "Lady Gaga & Bradley Cooper", "A Star Is Born", "216", "Pop", "1750000000"},
                {"Believer", "Imagine Dragons", "Evolve", "204", "Rock", "1700000000"}
        };

        for (String[] data : mockData) {
            TrackInfo track = new TrackInfo();
            track.setName(data[0]);
            track.setArtist(data[1]);
            track.setAlbum(data[2]);
            track.setDuration(Integer.parseInt(data[3]));
            track.setGenres(List.of(data[4]));
            track.setPlayCount(Integer.parseInt(data[5]));
            track.setUrl("https://www.last.fm/music/" + data[1] + "/_/" + data[0]);
            mockTracks.add(track);
        }
        return mockTracks;
    }

    private TrackInfo getMockTrackInfo(String artist, String track) {
        TrackInfo mockTrack = new TrackInfo();
        mockTrack.setName(track);
        mockTrack.setArtist(artist);
        mockTrack.setAlbum("Demo Album");
        mockTrack.setDuration(180);
        mockTrack.setGenres(List.of("Rock", "Pop"));
        mockTrack.setPlayCount(1000000);
        mockTrack.setUrl("https://www.last.fm/music/" + artist + "/_/" + track);
        return mockTrack;
    }

    public List<TrackInfo> getSimilarTracks(String artist, String trackName, int limit) {
        // 🔥 УВЕЛИЧИВАЕМ лимит в запросе к API
        if (Config.isApiMockEnabled()) {
            return getMockSimilarTracks(artist, trackName).stream()
                    .limit(limit)
                    .collect(Collectors.toList());
        }

        try {
            String encodedArtist = URLEncoder.encode(artist, StandardCharsets.UTF_8);
            String encodedTrack = URLEncoder.encode(trackName, StandardCharsets.UTF_8);

            // 🔥 Запрашиваем больше треков у API
            String url = String.format("%s?method=track.getSimilar&artist=%s&track=%s&api_key=%s&format=json&limit=%d",
                    baseUrl, encodedArtist, encodedTrack, apiKey, Math.max(limit, 10)); // Минимум 10

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .GET()
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() == 200) {
                return parseSimilarTracksResponse(response.body(), limit);
            } else {
                logger.error("Last.fm API error for similar tracks: {}", response.statusCode());
                return getMockSimilarTracks(artist, trackName).stream()
                        .limit(limit)
                        .collect(Collectors.toList());
            }
        } catch (Exception e) {
            logger.error("Error getting similar tracks from Last.fm for: {} - {}", artist, trackName, e);
            return getMockSimilarTracks(artist, trackName).stream()
                    .limit(limit)
                    .collect(Collectors.toList());
        }
    }

    /**
     * Парсит ответ от track.getSimilar
     */
    private List<TrackInfo> parseSimilarTracksResponse(String jsonResponse, int limit) {
        List<TrackInfo> similarTracks = new ArrayList<>();
        try {
            JsonNode root = objectMapper.readTree(jsonResponse);
            JsonNode similarTracksNode = root.path("similartracks").path("track");

            int count = 0;
            for (JsonNode trackNode : similarTracksNode) {
                if (count >= limit) break;

                TrackInfo track = new TrackInfo();
                track.setName(trackNode.path("name").asText());
                track.setArtist(trackNode.path("artist").path("name").asText());
                track.setUrl(trackNode.path("url").asText());

                // Получаем дополнительную информацию о треке
                TrackInfo fullInfo = getTrackInfo(track.getArtist(), track.getName());
                if (fullInfo != null) {
                    track.setAlbum(fullInfo.getAlbum());
                    track.setDuration(fullInfo.getDuration());
                    track.setGenres(fullInfo.getGenres());
                    track.setPlayCount(fullInfo.getPlayCount());
                }

                similarTracks.add(track);
                count++;

                // Задержка чтобы не перегружать API
                Thread.sleep(100);
            }
        } catch (Exception e) {
            logger.error("Error parsing similar tracks response", e);
        }
        return similarTracks;
    }

    /**
     * Мок-данные для похожих треков
     */
    private List<TrackInfo> getMockSimilarTracks(String artist, String trackName) {
        // Базовые мок-данные на основе артиста
        Map<String, List<String[]>> mockSimilarData = Map.of(
                "Queen", List.of(
                        new String[]{"We Will Rock You", "Queen", "News of the World", "122", "Rock", "1500000000"},
                        new String[]{"Another One Bites the Dust", "Queen", "The Game", "215", "Rock", "1400000000"},
                        new String[]{"Radio Ga Ga", "Queen", "The Works", "345", "Rock", "900000000"}
                ),
                "The Weeknd", List.of(
                        new String[]{"Save Your Tears", "The Weeknd", "After Hours", "215", "Pop", "2000000000"},
                        new String[]{"Starboy", "The Weeknd", "Starboy", "230", "Pop", "1800000000"},
                        new String[]{"Die For You", "The Weeknd", "Starboy", "260", "Pop", "1200000000"}
                )
        );

        List<TrackInfo> mockTracks = new ArrayList<>();
        List<String[]> similarData = mockSimilarData.getOrDefault(artist, Collections.emptyList());

        for (String[] data : similarData) {
            TrackInfo track = new TrackInfo();
            track.setName(data[0]);
            track.setArtist(data[1]);
            track.setAlbum(data[2]);
            track.setDuration(Integer.parseInt(data[3]));
            track.setGenres(List.of(data[4]));
            track.setPlayCount(Integer.parseInt(data[5]));
            track.setUrl("https://www.last.fm/music/" + data[1] + "/_/" + data[0]);
            mockTracks.add(track);
        }

        return mockTracks;
    }

    public List<AlbumInfo> searchAlbums(String query) {
        if (Config.isApiMockEnabled()) {
            return getMockAlbums(query);
        }

        try {
            String encodedQuery = URLEncoder.encode(query, StandardCharsets.UTF_8);
            String url = String.format("%s?method=album.search&album=%s&api_key=%s&format=json",
                    baseUrl, encodedQuery, apiKey);

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .GET()
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() == 200) {
                return parseAlbumSearchResults(response.body());
            } else {
                logger.error("Last.fm API error for album search: {}", response.statusCode());
                return getMockAlbums(query);
            }
        } catch (Exception e) {
            logger.error("Error searching albums from Last.fm: {}", query, e);
            return getMockAlbums(query);
        }
    }

    /**
     * Получение информации об альбоме
     */
    public AlbumInfo getAlbumInfo(String artist, String album) {
        if (Config.isApiMockEnabled()) {
            return getMockAlbumInfo(artist, album);
        }

        try {
            String encodedArtist = URLEncoder.encode(artist, StandardCharsets.UTF_8);
            String encodedAlbum = URLEncoder.encode(album, StandardCharsets.UTF_8);

            String url = String.format("%s?method=album.getInfo&artist=%s&album=%s&api_key=%s&format=json",
                    baseUrl, encodedArtist, encodedAlbum, apiKey);

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .GET()
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() == 200) {
                return parseAlbumInfo(response.body());
            } else {
                logger.error("Last.fm API error for album info: {}", response.statusCode());
                return getMockAlbumInfo(artist, album);
            }
        } catch (Exception e) {
            logger.error("Error getting album info from Last.fm: {} - {}", artist, album, e);
            return getMockAlbumInfo(artist, album);
        }
    }

    private List<AlbumInfo> parseAlbumSearchResults(String jsonResponse) {
        List<AlbumInfo> albums = new ArrayList<>();
        try {
            JsonNode root = objectMapper.readTree(jsonResponse);
            JsonNode results = root.path("results").path("albummatches").path("album");

            for (JsonNode albumNode : results) {
                AlbumInfo album = new AlbumInfo();
                album.setName(albumNode.path("name").asText());
                album.setArtist(albumNode.path("artist").asText());

                // Получаем URL обложки
                JsonNode imageNode = albumNode.path("image").get(2); // Средний размер
                if (imageNode != null && !imageNode.path("#text").asText().isEmpty()) {
                    album.setCoverUrl(imageNode.path("#text").asText());
                }

                albums.add(album);
            }
        } catch (Exception e) {
            logger.error("Error parsing Last.fm album search results", e);
        }
        return albums;
    }

    private AlbumInfo parseAlbumInfo(String jsonResponse) {
        try {
            JsonNode root = objectMapper.readTree(jsonResponse);
            JsonNode albumNode = root.path("album");

            AlbumInfo album = new AlbumInfo();
            album.setName(albumNode.path("name").asText());
            album.setArtist(albumNode.path("artist").asText());

            // Год выпуска
            JsonNode yearNode = albumNode.path("wiki").path("published");
            if (!yearNode.isMissingNode()) {
                String yearText = yearNode.asText();
                if (yearText.length() >= 4) {
                    try {
                        album.setReleaseYear(Integer.parseInt(yearText.substring(0, 4)));
                    } catch (NumberFormatException e) {
                        logger.debug("Could not parse release year: {}", yearText);
                    }
                }
            }

            // Жанры
            List<String> genres = new ArrayList<>();
            JsonNode tags = albumNode.path("tags").path("tag");
            for (JsonNode tag : tags) {
                String genreName = tag.path("name").asText();
                if (!genreName.isEmpty()) {
                    genres.add(genreName);
                }
            }
            album.setGenre(String.join(", ", genres));

            // Обложка
            JsonNode imageNode = albumNode.path("image").get(2); // Средний размер
            if (imageNode != null && !imageNode.path("#text").asText().isEmpty()) {
                album.setCoverUrl(imageNode.path("#text").asText());
            }

            return album;
        } catch (Exception e) {
            logger.error("Error parsing Last.fm album info", e);
            return null;
        }
    }

    // Мок-данные для альбомов
    private List<AlbumInfo> getMockAlbums(String query) {
        List<AlbumInfo> mockAlbums = new ArrayList<>();
        String[][] mockData = {
                {"After Hours", "The Weeknd", "2020", "Pop,R&B", "https://lastfm.freetls.fastly.net/i/u/300x300/0c2509f3e8c142dd9deb6a012a2c9f29.png"},
                {"Future Nostalgia", "Dua Lipa", "2020", "Pop,Disco", "https://lastfm.freetls.fastly.net/i/u/300x300/0c2509f3e8c142dd9deb6a012a2c9f29.png"},
                {"Fine Line", "Harry Styles", "2019", "Pop,Rock", "https://lastfm.freetls.fastly.net/i/u/300x300/0c2509f3e8c142dd9deb6a012a2c9f29.png"}
        };

        for (String[] data : mockData) {
            if (data[0].toLowerCase().contains(query.toLowerCase()) ||
                    data[1].toLowerCase().contains(query.toLowerCase())) {
                AlbumInfo album = new AlbumInfo();
                album.setName(data[0]);
                album.setArtist(data[1]);
                album.setReleaseYear(Integer.parseInt(data[2]));
                album.setGenre(data[3]);
                album.setCoverUrl(data[4]);
                mockAlbums.add(album);
            }
        }
        return mockAlbums;
    }

    private AlbumInfo getMockAlbumInfo(String artist, String album) {
        AlbumInfo mockAlbum = new AlbumInfo();
        mockAlbum.setName(album);
        mockAlbum.setArtist(artist);
        mockAlbum.setReleaseYear(2020);
        mockAlbum.setGenre("Pop, Rock");
        mockAlbum.setCoverUrl("https://lastfm.freetls.fastly.net/i/u/300x300/0c2509f3e8c142dd9deb6a012a2c9f29.png");
        return mockAlbum;
    }

    // Класс AlbumInfo
    public static class AlbumInfo {
        private String name;
        private String artist;
        private Integer releaseYear;
        private String genre;
        private String coverUrl;

        public String getName() { return name; }
        public void setName(String name) { this.name = name; }

        public String getArtist() { return artist; }
        public void setArtist(String artist) { this.artist = artist; }

        public Integer getReleaseYear() { return releaseYear; }
        public void setReleaseYear(Integer releaseYear) { this.releaseYear = releaseYear; }

        public String getGenre() { return genre; }
        public void setGenre(String genre) { this.genre = genre; }

        public String getCoverUrl() { return coverUrl; }
        public void setCoverUrl(String coverUrl) { this.coverUrl = coverUrl; }


        @Override
        public String toString() {
            return name + " - " + artist;
        }
    }


    public static class TrackInfo {
        private String name;
        private String artist;
        private String album;
        private Integer duration;
        private String url;
        private List<String> genres;
        private Integer playCount;

        // Getters and Setters
        public String getName() { return name; }
        public void setName(String name) { this.name = name; }

        public String getArtist() { return artist; }
        public void setArtist(String artist) { this.artist = artist; }

        public String getAlbum() { return album; }
        public void setAlbum(String album) { this.album = album; }

        public Integer getDuration() { return duration; }
        public void setDuration(Integer duration) { this.duration = duration; }

        public String getUrl() { return url; }
        public void setUrl(String url) { this.url = url; }

        public List<String> getGenres() { return genres; }
        public void setGenres(List<String> genres) { this.genres = genres; }

        public Integer getPlayCount() { return playCount; }
        public void setPlayCount(Integer playCount) { this.playCount = playCount; }

        @Override
        public String toString() {
            return String.format("%s - %s (%d plays)", artist, name, playCount != null ? playCount : 0);
        }
    }
}