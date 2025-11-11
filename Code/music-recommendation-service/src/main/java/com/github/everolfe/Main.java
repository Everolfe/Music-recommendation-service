package com.github.everolfe;

import com.github.everolfe.controller.LoginController;
import com.github.everolfe.controller.MainController;
import com.github.everolfe.database.DatabaseInitializer;
import com.github.everolfe.model.User;
import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.stage.Stage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Main extends Application {
    private static final Logger logger = LoggerFactory.getLogger(Main.class);
    private Stage primaryStage;
    private User currentUser;

    @Override
    public void init() {
        logger.info("Инициализация приложения...");
        // Инициализируем базу данных
        try {
            //DatabaseInitializer.initialize();
            logger.info("База данных инициализирована");
        } catch (Exception e) {
            logger.error("Ошибка инициализации БД: {}", e.getMessage());
        }
    }

    @Override
    public void start(Stage primaryStage) {
        try {
            this.primaryStage = primaryStage;
            logger.info("Запуск Music Recommendation Service");

            // Загружаем экран входа
            showLoginScreen();

            primaryStage.setTitle("Music Recommendation Service");
            primaryStage.setMinWidth(1000);
            primaryStage.setMinHeight(700);
            primaryStage.show();

            logger.info("Приложение успешно запущено");

        } catch (Exception e) {
            logger.error("Критическая ошибка при запуске", e);
            showErrorAlert("Критическая ошибка: " + e.getMessage());
        }
    }

    public void showLoginScreen() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/login.fxml"));
            Parent root = loader.load();

            // Получаем контроллер и устанавливаем ссылку на Main
            LoginController loginController = loader.getController();
            loginController.setMainApp(this);

            Scene scene = new Scene(root, 600, 500);
            primaryStage.setScene(scene);
            primaryStage.centerOnScreen();

            // Сбрасываем текущего пользователя
            this.currentUser = null;

        } catch (Exception e) {
            logger.error("Ошибка при загрузке экрана входа", e);
            showErrorAlert("Ошибка загрузки интерфейса: " + e.getMessage());
        }
    }

    public void showMainScreen(User user) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/main.fxml"));
            Parent root = loader.load();

            // Получаем контроллер и устанавливаем ссылку на Main
            MainController mainController = loader.getController();
            mainController.setMainApp(this);
            mainController.setCurrentUser(user); // Передаем пользователя

            Scene scene = new Scene(root, 1200, 800);
            primaryStage.setScene(scene);

            // Сохраняем текущего пользователя
            this.currentUser = user;

            logger.info("Главный экран показан для пользователя: {}",
                    user != null ? user.getUsername() : "Гость");

        } catch (Exception e) {
            logger.error("Ошибка при загрузке главного экрана", e);
            showErrorAlert("Ошибка загрузки интерфейса: " + e.getMessage());
        }
    }

    // Метод для гостевого входа
    public void showMainScreenAsGuest() {
        showMainScreen(null);
    }

    // Геттеры для состояния пользователя
    public User getCurrentUser() {
        return currentUser;
    }

    public boolean isUserLoggedIn() {
        return currentUser != null;
    }

    public boolean isGuestMode() {
        return currentUser == null;
    }

    private void showErrorAlert(String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle("Ошибка");
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    public static void main(String[] args) {
        try {
            launch(args);
        } catch (Exception e) {
            logger.error("Необработанное исключение в main", e);
            System.err.println("Критическая ошибка: " + e.getMessage());
        }
    }
}