package com.example.knowledgeforge.support;

import com.example.knowledgeforge.config.Schema;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;

/**
 * Izolacja testów integracyjnych od roboczej bazy (pgDB) — WSZYSTKIE tabele testowe żyją
 * w osobnym schemacie Postgresa ("kf_test", ustawionym jako search_path przez parametr JDBC
 * currentSchema), zamiast w "public", gdzie leżą prawdziwe dane deweloperskie aplikacji.
 * Testy nigdy nie czytają ani nie modyfikują niczego w schemacie "public".
 *
 * (Osobna, fizyczna baza danych — CREATE DATABASE — okazała się w tym środowisku Dockera
 * niedostępna: katalog danych kontenera jest bind-mountowany z Windows przez Docker Desktop,
 * a CREATE DATABASE cichnie nie potrafi sklonować katalogu template1 na tym wolumenie
 * ("database subdirectory ... is missing" mimo że wpis w pg_database istnieje). CREATE SCHEMA
 * nie robi kopiowania na poziomie plików, więc nie ma tego problemu — i tak samo dobrze
 * izoluje tabele testowe od tabel roboczych.)
 */
public final class TestDatabase {

    private static final String BASE_URL = "jdbc:postgresql://localhost:5432/pgDB";
    private static final String SCHEMA = "kf_test";
    private static final String USER = "user";
    private static final String PASSWORD = "password";

    private TestDatabase() {
    }

    public static HikariDataSource create() {
        ensureSchemaExists();

        HikariConfig hc = new HikariConfig();
        hc.setJdbcUrl(BASE_URL + "?currentSchema=" + SCHEMA);
        hc.setUsername(USER);
        hc.setPassword(PASSWORD);
        hc.setDriverClassName("org.postgresql.Driver");
        hc.setMaximumPoolSize(4);
        hc.setMinimumIdle(1);
        hc.setPoolName("kf-test-pool");
        hc.setAllowPoolSuspension(true);
        HikariDataSource ds = new HikariDataSource(hc);
        Schema.ensure(ds);
        return ds;
    }

    private static void ensureSchemaExists() {
        try (Connection c = DriverManager.getConnection(BASE_URL, USER, PASSWORD);
             Statement st = c.createStatement()) {
            st.execute("CREATE SCHEMA IF NOT EXISTS " + SCHEMA);
        } catch (SQLException e) {
            throw new IllegalStateException("Failed to prepare test schema '" + SCHEMA + "' on " + BASE_URL, e);
        }
    }
}
