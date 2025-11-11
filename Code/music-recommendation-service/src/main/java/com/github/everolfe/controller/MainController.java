package com.github.everolfe.controller;

import com.github.everolfe.Main;
import com.github.everolfe.api.LastFmService;
import com.github.everolfe.model.Album;
import com.github.everolfe.model.Recommendation;
import com.github.everolfe.model.SearchCriteria;
import com.github.everolfe.model.Track;
import com.github.everolfe.model.User;
import com.github.everolfe.service.MusicCollectionService;
import com.github.everolfe.service.RecommendationService;
import com.github.everolfe.service.SearchService;
import com.github.everolfe.service.TrackService;
import com.github.everolfe.service.UserService;
import java.util.ArrayList;
import java.util.stream.Collectors;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.image.Image;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import javafx.scene.image.ImageView;
import javax.swing.text.html.*;

public class MainController {
    private static final Logger logger = LoggerFactory.getLogger(MainController.class);

    private Main mainApp;
    private User currentUser;
    private TrackService trackService;
    private MusicCollectionService collectionService;
    private RecommendationService recommendationService;
    private UserService userService;
    private LastFmService lastFmService;

    // Основные элементы интерфейса
    @FXML private TabPane mainTabPane;
    @FXML private Label userInfoLabel;

    // Вкладка поиска музыки
    @FXML private TextField searchField;
    @FXML private TableView<Track> tracksTable;
    @FXML private TableColumn<Track, String> trackTitleColumn;
    @FXML private TableColumn<Track, String> trackArtistColumn;
    @FXML private TableColumn<Track, String> trackAlbumColumn;
    @FXML private TableColumn<Track, Integer> trackDurationColumn;

    // Рекомендации
    @FXML private TableView<Recommendation> recommendationsTable;
    @FXML private TableColumn<Recommendation, String> recTrackColumn;
    @FXML private TableColumn<Recommendation, String> recArtistColumn;
    @FXML private TableColumn<Recommendation, String> recTypeColumn;
    @FXML private TableColumn<Recommendation, Double> recScoreColumn;
    @FXML private Button refreshRecommendationsButton;
    @FXML private Button markAsViewedButton;
    @FXML private Button addRecommendedToCollectionButton;

    // Вкладка коллекции
    @FXML private TableView<Track> collectionTable;

    // Информация о треке
    @FXML private Label trackInfoTitle;
    @FXML private Label trackInfoArtist;
    @FXML private Label trackInfoAlbum;
    @FXML private Label trackInfoDuration;
    @FXML private Label trackInfoGenre;

    // Кнопки действий для поиска
    @FXML private Button addToCollectionButton;
    @FXML private Slider ratingSlider;
    @FXML private Label ratingLabel;

    // Кнопки действий для коллекции
    @FXML private Button removeFromCollectionButton;
    @FXML private Button updateRatingInCollectionButton;
    @FXML private Slider ratingSliderCollection;
    @FXML private Label ratingLabelCollection;

    @FXML private ComboBox<String> searchTypeComboBox;
    @FXML private TextField genreFilterField;
    @FXML private TextField artistFilterField;

    @FXML private TextField albumFilterField;


    // ✅ Для отображения обложек
    @FXML private ImageView albumCoverImage;
    @FXML private Label albumInfoLabel;

    private SearchService searchService;

    private ObservableList<Track> tracksData;
    private ObservableList<Track> collectionData;
    private ObservableList<Recommendation> recommendationsData;

    public MainController() {
        this.trackService = new TrackService();
        this.collectionService = new MusicCollectionService();
        this.recommendationService = new RecommendationService();
        this.userService = new UserService();
        this.lastFmService = new LastFmService(); //
        this.tracksData = FXCollections.observableArrayList();
        this.collectionData = FXCollections.observableArrayList();
        this.recommendationsData = FXCollections.observableArrayList();
        this.searchService = new SearchService();
    }

    public void setMainApp(Main mainApp) {
        this.mainApp = mainApp;
    }

    public void setCurrentUser(User user) {
        this.currentUser = user;
        updateUIForUser();
    }

