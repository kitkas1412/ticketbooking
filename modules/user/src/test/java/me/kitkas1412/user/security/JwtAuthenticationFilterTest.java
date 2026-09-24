package me.kitkas1412.user.security;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import me.kitkas1412.user.entity.Role;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.authentication.TestingAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

import java.time.Duration;
import java.util.Date;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class JwtAuthenticationFilterTest {

    private final JwtService jwtService =
            JwtServiceTest.jwtService(JwtServiceTest.SECRET, JwtServiceTest.ISSUER, Duration.ofMinutes(15));
    private final JwtAuthenticationFilter filter = new JwtAuthenticationFilter(jwtService);

    private final UUID userId = UUID.randomUUID();

    @AfterEach
    void clearContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void requestWithoutAuthorizationHeaderStaysAnonymous() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/orders");
        MockFilterChain chain = new MockFilterChain();

        filter.doFilter(request, new MockHttpServletResponse(), chain);

        assertThat(chain.getRequest()).isSameAs(request);
        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
        assertThat(request.getAttribute(JwtAuthenticationFilter.TOKEN_ERROR_ATTRIBUTE)).isNull();
    }

    @Test
    void nonBearerOrEmptyBearerHeaderIsIgnored() throws Exception {
        for (String header : List.of("Basic dXNlcjpwYXNz", "Bearer ", "Bearer    ")) {
            MockHttpServletRequest request = requestWithAuthorization(header);
            MockFilterChain chain = new MockFilterChain();

            filter.doFilter(request, new MockHttpServletResponse(), chain);

            assertThat(chain.getRequest()).isSameAs(request);
            assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
            assertThat(request.getAttribute(JwtAuthenticationFilter.TOKEN_ERROR_ATTRIBUTE)).isNull();
        }
    }

    @Test
    void validTokenAuthenticatesPrincipalFromClaims() throws Exception {
        String token = jwtService.generateAccessToken(CustomUserDetails.fromClaims(
                userId, "user@example.com", List.of(Role.USER.getAuthority())));
        MockHttpServletRequest request = requestWithAuthorization("Bearer " + token);
        MockFilterChain chain = new MockFilterChain();

        filter.doFilter(request, new MockHttpServletResponse(), chain);

        assertThat(chain.getRequest()).isSameAs(request);
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        assertThat(authentication).isNotNull();
        assertThat(authentication.isAuthenticated()).isTrue();
        assertThat(authentication.getName()).isEqualTo("user@example.com");
        assertThat(authentication.getAuthorities()).extracting(GrantedAuthority::getAuthority)
                .containsExactly("ROLE_USER");
        assertThat(authentication.getPrincipal()).isInstanceOfSatisfying(CustomUserDetails.class, principal -> {
            assertThat(principal.getId()).isEqualTo(userId);
            assertThat(principal.getPassword()).isNull();
        });
        assertThat(authentication.getDetails()).isNotNull();
    }

    @Test
    void expiredTokenIsFlaggedAsExpiredAndLeftAnonymous() throws Exception {
        String expired = JwtServiceTest.jwtService(JwtServiceTest.SECRET, JwtServiceTest.ISSUER, Duration.ofMinutes(-5))
                .generateAccessToken(CustomUserDetails.fromClaims(userId, "user@example.com", List.of("ROLE_USER")));
        MockHttpServletRequest request = requestWithAuthorization("Bearer " + expired);
        MockFilterChain chain = new MockFilterChain();

        filter.doFilter(request, new MockHttpServletResponse(), chain);

        assertThat(chain.getRequest()).isSameAs(request);
        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
        assertThat(request.getAttribute(JwtAuthenticationFilter.TOKEN_ERROR_ATTRIBUTE))
                .isEqualTo(JwtAuthenticationFilter.TOKEN_EXPIRED);
    }

    @Test
    void garbageTokenIsFlaggedAsInvalid() throws Exception {
        MockHttpServletRequest request = requestWithAuthorization("Bearer not-a-jwt");
        MockFilterChain chain = new MockFilterChain();

        filter.doFilter(request, new MockHttpServletResponse(), chain);

        assertThat(chain.getRequest()).isSameAs(request);
        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
        assertThat(request.getAttribute(JwtAuthenticationFilter.TOKEN_ERROR_ATTRIBUTE))
                .isEqualTo(JwtAuthenticationFilter.TOKEN_INVALID);
    }

    @Test
    void signedTokenMissingRequiredClaimsIsFlaggedAsInvalid() throws Exception {
        String withoutUid = Jwts.builder()
                .issuer(JwtServiceTest.ISSUER)
                .subject("user@example.com")
                .claim(JwtService.CLAIM_ROLES, List.of("ROLE_USER"))
                .expiration(new Date(System.currentTimeMillis() + 60_000))
                .signWith(Keys.hmacShaKeyFor(Decoders.BASE64.decode(JwtServiceTest.SECRET)), Jwts.SIG.HS256)
                .compact();
        MockHttpServletRequest request = requestWithAuthorization("Bearer " + withoutUid);

        filter.doFilter(request, new MockHttpServletResponse(), new MockFilterChain());

        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
        assertThat(request.getAttribute(JwtAuthenticationFilter.TOKEN_ERROR_ATTRIBUTE))
                .isEqualTo(JwtAuthenticationFilter.TOKEN_INVALID);
    }

    @Test
    void existingAuthenticationIsNotOverwritten() throws Exception {
        TestingAuthenticationToken existing = new TestingAuthenticationToken("someone", null, "ROLE_ADMIN");
        SecurityContextHolder.getContext().setAuthentication(existing);
        String token = jwtService.generateAccessToken(CustomUserDetails.fromClaims(
                userId, "user@example.com", List.of("ROLE_USER")));
        MockHttpServletRequest request = requestWithAuthorization("Bearer " + token);

        filter.doFilter(request, new MockHttpServletResponse(), new MockFilterChain());

        assertThat(SecurityContextHolder.getContext().getAuthentication()).isSameAs(existing);
    }

    private static MockHttpServletRequest requestWithAuthorization(String header) {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/orders");
        request.addHeader("Authorization", header);
        return request;
    }
}
