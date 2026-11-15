package com.example.knowledgeforge.config;

import org.springframework.beans.factory.InitializingBean;
import org.springframework.boot.autoconfigure.orm.jpa.EntityManagerFactoryDependsOnPostProcessor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import javax.sql.DataSource;

/**
 * Uruchamia idempotentne migracje {@link Schema} przy starcie, ZANIM powstanie EntityManagerFactory
 * (Hibernate ma ddl-auto=none i zastaje gotowy schemat). Zastępuje dawne ręczne
 * {@code Schema.ensure(dataSource)} z Main.
 */
@Configuration(proxyBeanMethods = false)
public class SchemaInitializer {

    public static final String BEAN_NAME = "knowledgeForgeSchema";

    @Bean(BEAN_NAME)
    InitializingBean knowledgeForgeSchema(DataSource dataSource) {
        return () -> Schema.ensure(dataSource);
    }

    /** Wymusza kolejność: najpierw schemat, potem JPA. */
    @Configuration(proxyBeanMethods = false)
    static class JpaDependsOnSchema extends EntityManagerFactoryDependsOnPostProcessor {
        JpaDependsOnSchema() {
            super(BEAN_NAME);
        }
    }
}