    @FXML
    private void initialize() {
        logger.info("MainController initialized");
        setupTracksTable();
        setupCollectionTable();
        setupRecommendationsTable();
        loadAllTracks();

        // Настройка слушателей
        tracksTable.getSelectionModel().selectedItemProperty().addListener(
                (observable, oldValue, newValue) -> showTrackDetails(newValue));

        collectionTable.getSelectionModel().selectedItemProperty().addListener(
                (observable, oldValue, newValue) -> onCollectionTrackSelected(newValue));

        recommendationsTable.getSelectionModel().selectedItemProperty().addListener(
                (observable, oldValue, newValue) -> onRecommendationSelected(newValue));

        ratingSlider.valueProperty().addListener((observable, oldValue, newValue) -> {
            ratingLabel.setText(String.format("Оценка: %.0f", newValue));
        });

        ratingSliderCollection.valueProperty().addListener((observable, oldValue, newValue) -> {
            ratingLabelCollection.setText(String.format("%.0f", newValue));
        });

        updateUIForUser();
    }

    private void updateUIForUser() {
        boolean isGuest = (currentUser == null);

        if (isGuest) {
            // Гостевой режим
            userInfoLabel.setText("Гость");
            addToCollectionButton.setDisable(true);
            removeFromCollectionButton.setDisable(true);
            updateRatingInCollectionButton.setDisable(true);
            ratingSlider.setDisable(true);
            ratingSliderCollection.setDisable(true);
            refreshRecommendationsButton.setDisable(true);
            markAsViewedButton.setDisable(true);
            addRecommendedToCollectionButton.setDisable(true);

            addToCollectionButton.setText("Войдите для добавления");
            removeFromCollectionButton.setText("Войдите для удаления");
            updateRatingInCollectionButton.setText("Войдите для оценки");

            collectionData.clear();
            recommendationsData.clear();
            loadGuestRecommendations();

        } else {
            // Режим зарегистрированного пользователя
            userInfoLabel.setText(currentUser.getUsername());
            addToCollectionButton.setDisable(false);
            removeFromCollectionButton.setDisable(false);
            updateRatingInCollectionButton.setDisable(false);
            ratingSlider.setDisable(false);
            ratingSliderCollection.setDisable(false);
            refreshRecommendationsButton.setDisable(false);
            markAsViewedButton.setDisable(false);
            addRecommendedToCollectionButton.setDisable(false);

            addToCollectionButton.setText("Добавить в коллекцию с выбранной оценкой");
            removeFromCollectionButton.setText("🗑️ Удалить из коллекции");
            updateRatingInCollectionButton.setText("Обновить рейтинг");

            loadUserCollection();
            loadUserRecommendations();
        }
    }

    private void updateTrackCount() {
        int totalTracks = tracksData.size();
        int dbTracks = (int) tracksData.stream().filter(t -> t.getId() > 0).count();
        int apiTracks = (int) tracksData.stream().filter(t -> t.getId() <= 0).count();

        logger.info("Total tracks: {} (DB: {}, API: {})", totalTracks, dbTracks, apiTracks);
    }

    private void onCollectionTrackSelected(Track track) {
        if (track != null && currentUser != null) {
            // Устанавливаем слайдер рейтинга на значение из коллекции
            if (track.getRating() != null) {
                ratingSliderCollection.setValue(track.getRating());
                ratingLabelCollection.setText(String.valueOf(track.getRating()));
            } else {
                // Если рейтинга нет, ставим среднее значение
                ratingSliderCollection.setValue(3);
                ratingLabelCollection.setText("3");
            }
        }
    }

    private void setupTracksTable() {
        trackTitleColumn.setCellValueFactory(new PropertyValueFactory<>("title"));
        trackArtistColumn.setCellValueFactory(new PropertyValueFactory<>("artistName"));
        trackAlbumColumn.setCellValueFactory(new PropertyValueFactory<>("albumTitle"));
        trackDurationColumn.setCellValueFactory(new PropertyValueFactory<>("duration"));
        tracksTable.setItems(tracksData);
    }

