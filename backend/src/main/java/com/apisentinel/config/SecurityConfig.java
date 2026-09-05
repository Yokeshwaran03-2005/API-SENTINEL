package com.apisentinel.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.provisioning.InMemoryUserDetailsManager;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.Arrays;
import java.util.List;

/**
 * Security configuration for API Sentinel.
 * Configures stateless REST security, CORS policies, and public access endpoints.
 */
@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Value("${sentinel.cors.allowed-origins:http://localhost:3000,http://127.0.0.1:3000,https://*.vercel.app}")
    private String allowedOrigins;

    @Value("${sentinel.cors.allowed-methods:GET,POST,PUT,DELETE,PATCH,OPTIONS}")
    private String allowedMethods;

    @Value("${sentinel.cors.allowed-headers:*}")
    private String allowedHeaders;

    @Value("${sentinel.cors.allow-credentials:true}")
    private boolean allowCredentials;

    @Bean
    public UserDetailsService userDetailsService() {
        // Prevents default password generation logs until phase 3 authentication is built
        return new InMemoryUserDetailsManager();
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))
                .csrf(AbstractHttpConfigurer::disable)
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        // Public system endpoints
                        .requestMatchers("/api/health").permitAll()
                        .requestMatchers("/actuator/**").permitAll()
                        // Phase 2 allows open access until auth module is implemented in subsequent phases
                        .anyRequest().permitAll()
                );

        return http.build();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();

        List<String> origins = Arrays.stream(allowedOrigins.split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .toList();

        configuration.setAllowedOriginPatterns(origins);
        configuration.setAllowedMethods(Arrays.asList(allowedMethods.split(",")));
        configuration.setAllowedHeaders(Arrays.asList(allowedHeaders.split(",")));
        configuration.setAllowCredentials(allowCredentials);
        configuration.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }
}
