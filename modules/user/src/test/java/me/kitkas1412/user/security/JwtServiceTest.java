package me.kitkas1412.user.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.IncorrectClaimException;
import io.jsonwebtoken.security.SignatureException;
import io.jsonwebtoken.security.WeakKeyException;
import me.kitkas1412.user.entity.Role;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Base64;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class JwtServiceTest {

    static final String SECRET = base64("0123456789abcdef0123456789abcdef");
    static final String OTHER_SECRET = base64("fedcba9876543210fedcba9876543210");
    static final String ISSUER = "ticketbooking-test";

    private final JwtService jwtService = jwtService(SECRET, ISSUER, Duration.ofMinutes(15));

    private final UUID userId = UUID.randomUUID();
    private final CustomUserDetails user = CustomUserDetails.fromClaims(
            userId, "user@example.com", List.of(Role.USER.getAuthority(), Role.ADMIN.getAuthority()));

    @Test
    void generatedTokenCarriesSubjectUserIdRolesAndIssuer() {
        String token = jwtService.generateAccessToken(user);

        Claims claims = jwtService.parseClaims(token);

        assertThat(claims.getSubject()).isEqualTo("user@example.com");
        assertThat(claims.getIssuer()).isEqualTo(ISSUER);
        assertThat(claims.getId()).isNotBlank();
        assertThat(claims.getExpiration()).isAfter(claims.getIssuedAt());
        assertThat(Duration.between(claims.getIssuedAt().toInstant(), claims.getExpiration().toInstant()))
                .isEqualTo(Duration.ofMinutes(15));
    }

    @Test
    void extractorsReadBackTheClaims() {
        String token = jwtService.generateAccessToken(user);

        assertThat(jwtService.extractUsername(token)).isEqualTo("user@example.com");
        assertThat(jwtService.extractUserId(token)).isEqualTo(userId);
        assertThat(jwtService.extractRoles(token)).containsExactlyInAnyOrder("ROLE_USER", "ROLE_ADMIN");
    }

    @Test
    void eachTokenHasUniqueId() {
        Claims first = jwtService.parseClaims(jwtService.generateAccessToken(user));
        Claims second = jwtService.parseClaims(jwtService.generateAccessToken(user));

        assertThat(first.getId()).isNotEqualTo(second.getId());
    }

    @Test
    void tokenSignedWithAnotherKeyIsRejected() {
        String foreignToken = jwtService(OTHER_SECRET, ISSUER, Duration.ofMinutes(15)).generateAccessToken(user);

        assertThatThrownBy(() -> jwtService.parseClaims(foreignToken)).isInstanceOf(SignatureException.class);
        assertThat(jwtService.isTokenValid(foreignToken)).isFalse();
    }

    @Test
    void tokenFromAnotherIssuerIsRejected() {
        String foreignToken = jwtService(SECRET, "someone-else", Duration.ofMinutes(15)).generateAccessToken(user);

        assertThatThrownBy(() -> jwtService.parseClaims(foreignToken)).isInstanceOf(IncorrectClaimException.class);
        assertThat(jwtService.isTokenValid(foreignToken)).isFalse();
    }

    @Test
    void expiredTokenBeyondClockSkewIsRejected() {
        String expired = jwtService(SECRET, ISSUER, Duration.ofMinutes(-5)).generateAccessToken(user);

        assertThatThrownBy(() -> jwtService.parseClaims(expired)).isInstanceOf(ExpiredJwtException.class);
        assertThat(jwtService.isTokenValid(expired)).isFalse();
    }

    @Test
    void tokenExpiredWithinClockSkewIsStillAccepted() {
        String justExpired = jwtService(SECRET, ISSUER, Duration.ofSeconds(-30)).generateAccessToken(user);

        assertThat(jwtService.isTokenValid(justExpired)).isTrue();
    }

    @Test
    void malformedOrEmptyTokenIsInvalid() {
        assertThat(jwtService.isTokenValid("not-a-jwt")).isFalse();
        assertThat(jwtService.isTokenValid("")).isFalse();
    }

    @Test
    void validTokenIsValid() {
        assertThat(jwtService.isTokenValid(jwtService.generateAccessToken(user))).isTrue();
    }

    @Test
    void secretShorterThan256BitsFailsFast() {
        String weakSecret = base64("too-short-key");

        assertThatThrownBy(() -> jwtService(weakSecret, ISSUER, Duration.ofMinutes(15)))
                .isInstanceOf(WeakKeyException.class);
    }

    @Test
    void exposesConfiguredTtl() {
        assertThat(jwtService.getAccessTokenTtl()).isEqualTo(Duration.ofMinutes(15));
    }

    static JwtService jwtService(String secret, String issuer, Duration ttl) {
        return new JwtService(new JwtProperties(secret, issuer, ttl));
    }

    private static String base64(String raw) {
        return Base64.getEncoder().encodeToString(raw.getBytes(StandardCharsets.UTF_8));
    }
}
