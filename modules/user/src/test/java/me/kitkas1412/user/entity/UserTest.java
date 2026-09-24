package me.kitkas1412.user.entity;

import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThat;

class UserTest {

    @Test
    void builderDefaultsToActiveUserRole() {
        User user = User.builder().email("user@example.com").build();

        assertThat(user.getRoles()).containsExactly(Role.USER);
        assertThat(user.getStatus()).isEqualTo(User.UserStatus.ACTIVE);
        assertThat(user.hasRole(Role.USER)).isTrue();
        assertThat(user.hasRole(Role.ADMIN)).isFalse();
    }

    @Test
    void normalizeEmailTrimsAndLowercases() {
        User user = User.builder().email("  User@Example.COM ").build();

        ReflectionTestUtils.invokeMethod(user, "normalizeEmail");

        assertThat(user.getEmail()).isEqualTo("user@example.com");
    }

    @Test
    void normalizeEmailToleratesNull() {
        User user = User.builder().build();

        ReflectionTestUtils.invokeMethod(user, "normalizeEmail");

        assertThat(user.getEmail()).isNull();
    }

    @Test
    void toStringDoesNotLeakPasswordHash() {
        User user = User.builder().email("user@example.com").password("{bcrypt}secret-hash").build();

        assertThat(user.toString()).doesNotContain("secret-hash");
    }

    @Test
    void roleAuthorityHasSpringSecurityPrefix() {
        assertThat(Role.USER.getAuthority()).isEqualTo("ROLE_USER");
        assertThat(Role.ADMIN.getAuthority()).isEqualTo("ROLE_ADMIN");
    }
}
