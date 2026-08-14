package me.kitkas1412.ticketbooking.security;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import java.time.Duration;

/**
 * Cấu hình JWT, bind từ prefix {@code app.jwt}.
 *
 * <p>{@code secret} là chuỗi Base64 của khoá HMAC — phải giải mã ra tối thiểu
 * 256 bit (44 ký tự Base64) cho HS256, nếu không {@link JwtService} sẽ ném
 * {@code WeakKeyException} ngay lúc khởi động.
 */
@Validated
@ConfigurationProperties(prefix = "app.jwt")
public record JwtProperties(

        @NotBlank
        String secret,

        @NotBlank
        String issuer,

        @NotNull
        Duration accessTokenTtl
) {
}
