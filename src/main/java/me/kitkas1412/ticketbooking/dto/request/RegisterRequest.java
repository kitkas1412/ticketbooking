package me.kitkas1412.ticketbooking.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Cố ý không có trường {@code role}: nếu client tự chọn được vai trò thì bất kỳ
 * ai cũng đăng ký được một tài khoản ADMIN. Mọi tài khoản đăng ký qua API đều
 * nhận mặc định USER; việc nâng quyền phải làm qua đường khác.
 */
public record RegisterRequest(

        @NotBlank(message = "Email không được để trống")
        @Email(message = "Email không hợp lệ")
        @Size(max = 255, message = "Email tối đa 255 ký tự")
        String email,

        // Chặn trên 72: BCrypt cắt cụt đầu vào ở 72 byte, ký tự thứ 73 trở đi
        // không ảnh hưởng gì tới hash. Nhận mật khẩu dài hơn là tạo cảm giác
        // an toàn giả.
        @NotBlank(message = "Mật khẩu không được để trống")
        @Size(min = 8, max = 72, message = "Mật khẩu phải từ 8 đến 72 ký tự")
        String password,

        @NotBlank(message = "Họ tên không được để trống")
        @Size(max = 100, message = "Họ tên tối đa 100 ký tự")
        String fullName) {
}