    private void setupCollectionTable() {
        if (collectionTable.getColumns().size() >= 3) {
            TableColumn<Track, String> colTitle = (TableColumn<Track, String>) collectionTable.getColumns().get(0);
            TableColumn<Track, String> colArtist = (TableColumn<Track, String>) collectionTable.getColumns().get(1);
            TableColumn<Track, Integer> colRating = (TableColumn<Track, Integer>) collectionTable.getColumns().get(2);

            colTitle.setCellValueFactory(new PropertyValueFactory<>("title"));
            colArtist.setCellValueFactory(new PropertyValueFactory<>("artistName"));
            colRating.setCellValueFactory(new PropertyValueFactory<>("rating"));

            // Кастомный рендерер для рейтинга
            colRating.setCellFactory(column -> new TableCell<Track, Integer>() {
                @Override
                protected void updateItem(Integer rating, boolean empty) {
                    super.updateItem(rating, empty);
                    if (empty || rating == null) {
                        setText(null);
                        setStyle("");
                    } else {
                        setText("★ ".repeat(rating) + "☆ ".repeat(5 - rating));
                        // Цветовая индикация в зависимости от рейтинга
                        if (rating >= 4) {
                            setStyle("-fx-text-fill: #FFD700; -fx-font-weight: bold;");
                        } else if (rating >= 3) {
                            setStyle("-fx-text-fill: #C0C0C0;");
                        } else {
                            setStyle("-fx-text-fill: #CD7F32;");
                        }
                    }
                }
            });
        }
        collectionTable.setItems(collectionData);
    }

    @FXML
    private void handleSearch() {
        String query = searchField.getText();

        if (query == null || query.trim().isEmpty()) {
            loadAllTracks();
            return;
        }

        logger.info("Starting quick search for: '{}'", query);

        try {
            tracksTable.setPlaceholder(new Label("Поиск..."));
            List<Track> searchResults = searchService.quickSearch(query);
            tracksData.setAll(searchResults);

            logger.info("Quick search completed, found {} tracks for: '{}'", searchResults.size(), query);

            if (searchResults.isEmpty()) {
                tracksTable.setPlaceholder(new Label("Ничего не найдено для: " + query));
            }

        } catch (Exception e) {
            logger.error("Error during quick search for: '{}'", query, e);
            tracksTable.setPlaceholder(new Label("Ошибка при поиске"));
            showAlert(Alert.AlertType.ERROR, "Ошибка поиска", "Произошла ошибка при поиске: " + e.getMessage());
        }
    }

    @FXML
    private void handleAddToCollection() {
        if (currentUser == null) {
            showAlert(Alert.AlertType.WARNING, "Внимание", "Эта функция доступна только для зарегистрированных пользователей");
            return;
        }

        Track selectedTrack = tracksTable.getSelectionModel().getSelectedItem();
        if (selectedTrack == null) {
            showAlert(Alert.AlertType.WARNING, "Внимание", "Выберите трек для добавления в коллекцию");
            return;
        }

        try {

            int rating = (int) ratingSlider.getValue();


            MusicCollectionService.AddTrackResult result =
                    collectionService.addTrackToCollection(currentUser.getId(), selectedTrack, rating);

            if (result.isSuccess()) {
                showAlert(Alert.AlertType.INFORMATION, "Успех", result.getMessage());
                loadUserCollection(); // Обновляем коллекцию
                ratingSlider.setValue(3);
            } else {
                showAlert(Alert.AlertType.WARNING, "Внимание", result.getMessage());
            }
        } catch (Exception e) {
            logger.error("Error adding track to collection", e);
            showAlert(Alert.AlertType.ERROR, "Ошибка", "Произошла ошибка при добавлении трека");
        }
    }

    @FXML
    private void handleUpdateRatingInCollection() {
        if (currentUser == null) {
            showAlert(Alert.AlertType.WARNING, "Внимание", "Эта функция доступна только для зарегистрированных пользователей");
            return;
        }

        Track selectedTrack = collectionTable.getSelectionModel().getSelectedItem();
        if (selectedTrack == null) {
            showAlert(Alert.AlertType.WARNING, "Внимание", "Выберите трек из коллекции для обновления рейтинга");
            return;
        }

        try {
            int rating = (int) ratingSliderCollection.getValue();
            boolean success = collectionService.updateRating(currentUser.getId(), selectedTrack.getId(), rating);
            if (success) {
                showAlert(Alert.AlertType.INFORMATION, "Успех",
                        "Рейтинг трека '" + selectedTrack.getTitle() + "' обновлен на " + rating);
                loadUserCollection(); // Обновляем коллекцию
            } else {
                showAlert(Alert.AlertType.ERROR, "Ошибка", "Не удалось обновить рейтинг");
            }
        } catch (Exception e) {
            logger.error("Error updating rating in collection", e);
            showAlert(Alert.AlertType.ERROR, "Ошибка", "Произошла ошибка при обновлении рейтинга");
        }
    }
    @FXML
    private void handleRemoveFromCollection() {
        if (currentUser == null) {
            showAlert(Alert.AlertType.WARNING, "Внимание", "Эта функция доступна только для зарегистрированных пользователей");
            return;
        }

        Track selectedTrack = collectionTable.getSelectionModel().getSelectedItem();
        if (selectedTrack == null) {
            showAlert(Alert.AlertType.WARNING, "Внимание", "Выберите трек из коллекции для удаления");
            return;
        }

        try {
            boolean success = collectionService.removeFromCollection(currentUser.getId(), selectedTrack.getId());
            if (success) {
                showAlert(Alert.AlertType.INFORMATION, "Успех", "Трек удален из коллекции");
                loadUserCollection(); // Обновляем коллекцию
            } else {
                showAlert(Alert.AlertType.ERROR, "Ошибка", "Не удалось удалить трек из коллекции");
            }
        } catch (Exception e) {
            logger.error("Error removing track from collection", e);
            showAlert(Alert.AlertType.ERROR, "Ошибка", "Произошла ошибка при удалении трека");
        }
    }

