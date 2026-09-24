package me.kitkas1412.user.service.impl;

import me.kitkas1412.user.dto.request.LoginRequest;
import me.kitkas1412.user.dto.request.RegisterRequest;
import me.kitkas1412.user.dto.response.LoginResponse;
import me.kitkas1412.user.entity.Role;
import me.kitkas1412.user.entity.User;
import me.kitkas1412.user.exception.EmailAlreadyExistsException;
import me.kitkas1412.user.repository.UserRepository;
import me.kitkas1412.user.security.CustomUserDetails;
import me.kitkas1412.user.security.JwtService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.Duration;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuthServiceImplTest {

    @Mock
    private AuthenticationManager authenticationManager;
    @Mock
    private JwtService jwtService;
    @Mock
    private UserRepository userRepository;
    @Mock
    private PasswordEncoder passwordEncoder;

    @InjectMocks
    private AuthServiceImpl authService;

    @BeforeEach
    void setUp() {
        lenient().when(jwtService.generateAccessToken(any(CustomUserDetails.class))).thenReturn("jwt-token");
        lenient().when(jwtService.getAccessTokenTtl()).thenReturn(Duration.ofMinutes(15));
    }

    @Test
    void registerCreatesActiveUserWithNormalizedEmailAndHashedPassword() {
        when(userRepository.existsByEmail("user@example.com")).thenReturn(false);
        when(passwordEncoder.encode("MatKhau123")).thenReturn("{bcrypt}hash");

        LoginResponse response = authService.register(
                new RegisterRequest("  User@Example.COM ", "MatKhau123", "  Nguyễn Văn A  "));

        ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).saveAndFlush(captor.capture());
        User saved = captor.getValue();
        assertThat(saved.getEmail()).isEqualTo("user@example.com");
        assertThat(saved.getPassword()).isEqualTo("{bcrypt}hash");
        assertThat(saved.getFullName()).isEqualTo("Nguyễn Văn A");
        assertThat(saved.getRoles()).containsExactly(Role.USER);
        assertThat(saved.getStatus()).isEqualTo(User.UserStatus.ACTIVE);

        assertThat(response).isEqualTo(new LoginResponse(
                "jwt-token", "Bearer", 900L, "user@example.com", List.of("ROLE_USER")));
    }

    @Test
    void registerRejectsExistingEmailBeforeHashing() {
        when(userRepository.existsByEmail("user@example.com")).thenReturn(true);

        assertThatThrownBy(() -> authService.register(
                new RegisterRequest("User@example.com", "MatKhau123", "A")))
                .isInstanceOf(EmailAlreadyExistsException.class)
                .hasMessageContaining("user@example.com");

        verify(userRepository, never()).saveAndFlush(any());
        verifyNoInteractions(passwordEncoder, jwtService);
    }

    @Test
    void registerTranslatesUniqueConstraintRaceIntoEmailAlreadyExists() {
        when(userRepository.existsByEmail("user@example.com")).thenReturn(false);
        when(passwordEncoder.encode("MatKhau123")).thenReturn("{bcrypt}hash");
        when(userRepository.saveAndFlush(any(User.class)))
                .thenThrow(new DataIntegrityViolationException("uk_users_email"));

        assertThatThrownBy(() -> authService.register(
                new RegisterRequest("user@example.com", "MatKhau123", "A")))
                .isInstanceOf(EmailAlreadyExistsException.class);

        verify(jwtService, never()).generateAccessToken(any());
    }

    @Test
    void loginAuthenticatesWithNormalizedEmailAndIssuesToken() {
        CustomUserDetails principal = CustomUserDetails.fromClaims(
                UUID.randomUUID(), "user@example.com", List.of("ROLE_USER", "ROLE_ADMIN"));
        Authentication authenticated = new UsernamePasswordAuthenticationToken(
                principal, null, principal.getAuthorities());
        when(authenticationManager.authenticate(any())).thenReturn(authenticated);

        LoginResponse response = authService.login(new LoginRequest(" User@Example.com ", "MatKhau123"));

        ArgumentCaptor<Authentication> captor = ArgumentCaptor.forClass(Authentication.class);
        verify(authenticationManager).authenticate(captor.capture());
        assertThat(captor.getValue().getPrincipal()).isEqualTo("user@example.com");
        assertThat(captor.getValue().getCredentials()).isEqualTo("MatKhau123");

        verify(jwtService).generateAccessToken(principal);
        assertThat(response.accessToken()).isEqualTo("jwt-token");
        assertThat(response.tokenType()).isEqualTo("Bearer");
        assertThat(response.expiresIn()).isEqualTo(900L);
        assertThat(response.email()).isEqualTo("user@example.com");
        assertThat(response.roles()).containsExactlyInAnyOrder("ROLE_USER", "ROLE_ADMIN");
    }

    @Test
    void loginPropagatesBadCredentialsWithoutIssuingToken() {
        when(authenticationManager.authenticate(any())).thenThrow(new BadCredentialsException("bad"));

        assertThatThrownBy(() -> authService.login(new LoginRequest("user@example.com", "wrong")))
                .isInstanceOf(BadCredentialsException.class);

        verify(jwtService, never()).generateAccessToken(any());
    }
}
