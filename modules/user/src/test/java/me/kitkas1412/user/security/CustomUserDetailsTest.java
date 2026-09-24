package me.kitkas1412.user.security;

import me.kitkas1412.user.entity.Role;
import me.kitkas1412.user.entity.User;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.GrantedAuthority;

import java.util.EnumSet;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class CustomUserDetailsTest {

    @Test
    void fromActiveUserCopiesFieldsAndMapsRolesToAuthorities() {
        UUID id = UUID.randomUUID();
        User user = user(id, User.UserStatus.ACTIVE);
        user.setRoles(EnumSet.of(Role.USER, Role.ADMIN));

        CustomUserDetails details = CustomUserDetails.from(user);

        assertThat(details.getId()).isEqualTo(id);
        assertThat(details.getUsername()).isEqualTo("user@example.com");
        assertThat(details.getPassword()).isEqualTo("{bcrypt}hash");
        assertThat(details.getAuthorities()).extracting(GrantedAuthority::getAuthority)
                .containsExactlyInAnyOrder("ROLE_USER", "ROLE_ADMIN");
        assertThat(details.isEnabled()).isTrue();
        assertThat(details.isAccountNonLocked()).isTrue();
    }

    @Test
    void lockedUserIsEnabledFalseAndLocked() {
        CustomUserDetails details = CustomUserDetails.from(user(UUID.randomUUID(), User.UserStatus.LOCKED));

        assertThat(details.isEnabled()).isFalse();
        assertThat(details.isAccountNonLocked()).isFalse();
    }

    @Test
    void disabledUserIsNotEnabledButNotLocked() {
        CustomUserDetails details = CustomUserDetails.from(user(UUID.randomUUID(), User.UserStatus.DISABLED));

        assertThat(details.isEnabled()).isFalse();
        assertThat(details.isAccountNonLocked()).isTrue();
    }

    @Test
    void snapshotIsNotAffectedByLaterEntityChanges() {
        User user = user(UUID.randomUUID(), User.UserStatus.ACTIVE);
        CustomUserDetails details = CustomUserDetails.from(user);

        user.setEmail("changed@example.com");
        user.getRoles().add(Role.ADMIN);

        assertThat(details.getUsername()).isEqualTo("user@example.com");
        assertThat(details.getAuthorities()).extracting(GrantedAuthority::getAuthority).containsExactly("ROLE_USER");
    }

    @Test
    void authoritiesAreImmutable() {
        CustomUserDetails details = CustomUserDetails.from(user(UUID.randomUUID(), User.UserStatus.ACTIVE));

        assertThatThrownBy(() -> details.getAuthorities().clear()).isInstanceOf(UnsupportedOperationException.class);
    }

    @Test
    void fromClaimsHasNoPasswordAndIsAlwaysUsable() {
        UUID id = UUID.randomUUID();

        CustomUserDetails details = CustomUserDetails.fromClaims(id, "user@example.com", List.of("ROLE_USER"));

        assertThat(details.getId()).isEqualTo(id);
        assertThat(details.getUsername()).isEqualTo("user@example.com");
        assertThat(details.getPassword()).isNull();
        assertThat(details.getAuthorities()).extracting(GrantedAuthority::getAuthority).containsExactly("ROLE_USER");
        assertThat(details.isEnabled()).isTrue();
        assertThat(details.isAccountNonLocked()).isTrue();
        assertThat(details.isAccountNonExpired()).isTrue();
        assertThat(details.isCredentialsNonExpired()).isTrue();
    }

    private static User user(UUID id, User.UserStatus status) {
        User user = User.builder()
                .email("user@example.com")
                .password("{bcrypt}hash")
                .fullName("Nguyễn Văn A")
                .roles(EnumSet.of(Role.USER))
                .status(status)
                .build();
        user.setId(id);
        return user;
    }
}
