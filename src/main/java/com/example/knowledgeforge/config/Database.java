package com.example.knowledgeforge.config;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;

/** Tworzy pulę połączeń JDBC (HikariCP) — jedyna "magia" to pooling, bez ORM. */
public final class Database {

    private Database() {
    }

    public static HikariDataSource createDataSource(AppConfig config) {
        HikariConfig hc = new HikariConfig();
        hc.setJdbcUrl(config.dbUrl());
        hc.setUsername(config.dbUsername());
        hc.setPassword(config.dbPassword());
        hc.setDriverClassName("org.postgresql.Driver");
        hc.setMaximumPoolSize(10);
        hc.setMinimumIdle(2);
        hc.setPoolName("knowledge-forge-pool");
        // Pozwala DatabaseRestoreService bezpiecznie zawiesić/wznowić pulę na czas pg_restore
        // (żeby idle-połączenia z puli nie trzymały blokad, których potrzebuje --clean).
        hc.setAllowPoolSuspension(true);
        return new HikariDataSource(hc);
    }
}
