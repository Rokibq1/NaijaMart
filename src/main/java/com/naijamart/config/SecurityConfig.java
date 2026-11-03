
package com.naijamart.config;

import com.naijamart.security.GoogleLoginSuccessHandler;
import com.naijamart.security.JwtAuthenticationFilter;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;

import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
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
            HttpSecurity http,
            ObjectProvider<ClientRegistrationRepository> registrationProvider)
            throws Exception {

        http
            .csrf(csrf -> csrf.disable())

            .sessionManagement(session ->
                session.sessionCreationPolicy(
                    SessionCreationPolicy.IF_REQUIRED
                )
            )

            .authorizeHttpRequests(auth -> auth

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

                .requestMatchers(
                    "/oauth2/**",
                    "/login/oauth2/**"
                ).permitAll()

                .requestMatchers(
                    HttpMethod.POST,
                    "/api/users",
                    "/api/users/login",
                    "/api/auth/google/exchange"
                ).permitAll()

                .requestMatchers(
                    HttpMethod.GET,
                    "/api/products",
                    "/api/products/*"
                ).permitAll()

                .requestMatchers(
                    HttpMethod.POST,
                    "/api/products/*/notify-when-available"
                ).permitAll()

                .requestMatchers(
                    HttpMethod.POST,
                    "/api/orders"
                ).permitAll()

                .requestMatchers(
                    HttpMethod.GET,
                    "/api/orders/track/*"
                ).permitAll()

                .requestMatchers(
                    HttpMethod.POST,
                    "/api/demo-payments/success"
                ).permitAll()

                .anyRequest().authenticated()
            );

        // Enable Google login only when Google credentials are configured.
        if (registrationProvider.getIfAvailable() != null) {
            http.oauth2Login(oauth ->
                oauth.successHandler(googleLoginSuccessHandler)
            );
        }

        http.addFilterBefore(
            jwtAuthenticationFilter,
            UsernamePasswordAuthenticationFilter.class
        );

        return http.build();
    }
}
