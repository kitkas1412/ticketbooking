package me.kitkas1412.ticketbooking.config;

import me.kitkas1412.ticketbooking.entity.Role;
import me.kitkas1412.ticketbooking.security.JwtAuthenticationFilter;
import me.kitkas1412.ticketbooking.security.JwtProperties;
import me.kitkas1412.ticketbooking.security.JwtService;
import me.kitkas1412.ticketbooking.security.RestAuthErrorHandler;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.factory.PasswordEncoderFactories;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
@EnableConfigurationProperties(JwtProperties.class)
public class SecurityConfig {

    /**
     * Dùng {@code DelegatingPasswordEncoder}: hash lưu xuống DB mang tiền tố
     * thuật toán, ví dụ {@code {bcrypt}$2a$10$...}.
     *
     * <p>Tiền tố này là thứ cho phép đổi thuật toán sau này mà không phải reset
     * mật khẩu toàn bộ user: đổi encoder mặc định thì user mới hash bằng thuật
     * toán mới, user cũ vẫn verify được vì tiền tố chỉ ra encoder cần dùng.
     * {@code BCryptPasswordEncoder} trần không có đường lùi đó.
     *
     * <p>Hệ quả cần nhớ: mọi hash ghi vào cột {@code users.password} phải đi qua
     * bean này. Hash BCrypt trần (bắt đầu bằng {@code $2a$}) sẽ bị coi là
     * "unmapped id" và ném {@code IllegalArgumentException} lúc verify, kể cả khi
     * mật khẩu đúng — đây là lỗi hay gặp khi seed user bằng SQL thủ công.
     *
     * <p>Mặc định của factory là bcrypt strength 10.
     */
    @Bean
    public PasswordEncoder passwordEncoder() {
        return PasswordEncoderFactories.createDelegatingPasswordEncoder();
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http,
                                                   JwtService jwtService,
                                                   RestAuthErrorHandler authErrorHandler) throws Exception {
        return http
                // API stateless dùng Bearer token: không có cookie phiên nên
                // không tồn tại vector CSRF mà token CSRF sinh ra để chặn.
                .csrf(AbstractHttpConfigurer::disable)

                // Tắt hai cơ chế mặc định của Spring Boot. Không tắt thì lỗi 401
                // sẽ trả về redirect tới trang login hoặc header WWW-Authenticate,
                // thay vì JSON.
                .formLogin(AbstractHttpConfigurer::disable)
                .httpBasic(AbstractHttpConfigurer::disable)

                // Không tạo HttpSession. Bắt buộc với JWT: nếu để mặc định,
                // Spring lưu SecurityContext vào session và request thứ hai sẽ
                // được xác thực bằng cookie chứ không phải token.
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))

                .exceptionHandling(ex -> ex
                        .authenticationEntryPoint(authErrorHandler)
                        .accessDeniedHandler(authErrorHandler))

                .authorizeHttpRequests(auth -> auth
                        // Đăng ký / đăng nhập phải mở, nếu không sẽ không có
                        // đường nào lấy được token đầu tiên.
                        .requestMatchers("/api/auth/**").permitAll()

                        // Xem danh sách / chi tiết event là công khai.
                        .requestMatchers(HttpMethod.GET, "/api/events/**").permitAll()

                        // Tạo event: chỉ ADMIN. hasRole("ADMIN") so khớp với
                        // authority "ROLE_ADMIN" do Role.getAuthority() sinh ra.
                        .requestMatchers(HttpMethod.POST, "/api/events").hasRole(Role.ADMIN.name())

                        // Mua vé và tra cứu order: cần đăng nhập.
                        .requestMatchers(HttpMethod.POST, "/api/events/*/buy").authenticated()
                        .requestMatchers("/api/orders/**").authenticated()

                        .anyRequest().authenticated())

                // Cố ý new trực tiếp thay vì khai báo @Bean: Spring Boot tự đăng
                // ký mọi bean kiểu Filter vào servlet filter chain, khiến filter
                // chạy thêm một lần nữa ở ngoài chuỗi của Spring Security.
                .addFilterBefore(new JwtAuthenticationFilter(jwtService),
                        UsernamePasswordAuthenticationFilter.class)

                .build();
    }
}
