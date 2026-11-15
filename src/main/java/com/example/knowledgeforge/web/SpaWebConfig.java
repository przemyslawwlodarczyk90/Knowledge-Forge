package com.example.knowledgeforge.web;

import com.example.knowledgeforge.backup.MaintenanceGate;
import org.springframework.boot.autoconfigure.security.SecurityProperties;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import org.springframework.web.servlet.resource.PathResourceResolver;

import java.io.IOException;

/**
 * Serwowanie zbudowanego frontendu z classpath:/static (dawny SpaServlet): istniejące pliki
 * (main.js, index.css, favicon.svg...) są serwowane wprost; wszystko inne (trasy React Router,
 * np. /dashboard, /login) dostaje index.html. Wyjątek: /api/** i /ws/** — nieznana trasa API to 404,
 * nie strona SPA.
 */
@Configuration
public class SpaWebConfig implements WebMvcConfigurer {

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        registry.addResourceHandler("/**")
                .addResourceLocations("classpath:/static/")
                .resourceChain(true)
                .addResolver(new PathResourceResolver() {
                    @Override
                    protected Resource getResource(String resourcePath, Resource location) throws IOException {
                        Resource requested = location.createRelative(resourcePath);
                        boolean looksLikeFile = !resourcePath.isEmpty() && !resourcePath.endsWith("/");
                        if (looksLikeFile && requested.exists() && requested.isReadable()) {
                            return requested;
                        }
                        if (resourcePath.startsWith("api/") || resourcePath.startsWith("ws/")) {
                            return null;
                        }
                        Resource index = new ClassPathResource("/static/index.html");
                        return index.exists() ? index : null;
                    }
                });
    }

    /**
     * Blokuje żądania modyfikujące dane na czas pg_restore (zob. DatabaseRestoreService) — zaraz po
     * łańcuchu Spring Security (CORS + JWT), przed kontrolerami.
     */
    @Bean
    public FilterRegistrationBean<MaintenanceModeFilter> maintenanceModeFilter(MaintenanceGate gate) {
        FilterRegistrationBean<MaintenanceModeFilter> reg = new FilterRegistrationBean<>(new MaintenanceModeFilter(gate));
        reg.addUrlPatterns("/*");
        reg.setOrder(SecurityProperties.DEFAULT_FILTER_ORDER + 1);
        return reg;
    }
}
