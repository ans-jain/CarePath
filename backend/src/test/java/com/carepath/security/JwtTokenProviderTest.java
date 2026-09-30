package com.carepath.security;

import com.carepath.domain.enums.Role;
import com.carepath.domain.models.User;
import io.jsonwebtoken.Claims;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class JwtTokenProviderTest {

    private static final String TEST_SECRET = "404E635266556A586E3272357538782F413F4428472B4B6250645367566B5970";
    private static final long ACCESS_EXPIRATION_MS = 900000;   // 15 min
    private static final long REFRESH_EXPIRATION_MS = 604800000; // 7 days

    private JwtTokenProvider jwtTokenProvider;
    private UserPrincipal samplePrincipal;
    private UUID userId;

    @BeforeEach
    void setUp() {
        jwtTokenProvider = new JwtTokenProvider(TEST_SECRET, ACCESS_EXPIRATION_MS, REFRESH_EXPIRATION_MS);

        userId = UUID.randomUUID();
        User user = new User("sarah.jenkins@example.com", "dummyHash", Role.ROLE_PATIENT, "Sarah", "Jenkins");
        user.setId(userId);
        samplePrincipal = UserPrincipal.create(user);
    }

    @Test
    @DisplayName("Should generate valid access token containing expected claims")
    void testGenerateAndValidateAccessToken() {
        String token = jwtTokenProvider.generateAccessToken(samplePrincipal);

        assertThat(token).isNotBlank();
        assertThat(jwtTokenProvider.validateToken(token)).isTrue();

        Claims claims = jwtTokenProvider.getClaimsFromToken(token);
        assertThat(claims.getSubject()).isEqualTo(userId.toString());
        assertThat(claims.get("email", String.class)).isEqualTo("sarah.jenkins@example.com");
        assertThat(claims.get("role", String.class)).isEqualTo("ROLE_PATIENT");
        assertThat(claims.get("userId", String.class)).isEqualTo(userId.toString());
        assertThat(claims.getIssuedAt()).isNotNull();
        assertThat(claims.getExpiration()).isNotNull();
    }

    @Test
    @DisplayName("Should extract userId, email, and role from token")
    void testExtractClaims() {
        String token = jwtTokenProvider.generateAccessToken(samplePrincipal);

        assertThat(jwtTokenProvider.getUserIdFromToken(token)).isEqualTo(userId);
        assertThat(jwtTokenProvider.getEmailFromToken(token)).isEqualTo("sarah.jenkins@example.com");
        assertThat(jwtTokenProvider.getRoleFromToken(token)).isEqualTo("ROLE_PATIENT");
    }

    @Test
    @DisplayName("Should generate valid refresh token with tokenType claim")
    void testGenerateRefreshToken() {
        String refreshToken = jwtTokenProvider.generateRefreshToken(samplePrincipal);

        assertThat(refreshToken).isNotBlank();
        assertThat(jwtTokenProvider.validateToken(refreshToken)).isTrue();

        Claims claims = jwtTokenProvider.getClaimsFromToken(refreshToken);
        assertThat(claims.get("tokenType", String.class)).isEqualTo("REFRESH");
        assertThat(claims.get("email", String.class)).isEqualTo("sarah.jenkins@example.com");
    }

    @Test
    @DisplayName("Should reject expired token")
    void testExpiredToken() {
        // Provider configured with negative expiration
        JwtTokenProvider expiredProvider = new JwtTokenProvider(TEST_SECRET, -1000L, -1000L);
        String expiredToken = expiredProvider.generateAccessToken(samplePrincipal);

        assertThat(jwtTokenProvider.validateToken(expiredToken)).isFalse();
    }

    @Test
    @DisplayName("Should reject malformed token")
    void testMalformedToken() {
        assertThat(jwtTokenProvider.validateToken("not.a.valid.jwt")).isFalse();
    }

    @Test
    @DisplayName("Should reject tampered token")
    void testTamperedToken() {
        String token = jwtTokenProvider.generateAccessToken(samplePrincipal);
        String tamperedToken = token.substring(0, token.length() - 5) + "abcde";

        assertThat(jwtTokenProvider.validateToken(tamperedToken)).isFalse();
    }

    @Test
    @DisplayName("Should reject null or blank token")
    void testNullOrBlankToken() {
        assertThat(jwtTokenProvider.validateToken(null)).isFalse();
        assertThat(jwtTokenProvider.validateToken("")).isFalse();
        assertThat(jwtTokenProvider.validateToken("   ")).isFalse();
    }

    @Test
    @DisplayName("Should return expiration durations in seconds")
    void testExpirationDurations() {
        assertThat(jwtTokenProvider.getAccessTokenExpirationSeconds()).isEqualTo(900);
        assertThat(jwtTokenProvider.getRefreshTokenExpirationSeconds()).isEqualTo(604800);
    }
}
