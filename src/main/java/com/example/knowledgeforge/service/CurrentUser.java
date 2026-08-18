package com.example.knowledgeforge.service;

import com.example.knowledgeforge.dao.UserDao;

/**
 * Aplikacja działa bez logowania — wszystkie dane są przypisywane do
 * jednego domyślnego użytkownika, zakładanego przy starcie przez
 * {@link com.example.knowledgeforge.config.DefaultUserSeeder}. Wynik
 * jest buforowany po pierwszym odczycie — id się nie zmienia.
 */
public class CurrentUser {

    private final UserDao userDao;
    private final String defaultUsername;
    private volatile Long cachedId;

    public CurrentUser(UserDao userDao, String defaultUsername) {
        this.userDao = userDao;
        this.defaultUsername = defaultUsername;
    }

    public Long id() {
        Long id = cachedId;
        if (id != null) return id;
        synchronized (this) {
            if (cachedId == null) {
                cachedId = userDao.findByUsername(defaultUsername)
                        .orElseThrow(() -> new IllegalStateException(
                                "Default user '" + defaultUsername + "' not found — DefaultUserSeeder should create it at startup"))
                        .getId();
            }
            return cachedId;
        }
    }
}
