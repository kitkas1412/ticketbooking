package me.kitkas1412.ticketbooking.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

@Schema(description = "Thông tin đăng nhập")
public record LoginRequest(

        @Schema(description = "Email đã đăng ký", example = "user@example.com")
        @NotBlank(message = "Email không được để trống")
        @Email(message = "Email không hợp lệ")
        String email,

        @Schema(description = "Mật khẩu", example = "MatKhau123")
        @NotBlank(message = "Mật khẩu không được để trống")
        String password) {
}
