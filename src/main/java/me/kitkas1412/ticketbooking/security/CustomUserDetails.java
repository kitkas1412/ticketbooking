package me.kitkas1412.ticketbooking.security;

import lombok.Getter;
import me.kitkas1412.ticketbooking.entity.Role;
import me.kitkas1412.ticketbooking.entity.User;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Snapshot bất biến của User dùng trong SecurityContext.
 * <p>
 * Cố ý copy field ra thay vì giữ tham chiếu tới entity User: object này sống
 * suốt request (và được cache trong SecurityContext), nếu ôm entity đã detached
 * thì mọi truy cập collection lazy sẽ ném LazyInitializationException, đồng thời
 * dễ vô tình mang password hash đi xa hơn mức cần thiết.
 */
@Getter
public final class CustomUserDetails implements UserDetails {

    private final UUID id;
    private final String email;
    private final String password;
    private final Set<GrantedAuthority> authorities;
    private final boolean enabled;
    private final boolean accountNonLocked;

    private CustomUserDetails(UUID id, String email, String password,
                              Set<GrantedAuthority> authorities,
                              boolean enabled, boolean accountNonLocked) {
        this.id = id;
        this.email = email;
        this.password = password;
        this.authorities = authorities;
        this.enabled = enabled;
        this.accountNonLocked = accountNonLocked;
    }

    public static CustomUserDetails from(User user) {
        Set<GrantedAuthority> authorities = user.getRoles().stream()
                .map(Role::getAuthority)
                .map(SimpleGrantedAuthority::new)
                .collect(Collectors.toUnmodifiableSet());

        return new CustomUserDetails(
                user.getId(),
                user.getEmail(),
                user.getPassword(),
                authorities,
                user.getStatus() == User.UserStatus.ACTIVE,
                user.getStatus() != User.UserStatus.LOCKED
        );
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return authorities;
    }

    /**
     * Username của hệ thống này là email — đây là giá trị sẽ nằm trong
     * Authentication.getName() và là subject của JWT.
     */
    @Override
    public String getUsername() {
        return email;
    }

    @Override
    public boolean isEnabled() {
        return enabled;
    }

    @Override
    public boolean isAccountNonLocked() {
        return accountNonLocked;
    }
}
