package com.github.everolfe.controller;

import com.github.everolfe.Main;
import com.github.everolfe.model.User;
import com.github.everolfe.service.UserService;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Optional;

public class LoginController {
    private static final Logger logger = LoggerFactory.getLogger(LoginController.class);

    private UserService userService;
    private Main mainApp;

    @FXML private TextField usernameField;
    @FXML private PasswordField passwordField;
    @FXML private TextField registerUsernameField;
    @FXML private TextField registerEmailField;
    @FXML private PasswordField registerPasswordField;

    public LoginController() {
        this.userService = new UserService();
    }

    public void setMainApp(Main mainApp) {
        this.mainApp = mainApp;
    }

    @FXML
    private void initialize() {
        logger.info("LoginController initialized");
    }

    @FXML
    private void handleLogin() {
        String username = usernameField.getText();
        String password = passwordField.getText();

        if (username.isEmpty() || password.isEmpty()) {
            showAlert(Alert.AlertType.WARNING, "Ошибка входа", "Заполните все поля");
            return;
        }

        try {
            Optional<User> user = userService.login(username, password);
            if (user.isPresent()) {
                logger.info("User successfully logged in: {}", username);
                mainApp.showMainScreen(user.get()); // Передаем пользователя
                clearLoginFields();
            } else {
                showAlert(Alert.AlertType.ERROR, "Ошибка входа", "Неверное имя пользователя или пароль");
            }
        } catch (Exception e) {
            logger.error("Login error", e);
            showAlert(Alert.AlertType.ERROR, "Ошибка", "Произошла ошибка при входе");
        }
    }

    @FXML
    private void handleRegister() {
        String username = registerUsernameField.getText();
        String email = registerEmailField.getText();
        String password = registerPasswordField.getText();

        if (username.isEmpty() || email.isEmpty() || password.isEmpty()) {
            showAlert(Alert.AlertType.WARNING, "Ошибка регистрации", "Заполните все поля");
            return;
        }

        try {
            boolean success = userService.register(username, email, password);
            if (success) {
                showAlert(Alert.AlertType.INFORMATION, "Успех", "Регистрация прошла успешно!");

                // Автоматически входим после регистрации
                Optional<User> user = userService.login(username, password);
                if (user.isPresent()) {
                    mainApp.showMainScreen(user.get());
                }

                clearRegisterFields();
            } else {
                showAlert(Alert.AlertType.ERROR, "Ошибка регистрации", "Пользователь с таким именем уже существует");
            }
        } catch (Exception e) {
            logger.error("Registration error", e);
            showAlert(Alert.AlertType.ERROR, "Ошибка", "Произошла ошибка при регистрации");
        }
    }

    @FXML
    private void handleGuestLogin() {
        logger.info("Guest user entered");
        mainApp.showMainScreenAsGuest(); // Гостевой вход
    }

    private void clearLoginFields() {
        usernameField.clear();
        passwordField.clear();
    }

    private void clearRegisterFields() {
        registerUsernameField.clear();
        registerEmailField.clear();
        registerPasswordField.clear();
    }

    private void showAlert(Alert.AlertType alertType, String title, String message) {
        Alert alert = new Alert(alertType);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}