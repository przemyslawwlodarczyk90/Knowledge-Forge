package com.example.knowledgeforge;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.security.servlet.UserDetailsServiceAutoConfiguration;
import org.springframework.scheduling.annotation.EnableAsync;

/**
 * Punkt wejścia — Spring Boot (embedded Tomcat, Spring MVC, Spring Data JPA, Spring Security,
 * Spring WebSocket). Wiązanie obiektów, które wcześniej było ręczne (dawny Main), robi kontener:
 * serwisy/repozytoria/kontrolery przez adnotacje, reszta w config.ApplicationBeans.
 */
@SpringBootApplication(exclude = UserDetailsServiceAutoConfiguration.class)
@EnableAsync
public class KnowledgeForgeApplication {

    public static void main(String[] args) {
        SpringApplication.run(KnowledgeForgeApplication.class, args);
    }
}
