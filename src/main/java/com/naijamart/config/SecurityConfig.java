
package com.naijamart.config;

import com.naijamart.security.GoogleLoginSuccessHandler;
import com.naijamart.security.JwtAuthenticationFilter;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;

import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
@EnableMethodSecurity
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthenticationFilter;
    private final GoogleLoginSuccessHandler googleLoginSuccessHandler;

    public SecurityConfig(
            JwtAuthenticationFilter jwtAuthenticationFilter,
            GoogleLoginSuccessHandler googleLoginSuccessHandler) {

        this.jwtAuthenticationFilter = jwtAuthenticationFilter;
        this.googleLoginSuccessHandler = googleLoginSuccessHandler;
    }

    @Bean
    public SecurityFilterChain securityFilterChain(
            HttpSecurity http) throws Exception {

        http
            .csrf(csrf -> csrf.disable())

            // OAuth needs a temporary session for the Google redirect.
            .sessionManagement(session ->
                session.sessionCreationPolicy(
                    SessionCreationPolicy.IF_REQUIRED
                )
            )

            .authorizeHttpRequests(auth -> auth

                // Public website pages and static resources.
                .requestMatchers(
                    "/",
                    "/index.html",
                    "/admin.html",
                    "/favicon.ico",
                    "/error",
                    "/css/**",
                    "/js/**",
                    "/images/**"
                ).permitAll()

                // Google Sign-In routes.
                .requestMatchers(
                    "/oauth2/**",
                    "/login/oauth2/**"
                ).permitAll()

                // Registration and login.
                .requestMatchers(
                    HttpMethod.POST,
                    "/api/users",
                    "/api/users/login",
                    "/api/auth/google/exchange"
                ).permitAll()

                // Public product browsing.
                .requestMatchers(
                    HttpMethod.GET,
                    "/api/products",
                    "/api/products/*"
                ).permitAll()

                // Product restock notifications.
                .requestMatchers(
                    HttpMethod.POST,
                    "/api/products/*/notify-when-available"
                ).permitAll()

                // Customer checkout.
                .requestMatchers(
                    HttpMethod.POST,
                    "/api/orders"
                ).permitAll()

                // Order tracking.
                .requestMatchers(
                    HttpMethod.GET,
                    "/api/orders/track/*"
                ).permitAll()

                // Local simulated payment endpoint.
                .requestMatchers(
                    HttpMethod.POST,
                    "/api/demo-payments/success"
                ).permitAll()

                // All other endpoints require authentication.
                .anyRequest().authenticated()
            )

            .oauth2Login(oauth -> oauth
                .successHandler(googleLoginSuccessHandler)
            )

            .addFilterBefore(
                jwtAuthenticationFilter,
                UsernamePasswordAuthenticationFilter.class
            );

        return http.build();
    }
}
