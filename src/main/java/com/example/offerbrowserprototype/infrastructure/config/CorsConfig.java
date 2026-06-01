package com.example.offerbrowserprototype.infrastructure.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class CorsConfig {

    private static final Logger logger = LoggerFactory.getLogger(CorsConfig.class);

    @Value("${app.cors.allowed-origin}")
    private String allowedOrigin;

    @Bean
    public WebMvcConfigurer corsConfigurer() {
        logger.info("Initializing CORS configuration.");
        logger.debug("Allowed origin for CORS: {}", allowedOrigin);

        return new WebMvcConfigurer() {
            @Override
            public void addCorsMappings(CorsRegistry registry) {
                logger.info("Adding CORS mappings for all endpoints.");
                try {
                    registry.addMapping("/**")
                            .allowedOrigins(allowedOrigin)
                            .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS")
                            .allowedHeaders("*")
                            .allowCredentials(true);
                    logger.info("CORS mappings added successfully.");
                } catch (Exception e) {
                    logger.error("Error while configuring CORS", e);
                    throw e;
                }
            }
        };
    }
}