    @FXML
    private void handleUpdateRating() {
        if (currentUser == null) {
            showAlert(Alert.AlertType.WARNING, "Внимание", "Эта функция доступна только для зарегистрированных пользователей");
            return;
        }

        Track selectedTrack = collectionTable.getSelectionModel().getSelectedItem();
        if (selectedTrack == null) {
            showAlert(Alert.AlertType.WARNING, "Внимание", "Выберите трек из коллекции для обновления рейтинга");
            return;
        }

        try {
            int rating = (int) ratingSlider.getValue();
            boolean success = collectionService.updateRating(currentUser.getId(), selectedTrack.getId(), rating);
            if (success) {
                showAlert(Alert.AlertType.INFORMATION, "Успех", "Рейтинг обновлен");
                loadUserCollection(); // Обновляем коллекцию
            } else {
                showAlert(Alert.AlertType.ERROR, "Ошибка", "Не удалось обновить рейтинг");
            }
        } catch (Exception e) {
            logger.error("Error updating rating", e);
            showAlert(Alert.AlertType.ERROR, "Ошибка", "Произошла ошибка при обновлении рейтинга");
        }
    }

    @FXML
    private void handleToggleFavorite() {
        if (currentUser == null) {
            showAlert(Alert.AlertType.WARNING, "Внимание", "Эта функция доступна только для зарегистрированных пользователей");
            return;
        }

        Track selectedTrack = collectionTable.getSelectionModel().getSelectedItem();
        if (selectedTrack == null) {
            showAlert(Alert.AlertType.WARNING, "Внимание", "Выберите трек из коллекции");
            return;
        }

        try {
            boolean success = collectionService.toggleFavorite(currentUser.getId(), selectedTrack.getId());
            if (success) {
                String message = selectedTrack.getFavorite() ? "Трек убран из избранного" : "Трек добавлен в избранное";
                showAlert(Alert.AlertType.INFORMATION, "Успех", message);
                loadUserCollection(); // Обновляем коллекцию
            } else {
                showAlert(Alert.AlertType.ERROR, "Ошибка", "Не удалось обновить статус избранного");
            }
        } catch (Exception e) {
            logger.error("Error toggling favorite", e);
            showAlert(Alert.AlertType.ERROR, "Ошибка", "Произошла ошибка при обновлении статуса");
        }
    }

