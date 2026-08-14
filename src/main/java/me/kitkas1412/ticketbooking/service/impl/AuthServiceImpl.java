package me.kitkas1412.ticketbooking.service.impl;

import me.kitkas1412.ticketbooking.dto.request.LoginRequest;
import me.kitkas1412.ticketbooking.dto.request.RegisterRequest;
import me.kitkas1412.ticketbooking.dto.response.LoginResponse;
import me.kitkas1412.ticketbooking.entity.Role;
import me.kitkas1412.ticketbooking.entity.User;
import me.kitkas1412.ticketbooking.exception.EmailAlreadyExistsException;
import me.kitkas1412.ticketbooking.repository.UserRepository;
import me.kitkas1412.ticketbooking.security.CustomUserDetails;
import me.kitkas1412.ticketbooking.security.JwtService;
import me.kitkas1412.ticketbooking.service.AuthService;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.EnumSet;
import java.util.List;

@Service
public class AuthServiceImpl implements AuthService {

    private final AuthenticationManager authenticationManager;
    private final JwtService jwtService;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public AuthServiceImpl(AuthenticationManager authenticationManager,
                           JwtService jwtService,
                           UserRepository userRepository,
                           PasswordEncoder passwordEncoder) {
        this.authenticationManager = authenticationManager;
        this.jwtService = jwtService;
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    /**
     * Luôn gán {@link Role#USER}. Vai trò không đến từ request — xem javadoc
     * {@link RegisterRequest}.
     *
     * <p>Kiểm {@code existsByEmail} trước chỉ để trả lỗi đẹp trong trường hợp
     * thường; nó không phải hàng rào thật vì hai request đồng thời đều có thể
     * vượt qua. Hàng rào thật là unique constraint {@code uk_users_email} dưới
     * DB, và đó là lý do phải bắt {@code DataIntegrityViolationException}.
     */
    @Override
    @Transactional
    public LoginResponse register(RegisterRequest request) {
        String email = request.email().trim().toLowerCase();

        if (userRepository.existsByEmail(email)) {
            throw new EmailAlreadyExistsException("Email đã được sử dụng: " + email);
        }

        User user = User.builder()
                .email(email)
                .password(passwordEncoder.encode(request.password()))
                .fullName(request.fullName().trim())
                .roles(EnumSet.of(Role.USER))
                .status(User.UserStatus.ACTIVE)
                .build();

        try {
            userRepository.saveAndFlush(user);
        } catch (DataIntegrityViolationException e) {
            // Thua cuộc đua với một request đăng ký cùng email.
            throw new EmailAlreadyExistsException("Email đã được sử dụng: " + email);
        }

        // Phát token ngay để client không phải gọi login thêm một lần. Dựng
        // principal từ entity vừa lưu thay vì gọi lại AuthenticationManager:
        // mật khẩu vừa do chính ta hash, không có gì để xác thực lại.
        return buildResponse(CustomUserDetails.from(user));
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

        return buildResponse((CustomUserDetails) authentication.getPrincipal());
    }

    private LoginResponse buildResponse(CustomUserDetails principal) {
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
