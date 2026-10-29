
package com.naijamart.security;

import com.naijamart.entity.User;
import com.naijamart.service.GoogleAccountService;



import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class GoogleLoginSuccessHandler
        implements AuthenticationSuccessHandler {

    private final GoogleAccountService googleAccountService;
    private final JwtService jwtService;

    private final SecureRandom secureRandom = new SecureRandom();

    private final Map<String, LoginCode> loginCodes =
            new ConcurrentHashMap<>();

    private static final long CODE_LIFETIME_MILLIS = 60_000;

    public GoogleLoginSuccessHandler(
            GoogleAccountService googleAccountService,
            JwtService jwtService) {

        this.googleAccountService = googleAccountService;
        this.jwtService = jwtService;
    }

    @Override
    public void onAuthenticationSuccess(
            HttpServletRequest request,
            HttpServletResponse response,
            Authentication authentication)
            throws IOException, ServletException {

        OAuth2User googleUser =
                (OAuth2User) authentication.getPrincipal();

        String email = googleUser.getAttribute("email");
        Boolean emailVerified =
                googleUser.getAttribute("email_verified");

        if (email == null || !Boolean.TRUE.equals(emailVerified)) {
            response.sendError(
                    HttpServletResponse.SC_UNAUTHORIZED,
                    "Google did not return a verified email address."
            );
            return;
        }

        String firstName = googleUser.getAttribute("given_name");
        String lastName = googleUser.getAttribute("family_name");

        User user = googleAccountService.findOrCreateCustomer(
                email,
                firstName,
                lastName
        );

        String token = jwtService.generateToken(
                user.getEmail(),
                user.getRole().name()
        );

        byte[] randomBytes = new byte[32];
        secureRandom.nextBytes(randomBytes);

        String code = Base64.getUrlEncoder()
                .withoutPadding()
                .encodeToString(randomBytes);

        long expiresAt = System.currentTimeMillis()
                + CODE_LIFETIME_MILLIS;

        loginCodes.put(code, new LoginCode(token, expiresAt));

        // Remove expired codes as new logins complete.
        loginCodes.entrySet().removeIf(
                entry -> entry.getValue().expiresAt()
                        < System.currentTimeMillis()
        );

        String redirectUrl = "/?googleCode="
                + URLEncoder.encode(
                        code,
                        StandardCharsets.UTF_8
                );

        response.sendRedirect(redirectUrl);
    }

    public String consumeLoginCode(String code) {

        if (code == null || code.isBlank()) {
            return null;
        }

        LoginCode loginCode = loginCodes.remove(code);

        if (loginCode == null
                || loginCode.expiresAt() < System.currentTimeMillis()) {
            return null;
        }

        return loginCode.token();
    }

    private record LoginCode(String token, long expiresAt) {
    }
}