    @FXML
    private void handleRefreshRecommendations() {
        if (currentUser == null) {
            showAlert(Alert.AlertType.WARNING, "Внимание", "Эта функция доступна только для зарегистрированных пользователей");
            return;
        }

        try {
            // Показываем индикатор загрузки
            recommendationsTable.setPlaceholder(new Label("Генерация новых рекомендаций..."));

            // 🔥 ИСПРАВЛЕНИЕ: Используем новый метод принудительной генерации
            new Thread(() -> {
                try {
                    List<Recommendation> newRecommendations =
                            recommendationService.generateNewRecommendations(currentUser.getId());

                    // Обновляем UI в главном потоке
                    javafx.application.Platform.runLater(() -> {
                        recommendationsData.setAll(newRecommendations);

                        if (newRecommendations.isEmpty()) {
                            recommendationsTable.setPlaceholder(new Label("Не удалось сгенерировать рекомендации. Добавьте больше треков в коллекцию."));
                        } else {
                            recommendationsTable.setPlaceholder(null);
                            showAlert(Alert.AlertType.INFORMATION, "Успех",
                                    "Сгенерировано " + newRecommendations.size() + " новых рекомендаций!");
                        }
                    });

                } catch (Exception e) {
                    logger.error("Error generating new recommendations", e);
                    javafx.application.Platform.runLater(() -> {
                        showAlert(Alert.AlertType.ERROR, "Ошибка", "Не удалось сгенерировать новые рекомендации");
                        recommendationsTable.setPlaceholder(new Label("Ошибка генерации рекомендаций"));
                    });
                }
            }).start();

        } catch (Exception e) {
            logger.error("Error refreshing recommendations", e);
            showAlert(Alert.AlertType.ERROR, "Ошибка", "Не удалось обновить рекомендации");
        }
    }

    @FXML
    private void handleMarkAsViewed() {
        if (currentUser == null) return;

        Recommendation selectedRec = recommendationsTable.getSelectionModel().getSelectedItem();
        if (selectedRec == null) {
            showAlert(Alert.AlertType.WARNING, "Внимание", "Выберите рекомендацию для отметки");
            return;
        }

        try {
            boolean success = recommendationService.markRecommendationAsViewed(selectedRec.getId());
            if (success) {
                loadUserRecommendations(); // Обновляем список
                showAlert(Alert.AlertType.INFORMATION, "Успех", "Рекомендация отмечена как просмотренная");
            }
        } catch (Exception e) {
            logger.error("Error marking recommendation as viewed", e);
            showAlert(Alert.AlertType.ERROR, "Ошибка", "Ошибка при отметке рекомендации");
        }
    }

    @FXML
    private void handleAddRecommendedToCollection() {
        if (currentUser == null) {
            showAlert(Alert.AlertType.WARNING, "Внимание", "Эта функция доступна только для зарегистрированных пользователей");
            return;
        }

        Recommendation selectedRec = recommendationsTable.getSelectionModel().getSelectedItem();
        if (selectedRec == null) {
            showAlert(Alert.AlertType.WARNING, "Внимание", "Выберите рекомендацию для добавления");
            return;
        }

        try {
            // Получаем полную информацию о треке
            Track recommendedTrack = trackService.getTrackById(selectedRec.getTrackId());
            if (recommendedTrack == null) {
                showAlert(Alert.AlertType.ERROR, "Ошибка", "Трек не найден");
                return;
            }

            MusicCollectionService.AddTrackResult result =
                    collectionService.addTrackToCollection(currentUser.getId(), recommendedTrack,3);

            if (result.isSuccess()) {
                // Помечаем рекомендацию как просмотренную
                recommendationService.markRecommendationAsViewed(selectedRec.getId());

                showAlert(Alert.AlertType.INFORMATION, "Успех",
                        "Трек '" + recommendedTrack.getTitle() + "' добавлен в коллекцию!");

                // Обновляем коллекцию и рекомендации
                loadUserCollection();
                loadUserRecommendations();

            } else {
                showAlert(Alert.AlertType.WARNING, "Внимание", result.getMessage());
            }

        } catch (Exception e) {
            logger.error("Error adding recommended track to collection", e);
            showAlert(Alert.AlertType.ERROR, "Ошибка", "Произошла ошибка при добавлении трека");
        }
    }

    private void onRecommendationSelected(Recommendation recommendation) {
        if (recommendation != null) {
            // Включаем кнопки при выборе рекомендации
            markAsViewedButton.setDisable(false);
            addRecommendedToCollectionButton.setDisable(false);
        } else {
            markAsViewedButton.setDisable(true);
            addRecommendedToCollectionButton.setDisable(true);
        }
    }

    @FXML
    private void handleLogout() {
        logger.info("User logging out");
        if (mainApp != null) {
            mainApp.showLoginScreen();
        }
    }

    private void loadAllTracks() {
        try {
            tracksTable.setPlaceholder(new Label("Загрузка треков..."));
            List<Track> tracks = trackService.getAllTracks();
            tracksData.setAll(tracks);
            logger.info("Successfully loaded {} tracks", tracks.size());
            updateTrackCount();
        } catch (Exception e) {
            logger.error("Error loading tracks", e);
            tracksTable.setPlaceholder(new Label("Ошибка загрузки треков"));
            showAlert(Alert.AlertType.ERROR, "Ошибка", "Не удалось загрузить треки: " + e.getMessage());
        }
    }

