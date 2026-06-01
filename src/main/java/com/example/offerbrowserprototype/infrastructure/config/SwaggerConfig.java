package com.example.offerbrowserprototype.infrastructure.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class SwaggerConfig {

    @Bean
    public OpenAPI customOpenAPI() {

        SecurityScheme securityScheme = new SecurityScheme()
                .name("Bearer Authentication")
                .type(SecurityScheme.Type.HTTP)
                .scheme("bearer")
                .bearerFormat("JWT");

        Components components = new Components()
                .addSecuritySchemes("bearerAuth", securityScheme);


        SecurityRequirement securityRequirement = new SecurityRequirement()
                .addList("bearerAuth");

        Info apiInfo = new Info()
                .title("Offer Browser API")
                .description("Explore and manage job offers effortlessly with the Offer Browser API.\n\n"
                        + "### Key Features:\n"
                        + "- **User Management**: Register, confirm, and authenticate users securely with JWT.\n"
                        + "- **Offer Management**: Create, update, and organize job offers efficiently.\n"
                        + "- **Data Integration**: Fetch job offers dynamically from multiple external sources.\n"
                        + "- **Enhanced Analytics**: Access detailed statistics for better insights.\n\n"
                        + "Have questions? Contact our support team for assistance.")
                .version("1.0.0")
                .license(new License().name("MIT License").url("https://opensource.org/licenses/MIT"));


        return new OpenAPI()
                .components(components)
                .addSecurityItem(securityRequirement)
                .info(apiInfo);
    }
}
