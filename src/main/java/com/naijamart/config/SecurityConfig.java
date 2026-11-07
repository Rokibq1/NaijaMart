
package com.naijamart.config;

import com.naijamart.security.GoogleLoginSuccessHandler;
import com.naijamart.security.JwtAuthenticationFilter;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;

import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.HttpStatusEntryPoint;
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

                // PUBLIC WEBSITE FILES
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

                // GOOGLE OAUTH2 LOGIN
                .requestMatchers(
                    "/oauth2/**",
                    "/login/oauth2/**"
                ).permitAll()

                // CUSTOMER REGISTRATION AND LOGIN
                .requestMatchers(
                    HttpMethod.POST,
                    "/api/users",
                    "/api/users/login",
                    "/api/auth/google/exchange"
                ).permitAll()

                // PUBLIC PRODUCT BROWSING
                .requestMatchers(
                    HttpMethod.GET,
                    "/api/products",
                    "/api/products/*"
                ).permitAll()

                // PRODUCT STOCK NOTIFICATION
                .requestMatchers(
                    HttpMethod.POST,
                    "/api/products/*/notify-when-available"
                ).permitAll()

                // PUBLIC BANK ACCOUNT DETAILS
                .requestMatchers(
                    HttpMethod.GET,
                    "/api/transfers/bank-details"
                ).permitAll()

                // BLOCK OLD PAYMENT ENDPOINTS
                .requestMatchers(
                    "/api/demo-payments/**",
                    "/api/payments/**"
                ).denyAll()

                // CUSTOMER CHECKOUT REQUIRES LOGIN
                .requestMatchers(
                    HttpMethod.POST,
                    "/api/orders"
                ).authenticated()

                // PUBLIC ORDER TRACKING
                .requestMatchers(
                    HttpMethod.GET,
                    "/api/orders/track/*"
                ).permitAll()

                // OTHER BANK TRANSFER ENDPOINTS REQUIRE LOGIN
                .requestMatchers(
                    "/api/transfers/**"
                ).authenticated()

                // ALL OTHER ENDPOINTS REQUIRE AUTHENTICATION
                .anyRequest().authenticated()
            )

            // RETURN HTTP 401 FOR API REQUESTS
            // INSTEAD OF REDIRECTING TO GOOGLE LOGIN
            .exceptionHandling(exceptions ->
                exceptions.defaultAuthenticationEntryPointFor(
                    new HttpStatusEntryPoint(HttpStatus.UNAUTHORIZED),
                    request -> request.getServletPath().startsWith("/api/")
                )
            );

        // ENABLE GOOGLE LOGIN WHEN GOOGLE CREDENTIALS EXIST
        if (registrationProvider.getIfAvailable() != null) {

            http.oauth2Login(oauth ->
                oauth.successHandler(googleLoginSuccessHandler)
            );
        }

        // JWT AUTHENTICATION FILTER
        http.addFilterBefore(
            jwtAuthenticationFilter,
            UsernamePasswordAuthenticationFilter.class
        );

        return http.build();
    }
}
