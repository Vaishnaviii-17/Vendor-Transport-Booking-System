
package com.vendortransport.booking.config;

import jakarta.servlet.DispatcherType;

import java.util.List;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;

import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.HttpStatusEntryPoint;

@Configuration
public class SecurityConfig {

    // Public registration and login endpoints
    @Bean
    @Order(1)
    public SecurityFilterChain authSecurity(
            HttpSecurity http) throws Exception {

        http
            .securityMatcher("/api/auth/**")
            .csrf(csrf -> csrf.disable())
            .sessionManagement(session -> session
                .sessionCreationPolicy(SessionCreationPolicy.STATELESS)
            )
            .authorizeHttpRequests(auth -> auth
                .requestMatchers(
                    HttpMethod.POST,
                    "/api/auth/register",
                    "/api/auth/login"
                ).permitAll()
                .anyRequest().denyAll()
            )
            .exceptionHandling(ex -> ex
                .authenticationEntryPoint(
                    new HttpStatusEntryPoint(HttpStatus.UNAUTHORIZED)
                )
            );

        return http.build();
    }

    // Protected application endpoints
    @Bean
    @Order(2)
    public SecurityFilterChain applicationSecurity(
            HttpSecurity http,
            JwtAuthenticationConverter jwtAuthenticationConverter
    ) throws Exception {

        http
            .csrf(csrf -> csrf.disable())
            .sessionManagement(session -> session
                .sessionCreationPolicy(SessionCreationPolicy.STATELESS)
            )
            .authorizeHttpRequests(auth -> auth
                .dispatcherTypeMatchers(
                    DispatcherType.ERROR
                ).permitAll()

                .requestMatchers(
                    "/",
                    "/css/**",
                    "/js/**",
                    "/favicon.ico",
                    "/api",
                    "/actuator/health",
                    "/error"
                ).permitAll()

                .requestMatchers(
                    HttpMethod.GET,
                    "/api/customer/**",
                    "/api/vehicles",
                    "/api/availability"
                ).hasRole("CUSTOMER")
                
                .requestMatchers(
                HttpMethod.POST,
                "/api/bookings"
                ).hasRole("CUSTOMER")
                
                .requestMatchers(
                    "/api/admin/**"
                ).hasRole("ADMIN")

                .requestMatchers(
                HttpMethod.GET,
                "/api/bookings/my",
                "/api/bookings/*"
                ).hasAnyRole("CUSTOMER", "ADMIN")
                
                .anyRequest().authenticated()
            )
            .oauth2ResourceServer(oauth2 -> oauth2
                .jwt(jwt -> jwt
                    .jwtAuthenticationConverter(
                        jwtAuthenticationConverter
                    )
                )
            );

        return http.build();
    }

    // Convert JWT role claim to Spring Security authority
    @Bean
    public JwtAuthenticationConverter jwtAuthenticationConverter() {

        JwtAuthenticationConverter converter =
                new JwtAuthenticationConverter();

        converter.setJwtGrantedAuthoritiesConverter(jwt -> {

            String role = jwt.getClaimAsString("role");

            if (role == null || role.isBlank()) {
                return List.of();
            }

            return List.of(
                new SimpleGrantedAuthority("ROLE_" + role)
            );
        });

        return converter;
    }

    // BCrypt password encoder
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}

