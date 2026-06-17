package com.codeguardian.infrastructure.config;

import com.codeguardian.infrastructure.security.OAuth2LoginSuccessHandler;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.web.SecurityFilterChain;

/**
 * Security configuration setting up authentication rules for Web APIs, webhooks,
 * static frontend assets, and Spring Security OAuth2 login client.
 */
@Configuration
@EnableWebSecurity
public class SecurityConfig {

    private final OAuth2LoginSuccessHandler successHandler;
    private final String dashboardUrl;

    public SecurityConfig(OAuth2LoginSuccessHandler successHandler,
                          @Value("${codeguardian.dashboard-url:http://localhost:3000}") String dashboardUrl) {
        this.successHandler = successHandler;
        this.dashboardUrl = dashboardUrl;
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            // 1. Disable CSRF for endpoints called by webhooks or external clients (like curl/testing)
            .csrf(csrf -> csrf
                .ignoringRequestMatchers("/github/webhook", "/repository/index", "/feedback/**", "/api/**")
            )
            // 2. Map public, private, and webhook endpoint routes
            .authorizeHttpRequests(auth -> auth
                // Public system monitoring endpoints (probes)
                .requestMatchers("/actuator/health", "/actuator/info", "/actuator/**").permitAll()
                // Public GitHub webhook ingress point
                .requestMatchers("/github/webhook").permitAll()
                // Public assets & SPA files served locally
                .requestMatchers("/", "/index.html", "/assets/**", "/favicon.ico").permitAll()
                // All backend application API requests require authentication
                .requestMatchers("/api/**").authenticated()
                // Fallback permits access to React routing paths
                .anyRequest().permitAll()
            )
            // 3. Configure Spring Security OAuth2 Login with the custom success handler
            .oauth2Login(oauth2 -> oauth2
                .successHandler(successHandler)
            )
            // 4. Configure Session Logout
            .logout(logout -> logout
                .logoutUrl("/logout")
                .logoutSuccessUrl(dashboardUrl)
                .invalidateHttpSession(true)
                .clearAuthentication(true)
                .deleteCookies("JSESSIONID")
            );

        return http.build();
    }
}