    private void loadUserCollection() {
        if (currentUser != null) {
            try {
                logger.info("Loading collection for user: {}", currentUser.getUsername());

                List<Track> userTracks = collectionService.getUserCollection(currentUser.getId());
                collectionData.setAll(userTracks);

                logger.info("Successfully loaded {} tracks to collection table", userTracks.size());

                // Обновляем таблицу
                collectionTable.refresh();

            } catch (Exception e) {
                logger.error("Error loading user collection", e);
                collectionData.clear();
                showAlert(Alert.AlertType.ERROR, "Ошибка", "Не удалось загрузить коллекцию");
            }
        }
    }

    private void loadUserRecommendations() {
        if (currentUser != null) {
            try {
                logger.info("Loading recommendations for user: {}", currentUser.getUsername());

                List<Recommendation> userRecommendations =
                        recommendationService.generateNewRecommendations(currentUser.getId());

                recommendationsData.setAll(userRecommendations);

                logger.info("Successfully loaded {} recommendations", userRecommendations.size());

                if (userRecommendations.isEmpty()) {
                    recommendationsTable.setPlaceholder(new Label("Нет новых рекомендаций. Обновите список."));
                }

            } catch (Exception e) {
                logger.error("Error loading user recommendations", e);
                recommendationsData.clear();
                recommendationsTable.setPlaceholder(new Label("Ошибка загрузки рекомендаций"));
            }
        }
    }

    private void loadGuestRecommendations() {
        recommendationsData.clear();
        recommendationsTable.setPlaceholder(new Label("Рекомендации появятся после регистрации"));
    }

    @FXML
    private void handleAdvancedSearch() {
        try {
            SearchCriteria criteria = new SearchCriteria();

            // Основной поисковый запрос
            criteria.setQuery(searchField.getText());

            // Фильтр по артисту
            if (!artistFilterField.getText().isEmpty()) {
                criteria.setArtist(artistFilterField.getText());
            }

            // Фильтр по альбому
            if (!albumFilterField.getText().isEmpty()) {
                criteria.setAlbum(albumFilterField.getText());
            }

            // Фильтр по жанру
            if (!genreFilterField.getText().isEmpty()) {
                criteria.setGenre(genreFilterField.getText());
            }

            // Выполняем поиск треков
            List<Track> results = searchService.searchTracks(criteria);
            tracksData.setAll(results);

            if (results.isEmpty()) {
                tracksTable.setPlaceholder(new Label("Ничего не найдено"));
            } else {
                tracksTable.setPlaceholder(null);
            }

            logger.info("Advanced search completed. Found {} tracks", results.size());

        } catch (Exception e) {
            logger.error("Error during advanced search", e);
            showAlert(Alert.AlertType.ERROR, "Ошибка", "Ошибка при расширенном поиске: " + e.getMessage());
        }
    }


    private void setupRecommendationsTable() {
        recTrackColumn.setCellValueFactory(new PropertyValueFactory<>("trackTitle"));
        recArtistColumn.setCellValueFactory(new PropertyValueFactory<>("artistName"));
        recTypeColumn.setCellValueFactory(new PropertyValueFactory<>("recommendationType"));
        recScoreColumn.setCellValueFactory(new PropertyValueFactory<>("score"));

        // Кастомный рендерер для колонки с оценкой
        recScoreColumn.setCellFactory(column -> new TableCell<Recommendation, Double>() {
            @Override
            protected void updateItem(Double score, boolean empty) {
                super.updateItem(score, empty);
                if (empty || score == null) {
                    setText(null);
                    setStyle("");
                } else {
                    setText(String.format("%.0f%%", score * 100));
                    // Цветовая индикация в зависимости от уверенности
                    if (score >= 0.8) {
                        setStyle("-fx-text-fill: #2E7D32; -fx-font-weight: bold;");
                    } else if (score >= 0.6) {
                        setStyle("-fx-text-fill: #F9A825;");
                    } else {
                        setStyle("-fx-text-fill: #C62828;");
                    }
                }
            }
        });

        // Кастомный рендерер для типа рекомендации
        recTypeColumn.setCellFactory(column -> new TableCell<Recommendation, String>() {
            @Override
            protected void updateItem(String type, boolean empty) {
                super.updateItem(type, empty);
                if (empty || type == null) {
                    setText(null);
                    setStyle("");
                } else {
                    switch (type) {
                        case "content_based":
                            setText("🎵 Похожие треки");
                            break;
                        case "lastfm_similar":
                            setText("🔍 Last.fm рекомендации");
                            break;
                        case "popular":
                            setText("🔥 Популярное");
                            break;
                        default:
                            setText(type);
                    }
                }
            }
        });

        recommendationsTable.setItems(recommendationsData);
    }

