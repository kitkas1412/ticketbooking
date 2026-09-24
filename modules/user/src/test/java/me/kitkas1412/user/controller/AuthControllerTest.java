package me.kitkas1412.user.controller;

import me.kitkas1412.common.exception.GlobalExceptionHandler;
import me.kitkas1412.user.dto.request.LoginRequest;
import me.kitkas1412.user.dto.request.RegisterRequest;
import me.kitkas1412.user.dto.response.LoginResponse;
import me.kitkas1412.user.exception.EmailAlreadyExistsException;
import me.kitkas1412.user.exception.UserExceptionHandler;
import me.kitkas1412.user.service.AuthService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.LockedException;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.List;

import static org.hamcrest.Matchers.containsString;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class AuthControllerTest {

    private final AuthService authService = mock(AuthService.class);

    private MockMvc mockMvc;

    private final LoginResponse loginResponse =
            new LoginResponse("jwt-token", "Bearer", 900L, "user@example.com", List.of("ROLE_USER"));

    @BeforeEach
    void setUp() {
        // UserExceptionHandler đứng trước vì GlobalExceptionHandler có handler bắt mọi Exception.
        mockMvc = MockMvcBuilders.standaloneSetup(new AuthController(authService))
                .setControllerAdvice(new UserExceptionHandler(), new GlobalExceptionHandler())
                .build();
    }

    @Test
    void registerReturns201WithToken() throws Exception {
        when(authService.register(new RegisterRequest("user@example.com", "MatKhau123", "Nguyễn Văn A")))
                .thenReturn(loginResponse);

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email":"user@example.com","password":"MatKhau123","fullName":"Nguyễn Văn A"}
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.accessToken").value("jwt-token"))
                .andExpect(jsonPath("$.data.tokenType").value("Bearer"))
                .andExpect(jsonPath("$.data.expiresIn").value(900))
                .andExpect(jsonPath("$.data.roles[0]").value("ROLE_USER"));
    }

    @Test
    void registerWithInvalidBodyReturns400WithFieldErrors() throws Exception {
        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email":"not-an-email","password":"short","fullName":""}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.error.status").value(400))
                .andExpect(jsonPath("$.error.detail").value(containsString("email")))
                .andExpect(jsonPath("$.error.detail").value(containsString("password")))
                .andExpect(jsonPath("$.error.detail").value(containsString("fullName")));

        verifyNoInteractions(authService);
    }

    @Test
    void registerWithPasswordLongerThanBcryptLimitIsRejected() throws Exception {
        String tooLong = "a".repeat(73);

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"user@example.com\",\"password\":\"" + tooLong + "\",\"fullName\":\"A\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.detail").value(containsString("password")));

        verifyNoInteractions(authService);
    }

    @Test
    void registerWithDuplicateEmailReturns409() throws Exception {
        when(authService.register(any())).thenThrow(new EmailAlreadyExistsException("Email đã được sử dụng: user@example.com"));

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email":"user@example.com","password":"MatKhau123","fullName":"A"}
                                """))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.error.detail").value("Email đã được sử dụng: user@example.com"));
    }

    @Test
    void loginReturns200WithToken() throws Exception {
        when(authService.login(new LoginRequest("user@example.com", "MatKhau123"))).thenReturn(loginResponse);

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email":"user@example.com","password":"MatKhau123"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.accessToken").value("jwt-token"));
    }

    @Test
    void loginWithWrongCredentialsReturns401() throws Exception {
        when(authService.login(any())).thenThrow(new BadCredentialsException("Bad credentials"));

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email":"user@example.com","password":"wrong-password"}
                                """))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error.detail").value("Email hoặc mật khẩu không đúng"));
    }

    @Test
    void loginWithLockedAccountReturns403() throws Exception {
        when(authService.login(any())).thenThrow(new LockedException("locked"));

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email":"user@example.com","password":"MatKhau123"}
                                """))
                .andExpect(status().isForbidden());
    }

    @Test
    void loginWithMissingFieldsReturns400() throws Exception {
        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(authService);
    }
}
