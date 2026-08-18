package com.example.knowledgeforge.config;

import com.example.knowledgeforge.dao.UserDao;
import com.example.knowledgeforge.domain.user.User;

import java.util.logging.Logger;

/**
 * Aplikacja działa bez logowania — na starcie zakłada jednego domyślnego
 * użytkownika, jeśli jeszcze nie istnieje. Wszystkie dane są przypisywane
 * do niego (zob. service.CurrentUser).
 */
public final class DefaultUserSeeder {

    private static final Logger log = Logger.getLogger(DefaultUserSeeder.class.getName());

    private DefaultUserSeeder() {
    }

    public static void ensure(UserDao userDao, AppConfig config) {
        String username = config.defaultUsername();
        if (userDao.findByUsername(username).isPresent()) {
            log.info("Default user '" + username + "' already exists — skipping seed");
            return;
        }
        User user = new User();
        user.setUsername(username);
        user.setEmail(config.defaultUserEmail());
        user.setActive(true);
        userDao.insert(user);
        log.info("Default user '" + username + "' created successfully");
    }
}
