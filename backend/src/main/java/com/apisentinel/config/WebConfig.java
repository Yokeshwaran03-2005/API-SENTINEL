package com.apisentinel.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.util.Arrays;

/**
 * Spring MVC Web configuration.
 * Configures CORS mappings for frontend communication.
 */
@Configuration
public class WebConfig implements WebMvcConfigurer {

    @Value("${sentinel.cors.allowed-origins:http://localhost:3000,http://127.0.0.1:3000,https://*.vercel.app}")
    private String allowedOrigins;

    @Value("${sentinel.cors.allowed-methods:GET,POST,PUT,DELETE,PATCH,OPTIONS}")
    private String allowedMethods;

    @Value("${sentinel.cors.allowed-headers:*}")
    private String allowedHeaders;

    @Value("${sentinel.cors.allow-credentials:true}")
    private boolean allowCredentials;

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        String[] origins = Arrays.stream(allowedOrigins.split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .toArray(String[]::new);

        registry.addMapping("/**")
                .allowedOriginPatterns(origins)
                .allowedMethods(allowedMethods.split(","))
                .allowedHeaders(allowedHeaders.split(","))
                .allowCredentials(allowCredentials)
                .maxAge(3600);
    }
}
