package com.github.everolfe.service;

import com.github.everolfe.database.dao.UserDAO;
import com.github.everolfe.model.User;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Optional;

public class UserService {
    private static final Logger logger = LoggerFactory.getLogger(UserService.class);
    private final UserDAO userDAO;

    public UserService() {
        this.userDAO = new UserDAO();
    }

    public Optional<User> login(String username, String password) {
        try {
            Optional<User> user = userDAO.findByUsername(username);
            if (user.isPresent()) {
                // В реальном приложении здесь должно быть хеширование пароля
                if (user.get().getPasswordHash() != null &&
                        user.get().getPasswordHash().equals(password)) {
                    logger.info("User logged in: {}", username);
                    return user;
                }
            }
        } catch (Exception e) {
            logger.error("Error during login for user: {}", username, e);
        }
        return Optional.empty();
    }

    public boolean register(String username, String email, String password) {
        try {
            // Проверяем, не существует ли уже пользователь
            if (userDAO.findByUsername(username).isPresent()) {
                logger.warn("User already exists: {}", username);
                return false;
            }

            User user = new User(username, email);
            user.setPasswordHash(password); // В реальном приложении нужно хешировать

            boolean success = userDAO.save(user);
            if (success) {
                logger.info("User registered successfully: {}", username);
            }
            return success;
        } catch (Exception e) {
            logger.error("Error during registration for user: {}", username, e);
            return false;
        }
    }

    public Optional<User> getUserById(Long id) {
        return userDAO.findById(id);
    }

    public Optional<User> getUserByUsername(String username) {
        return userDAO.findByUsername(username);
    }

    public List<User> getAllUsers() {
        return userDAO.findAll();
    }

    public boolean updateUser(User user) {
        return userDAO.save(user);
    }

    public boolean deleteUser(Long id) {
        return userDAO.delete(id);
    }
}