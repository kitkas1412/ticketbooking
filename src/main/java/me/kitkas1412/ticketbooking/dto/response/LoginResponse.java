package me.kitkas1412.ticketbooking.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

/**
 * @param expiresIn thời gian sống của access token, tính bằng giây — lấy từ
 *                  cấu hình chứ không hardcode, để client và server không lệch
 *                  nhau khi đổi {@code app.jwt.access-token-ttl}.
 */
@Schema(description = "Access token và thông tin tài khoản sau khi đăng nhập / đăng ký")
public record LoginResponse(

        @Schema(description = "JWT dán vào nút Authorize hoặc gửi kèm header Authorization",
                example = "eyJhbGciOiJIUzI1NiJ9.eyJzdWIiOiJ1c2VyQGV4YW1wbGUuY29tIn0...")
        String accessToken,

        @Schema(description = "Loại token, luôn là Bearer", example = "Bearer")
        String tokenType,

        @Schema(description = "Thời gian sống còn lại của token, tính bằng giây", example = "900")
        long expiresIn,

        @Schema(description = "Email của tài khoản", example = "user@example.com")
        String email,

        @Schema(description = "Danh sách vai trò", example = "[\"USER\"]")
        List<String> roles) {
}
