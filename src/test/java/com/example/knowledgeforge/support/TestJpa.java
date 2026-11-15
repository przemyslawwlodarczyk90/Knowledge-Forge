package com.example.knowledgeforge.support;

import com.example.knowledgeforge.config.SchemaInitializer;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.WebApplicationType;
import org.springframework.boot.autoconfigure.ImportAutoConfiguration;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.boot.autoconfigure.jdbc.JdbcTemplateAutoConfiguration;
import org.springframework.boot.autoconfigure.orm.jpa.HibernateJpaAutoConfiguration;
import org.springframework.boot.autoconfigure.transaction.TransactionAutoConfiguration;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.Import;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.PlatformTransactionManager;

/**
 * Jeden współdzielony (na cały przebieg testów) minimalny kontekst Springa: DataSource + JPA +
 * repozytoria Spring Data na schemacie testowym kf_test (zob. TestDatabase) — bez web, security,
 * schedulerów. Serwisy testy składają ręcznie (zob. ServiceHarness), tak jak przed migracją.
 */
public final class TestJpa {

    private static volatile ConfigurableApplicationContext context;

    private TestJpa() {
    }

    @SpringBootConfiguration
    @ImportAutoConfiguration({DataSourceAutoConfiguration.class, JdbcTemplateAutoConfiguration.class, HibernateJpaAutoConfiguration.class,
            TransactionAutoConfiguration.class})
    @EntityScan("com.example.knowledgeforge.domain")
    @EnableJpaRepositories("com.example.knowledgeforge.repository")
    @Import(SchemaInitializer.class)
    static class Config {
    }

    public static ConfigurableApplicationContext context() {
        ConfigurableApplicationContext ctx = context;
        if (ctx == null) {
            synchronized (TestJpa.class) {
                if (context == null) {
                    TestDatabase.ensureSchemaExists();
                    // Argumenty linii poleceń mają NAJWYŻSZY priorytet — nadpisują application.properties
                    // (SpringApplicationBuilder#properties to tylko wartości domyślne i przegrywa z nim,
                    // przez co testy trafiłyby do roboczej bazy).
                    ConfigurableApplicationContext started = new SpringApplicationBuilder(Config.class)
                            .web(WebApplicationType.NONE)
                            .run("--spring.main.banner-mode=off",
                                    "--db.url=" + TestDatabase.URL,
                                    "--db.username=" + TestDatabase.USER,
                                    "--db.password=" + TestDatabase.PASSWORD,
                                    "--spring.datasource.url=" + TestDatabase.URL,
                                    "--spring.datasource.username=" + TestDatabase.USER,
                                    "--spring.datasource.password=" + TestDatabase.PASSWORD,
                                    "--spring.datasource.hikari.maximum-pool-size=10");
                    assertTestSchema(started);
                    context = started;
                    Runtime.getRuntime().addShutdownHook(new Thread(started::close));
                }
                ctx = context;
            }
        }
        return ctx;
    }

    /** Twardy bezpiecznik: testy NIGDY nie mogą pisać do roboczego schematu (public). */
    private static void assertTestSchema(ConfigurableApplicationContext ctx) {
        String schema = ctx.getBean(JdbcTemplate.class).queryForObject("SELECT current_schema()", String.class);
        if (!TestDatabase.SCHEMA.equals(schema)) {
            ctx.close();
            throw new IllegalStateException("Test JPA context is connected to schema '" + schema
                    + "' instead of '" + TestDatabase.SCHEMA + "' — refusing to run tests against working data");
        }
    }

    public static <T> T get(Class<T> type) {
        return context().getBean(type);
    }

    public static PlatformTransactionManager transactionManager() {
        return get(PlatformTransactionManager.class);
    }
}