    private void showTrackDetails(Track track) {
        if (track != null) {
            trackInfoTitle.setText(track.getTitle());
            trackInfoArtist.setText(track.getArtistName() != null ? track.getArtistName() : "Неизвестен");
            trackInfoAlbum.setText(track.getAlbumTitle() != null ? track.getAlbumTitle() : "Неизвестен");
            trackInfoDuration.setText(track.getDuration() != null ? formatDuration(track.getDuration()) : "Неизвестна");
            trackInfoGenre.setText(track.getGenre() != null ? track.getGenre() : "Неизвестен");

            // ✅ Пытаемся загрузить обложку альбома
            loadAlbumCover(track.getArtistName(), track.getAlbumTitle());
        } else {
            clearTrackDetails();
        }
    }

    // ✅ Исправленный метод для загрузки обложки альбома
    private void loadAlbumCover(String artist, String albumTitle) {
        if (artist == null || albumTitle == null ||
                artist.isEmpty() || albumTitle.isEmpty() ||
                albumTitle.startsWith("[АЛЬБОМ]") || albumTitle.startsWith("[ИСПОЛНИТЕЛЬ]")) {

            // Устанавливаем placeholder (если файл существует)
            try {
                Image placeholder = new Image(getClass().getResourceAsStream("/images/placeholder_album.png"));
                albumCoverImage.setImage(placeholder);
            } catch (Exception e) {
                // Если файла нет, создаем пустое изображение
                albumCoverImage.setImage(null);
            }
            albumInfoLabel.setText("Обложка не доступна");
            return;
        }

        try {
            // Используем Last.fm для получения обложки
            LastFmService.AlbumInfo albumInfo = lastFmService.getAlbumInfo(artist, albumTitle);

            if (albumInfo != null && albumInfo.getCoverUrl() != null && !albumInfo.getCoverUrl().isEmpty()) {
                // Загружаем обложку из URL
                Image image = new Image(albumInfo.getCoverUrl(), true); // true для асинхронной загрузки

                // ✅ Устанавливаем изображение правильно
                albumCoverImage.setImage(image);
                albumInfoLabel.setText(albumTitle + " - " + artist);

                logger.info("Album cover loaded: {} - {}", artist, albumTitle);
            } else {
                // Обложка не найдена
                try {
                    Image placeholder = new Image(getClass().getResourceAsStream("/images/placeholder_album.png"));
                    albumCoverImage.setImage(placeholder);
                } catch (Exception e) {
                    albumCoverImage.setImage(null);
                }
                albumInfoLabel.setText("Обложка не найдена");
            }
        } catch (Exception e) {
            logger.error("Error loading album cover for: {} - {}", artist, albumTitle, e);
            try {
                Image placeholder = new Image(getClass().getResourceAsStream("/images/placeholder_album.png"));
                albumCoverImage.setImage(placeholder);
            } catch (Exception ex) {
                albumCoverImage.setImage(null);
            }
            albumInfoLabel.setText("Ошибка загрузки");
        }
    }

    private void clearTrackDetails() {
        trackInfoTitle.setText("Выберите трек");
        trackInfoArtist.setText("");
        trackInfoAlbum.setText("");
        trackInfoDuration.setText("");
        trackInfoGenre.setText("");
        //albumCoverImage.setImage(new Image("/images/placeholder_album.png"));
        albumInfoLabel.setText("Выберите трек для просмотра обложки");
    }

    private String formatDuration(Integer seconds) {
        if (seconds == null) return "0:00";
        int minutes = seconds / 60;
        int remainingSeconds = seconds % 60;
        return String.format("%d:%02d", minutes, remainingSeconds);
    }

    private void showAlert(Alert.AlertType alertType, String title, String message) {
        Alert alert = new Alert(alertType);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}