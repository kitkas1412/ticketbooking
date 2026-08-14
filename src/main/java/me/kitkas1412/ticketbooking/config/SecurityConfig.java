package me.kitkas1412.ticketbooking.config;

import me.kitkas1412.ticketbooking.security.JwtProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.factory.PasswordEncoderFactories;
import org.springframework.security.crypto.password.PasswordEncoder;

/**
 * Cấu hình Spring Security. Hiện mới chỉ khai báo {@link PasswordEncoder};
 * {@code SecurityFilterChain} sẽ được thêm ở bước sau.
 */
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
}
