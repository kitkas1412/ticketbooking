package me.kitkas1412.ticketbooking.service.impl;

import me.kitkas1412.ticketbooking.dto.request.LoginRequest;
import me.kitkas1412.ticketbooking.dto.response.LoginResponse;
import me.kitkas1412.ticketbooking.security.CustomUserDetails;
import me.kitkas1412.ticketbooking.security.JwtService;
import me.kitkas1412.ticketbooking.service.AuthService;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class AuthServiceImpl implements AuthService {

    private final AuthenticationManager authenticationManager;
    private final JwtService jwtService;

    public AuthServiceImpl(AuthenticationManager authenticationManager, JwtService jwtService) {
        this.authenticationManager = authenticationManager;
        this.jwtService = jwtService;
    }

    /**
     * Uỷ quyền việc kiểm tra mật khẩu cho {@link AuthenticationManager} thay vì
     * tự gọi {@code passwordEncoder.matches()}.
     *
     * <p>Lý do: đi qua DaoAuthenticationProvider thì các cờ trạng thái tài khoản
     * (disabled/locked) được kiểm tra tự động, và provider luôn chạy một lần
     * hash "giả" khi không tìm thấy user — nhờ đó thời gian phản hồi của email
     * tồn tại và không tồn tại là như nhau, không rò rỉ qua timing.
     *
     * <p>Không nạp kết quả vào SecurityContextHolder: chain là stateless, request
     * tiếp theo sẽ tự xác thực lại bằng token vừa phát.
     */
    @Override
    public LoginResponse login(LoginRequest request) {
        // Chuẩn hoá giống lúc ghi (User.normalizeEmail) và giống
        // CustomUserDetailsService, nếu không thì email viết hoa sẽ không khớp.
        String email = request.email().trim().toLowerCase();

        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(email, request.password()));

        CustomUserDetails principal = (CustomUserDetails) authentication.getPrincipal();

        List<String> roles = principal.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .toList();

        return new LoginResponse(
                jwtService.generateAccessToken(principal),
                "Bearer",
                jwtService.getAccessTokenTtl().toSeconds(),
                principal.getUsername(),
                roles);
    }
}
