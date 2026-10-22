
package com.vendortransport.booking.config;

import jakarta.servlet.DispatcherType;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
public class SecurityConfig {

    // First security chain: Customer registration API
    @Bean
    @Order(1)
    public SecurityFilterChain registrationSecurity(
            HttpSecurity http) throws Exception {

        http
            .securityMatcher("/api/auth/register")

            // Registration is a stateless JSON API
            .csrf(csrf -> csrf.disable())

            .sessionManagement(session -> session
                .sessionCreationPolicy(
                    SessionCreationPolicy.STATELESS
                )
            )

            .authorizeHttpRequests(auth -> auth
                .requestMatchers(
                    HttpMethod.POST,
                    "/api/auth/register"
                ).permitAll()

                .anyRequest().denyAll()
            );

        return http.build();
    }

    // Second security chain: Remaining application endpoints
    @Bean
    @Order(2)
    public SecurityFilterChain applicationSecurity(
            HttpSecurity http) throws Exception {

        http
            .authorizeHttpRequests(auth -> auth

                // Allow Spring Boot error dispatches
                .dispatcherTypeMatchers(
                    DispatcherType.ERROR
                ).permitAll()

                // Public application endpoints
                .requestMatchers(
                    "/api",
                    "/actuator/health",
                    "/error"
                ).permitAll()

                // All other endpoints require authentication
                .anyRequest().authenticated()
            )

            // Default login page for protected endpoints
            .formLogin(form -> form.permitAll());

        return http.build();
    }

    // BCrypt password hashing
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
