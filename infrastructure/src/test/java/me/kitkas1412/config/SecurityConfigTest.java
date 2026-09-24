package me.kitkas1412.config;

import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;

import static org.assertj.core.api.Assertions.assertThat;

class SecurityConfigTest {

    @Test
    void passwordEncoderStoresAlgorithmPrefixAndVerifies() {
        PasswordEncoder encoder = new SecurityConfig().passwordEncoder();

        String hash = encoder.encode("MatKhau123");

        assertThat(hash).startsWith("{bcrypt}");
        assertThat(encoder.matches("MatKhau123", hash)).isTrue();
        assertThat(encoder.matches("wrong-password", hash)).isFalse();
    }

    @Test
    void corsUsesTrimmedWhitelistWhenAllowAllIsOff() {
        SecurityConfig config = securityConfig(" http://localhost:3000 , ,http://localhost:5173", false);

        CorsConfiguration cors = corsFor(config.corsConfigurationSource());

        assertThat(cors.getAllowedOrigins()).containsExactly("http://localhost:3000", "http://localhost:5173");
        assertThat(cors.getAllowedOriginPatterns()).isNull();
        assertThat(cors.getAllowCredentials()).isTrue();
        assertThat(cors.getAllowedMethods()).contains("GET", "POST", "OPTIONS");
        assertThat(cors.getAllowedHeaders()).contains("Authorization", "Content-Type");
        assertThat(cors.getExposedHeaders()).contains("X-Token-Expired");
        assertThat(cors.getMaxAge()).isEqualTo(3600L);
    }

    @Test
    void corsAllowAllUsesOriginPatternsToStayCompatibleWithCredentials() {
        SecurityConfig config = securityConfig("http://ignored", true);

        CorsConfiguration cors = corsFor(config.corsConfigurationSource());

        assertThat(cors.getAllowedOriginPatterns()).containsExactly("*");
        assertThat(cors.getAllowedOrigins()).isNull();
        assertThat(cors.getAllowCredentials()).isTrue();
    }

    private static SecurityConfig securityConfig(String allowedOrigins, boolean allowAll) {
        SecurityConfig config = new SecurityConfig();
        ReflectionTestUtils.setField(config, "allowedOriginsRaw", allowedOrigins);
        ReflectionTestUtils.setField(config, "corsAllowAll", allowAll);
        return config;
    }

    private static CorsConfiguration corsFor(CorsConfigurationSource source) {
        return source.getCorsConfiguration(new MockHttpServletRequest("GET", "/api/events"));
    }
}
