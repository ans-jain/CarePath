package com.carepath.security;

import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import io.jsonwebtoken.security.SecurityException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.HexFormat;
import java.util.UUID;

@Component
public class JwtTokenProvider {

    private static final Logger log = LoggerFactory.getLogger(JwtTokenProvider.class);

    @Value("${jwt.secret:404E635266556A586E3272357538782F413F4428472B4B6250645367566B5970}")
    private String jwtSecret;

    @Value("${jwt.access-token-expiration-ms:900000}")
    private long jwtExpirationMs; // 15 minutes default

    @Value("${jwt.refresh-token-expiration-ms:604800000}")
    private long jwtRefreshExpirationMs; // 7 days default

    public JwtTokenProvider() {
    }

    public JwtTokenProvider(String jwtSecret, long jwtExpirationMs, long jwtRefreshExpirationMs) {
        this.jwtSecret = jwtSecret;
        this.jwtExpirationMs = jwtExpirationMs;
        this.jwtRefreshExpirationMs = jwtRefreshExpirationMs;
    }

    private SecretKey getSigningKey() {
        byte[] keyBytes;
        if (jwtSecret != null && jwtSecret.matches("^[0-9a-fA-F]+$") && jwtSecret.length() >= 64) {
            keyBytes = HexFormat.of().parseHex(jwtSecret);
        } else if (jwtSecret != null) {
            keyBytes = jwtSecret.getBytes(StandardCharsets.UTF_8);
        } else {
            throw new IllegalStateException("JWT Secret is not configured");
        }
        return Keys.hmacShaKeyFor(keyBytes);
    }

    public String generateAccessToken(UserPrincipal principal) {
        Date now = new Date();
        Date expiryDate = new Date(now.getTime() + jwtExpirationMs);

        return Jwts.builder()
                .subject(principal.getId().toString())
                .claim("email", principal.getUsername())
                .claim("role", principal.getRole().name())
                .claim("userId", principal.getId().toString())
                .issuedAt(now)
                .expiration(expiryDate)
                .signWith(getSigningKey(), Jwts.SIG.HS256)
                .compact();
    }

    public String generateRefreshToken(UserPrincipal principal) {
        Date now = new Date();
        Date expiryDate = new Date(now.getTime() + jwtRefreshExpirationMs);

        return Jwts.builder()
                .subject(principal.getId().toString())
                .claim("email", principal.getUsername())
                .claim("tokenType", "REFRESH")
                .issuedAt(now)
                .expiration(expiryDate)
                .signWith(getSigningKey(), Jwts.SIG.HS256)
                .compact();
    }

    public boolean validateToken(String token) {
        if (token == null || token.trim().isEmpty()) {
            return false;
        }
        try {
            Jwts.parser()
                    .verifyWith(getSigningKey())
                    .build()
                    .parseSignedClaims(token.trim());
            return true;
        } catch (SecurityException | MalformedJwtException e) {
            log.warn("Invalid JWT signature or malformed token");
        } catch (ExpiredJwtException e) {
            log.warn("Expired JWT token");
        } catch (UnsupportedJwtException e) {
            log.warn("Unsupported JWT token format");
        } catch (IllegalArgumentException e) {
            log.warn("JWT claims string is empty or invalid");
        }
        return false;
    }

    public Claims getClaimsFromToken(String token) {
        return Jwts.parser()
                .verifyWith(getSigningKey())
                .build()
                .parseSignedClaims(token.trim())
                .getPayload();
    }

    public UUID getUserIdFromToken(String token) {
        Claims claims = getClaimsFromToken(token);
        String subject = claims.getSubject();
        if (subject != null) {
            return UUID.fromString(subject);
        }
        String userId = claims.get("userId", String.class);
        return UUID.fromString(userId);
    }

    public String getEmailFromToken(String token) {
        Claims claims = getClaimsFromToken(token);
        String email = claims.get("email", String.class);
        if (email != null) {
            return email;
        }
        return claims.getSubject();
    }

    public String getRoleFromToken(String token) {
        Claims claims = getClaimsFromToken(token);
        return claims.get("role", String.class);
    }

    public Date getExpirationFromToken(String token) {
        Claims claims = getClaimsFromToken(token);
        return claims.getExpiration();
    }

    public long getAccessTokenExpirationSeconds() {
        return jwtExpirationMs / 1000;
    }

    public long getRefreshTokenExpirationSeconds() {
        return jwtRefreshExpirationMs / 1000;
    }
}
