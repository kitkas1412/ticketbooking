package me.kitkas1412.ticketbooking.dto.response;

import java.util.List;

/**
 * @param expiresIn thời gian sống của access token, tính bằng giây — lấy từ
 *                  cấu hình chứ không hardcode, để client và server không lệch
 *                  nhau khi đổi {@code app.jwt.access-token-ttl}.
 */
public record LoginResponse(
        String accessToken,
        String tokenType,
        long expiresIn,
        String email,
        List<String> roles) {
}
