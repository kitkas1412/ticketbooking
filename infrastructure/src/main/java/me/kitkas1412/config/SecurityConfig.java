package me.kitkas1412.config;

import jakarta.servlet.Filter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.ProviderManager;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.factory.PasswordEncoderFactories;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.Arrays;
import java.util.List;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class SecurityConfig {



    @Value("${app.security.cors.allowed-origins:http://localhost:3000,http://localhost:5173}")
    private String allowedOriginsRaw;

    @Value("${app.security.swagger.public:true}")
    private boolean swaggerPublic;

    @Value("${app.security.cors.allow-all:false}")
    private boolean corsAllowAll;

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

    /**
     * Dựng {@code AuthenticationManager} tường minh thay vì lấy qua
     * {@code AuthenticationConfiguration}: chỉ có đúng một provider, và nhìn vào
     * đây là thấy ngay mật khẩu được kiểm bằng UserDetailsService nào với
     * encoder nào.
     */
    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration configuration) {
        return configuration.getAuthenticationManager();
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http,
                                                   Filter jwtAuthenticationFilter) throws Exception {
        return http
                // API stateless dùng Bearer token: không có cookie phiên nên
                // không tồn tại vector CSRF mà token CSRF sinh ra để chặn.
                .csrf(AbstractHttpConfigurer::disable)
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))

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
                        .authenticationEntryPoint((request, response, authException) -> {
                            response.setContentType("application/json;charset=UTF-8");
                            response.setStatus(401);
                            response.getWriter().write(
                                    "{\"success\":false,\"code\":\"UNAUTHORIZED\",\"message\":\"Vui lòng đăng nhập để tiếp tục\"}"
                            );
                        })
                        .accessDeniedHandler((request, response, accessDeniedException) -> {
                            response.setContentType("application/json;charset=UTF-8");
                            response.setStatus(403);
                            response.getWriter().write(
                                    "{\"success\":false,\"code\":\"FORBIDDEN\",\"message\":\"Bạn không có quyền truy cập tài nguyên này\"}"
                            );
                        }))

                .authorizeHttpRequests(auth -> auth
                        // Đăng ký / đăng nhập phải mở, nếu không sẽ không có
                        // đường nào lấy được token đầu tiên.
                        .requestMatchers("/api/auth/**").permitAll()

                        // Swagger UI + JSON đặc tả OpenAPI. Trang /swagger-ui.html
                        // chỉ redirect sang /swagger-ui/index.html nên phải mở cả hai;
                        // thiếu /v3/api-docs/** thì UI load được nhưng trắng trang.
                        // Lưu ý: mở công khai đồng nghĩa với công khai toàn bộ danh
                        // sách endpoint. Ở production nên đóng lại bằng
                        // springdoc.api-docs.enabled=false / springdoc.swagger-ui.enabled=false,
                        // hoặc đổi permitAll() thành hasRole(ADMIN).
                        .requestMatchers(
                                "/swagger-ui.html",
                                "/swagger-ui/**",
                                "/v3/api-docs",
                                "/v3/api-docs/**").permitAll()

                        // Xem danh sách / chi tiết event là công khai.
                        .requestMatchers(HttpMethod.GET, "/api/events/**").permitAll()

                        // Tạo event: chỉ ADMIN. hasRole("ADMIN") so khớp với
                        // authority "ROLE_ADMIN" do Role.getAuthority() sinh ra.
                        .requestMatchers(HttpMethod.POST, "/api/events").hasRole("ADMIN")

                        // Mua vé và tra cứu order: cần đăng nhập.
                        .requestMatchers(HttpMethod.POST, "/api/events/*/buy").authenticated()
                        .requestMatchers("/api/orders/**").authenticated()

                        .anyRequest().authenticated())

                // Cố ý new trực tiếp thay vì khai báo @Bean: Spring Boot tự đăng
                // ký mọi bean kiểu Filter vào servlet filter chain, khiến filter
                // chạy thêm một lần nữa ở ngoài chuỗi của Spring Security.
                .addFilterBefore(jwtAuthenticationFilter,
                        UsernamePasswordAuthenticationFilter.class)

                .build();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();

        if (corsAllowAll) {
            // Dev/local only: allow any origin.
            // Must use allowedOriginPatterns (not allowedOrigins) to be compatible with allowCredentials=true.
            configuration.setAllowedOriginPatterns(List.of("*"));
        } else {
            // Prod/staging: explicit whitelist only.
            List<String> origins = Arrays.stream(allowedOriginsRaw.split(","))
                    .map(String::trim)
                    .filter(s -> !s.isEmpty())
                    .toList();
            configuration.setAllowedOrigins(origins);
        }

        configuration.setAllowedMethods(Arrays.asList("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
        configuration.setAllowedHeaders(Arrays.asList("Authorization", "Content-Type", "X-Requested-With", "Accept"));
        configuration.setExposedHeaders(Arrays.asList("X-Token-Expired", "Authorization"));
        configuration.setAllowCredentials(true);
        configuration.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }
}
