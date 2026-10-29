
package com.naijamart.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;

@Service
public class JwtService {

    /*
     * Secret key used to sign JWT tokens.
     *
     * IMPORTANT:
     * In production, this should be stored in an
     * environment variable rather than directly in the code.
     */
    private static final String SECRET_KEY =
            "NaijaMartSuperSecretKeyForJwtAuthentication2026VerySecureKey";

    /*
     * Token validity:
     * 24 hours
     */
    private static final long EXPIRATION_TIME =
            1000 * 60 * 60 * 24;

    private SecretKey getSigningKey() {

        return Keys.hmacShaKeyFor(
                SECRET_KEY.getBytes(StandardCharsets.UTF_8)
        );
    }

    /**
     * Generate JWT token for a user.
     */
    public String generateToken(String email, String role) {

        Date now = new Date();

        Date expiration =
                new Date(now.getTime() + EXPIRATION_TIME);

        return Jwts.builder()
                .subject(email)
                .claim("role", role)
                .issuedAt(now)
                .expiration(expiration)
                .signWith(getSigningKey())
                .compact();
    }

    /**
     * Extract email from JWT token.
     */
    public String extractEmail(String token) {

        return extractAllClaims(token)
                .getSubject();
    }

    /**
     * Extract role from JWT token.
     */
    public String extractRole(String token) {

        return extractAllClaims(token)
                .get("role", String.class);
    }

    /**
     * Check whether JWT token is still valid.
     */
    public boolean isTokenValid(String token) {

        try {

            Claims claims = extractAllClaims(token);

            return claims.getExpiration()
                    .after(new Date());

        } catch (Exception e) {

            return false;
        }
    }

    /**
     * Read all claims from JWT token.
     */
    private Claims extractAllClaims(String token) {

        return Jwts.parser()
                .verifyWith(getSigningKey())
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }
}
