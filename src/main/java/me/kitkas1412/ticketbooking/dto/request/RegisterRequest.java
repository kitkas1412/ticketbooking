package me.kitkas1412.ticketbooking.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Cố ý không có trường {@code role}: nếu client tự chọn được vai trò thì bất kỳ
 * ai cũng đăng ký được một tài khoản ADMIN. Mọi tài khoản đăng ký qua API đều
 * nhận mặc định USER; việc nâng quyền phải làm qua đường khác.
 */
@Schema(description = "Dữ liệu đăng ký tài khoản mới. Không có trường vai trò — "
        + "mọi tài khoản tạo qua API đều là USER.")
public record RegisterRequest(

        @Schema(description = "Email dùng làm định danh đăng nhập, không được trùng",
                example = "user@example.com", maxLength = 255)
        @NotBlank(message = "Email không được để trống")
        @Email(message = "Email không hợp lệ")
        @Size(max = 255, message = "Email tối đa 255 ký tự")
        String email,

        // Chặn trên 72: BCrypt cắt cụt đầu vào ở 72 byte, ký tự thứ 73 trở đi
        // không ảnh hưởng gì tới hash. Nhận mật khẩu dài hơn là tạo cảm giác
        // an toàn giả.
        @Schema(description = "Mật khẩu, 8–72 ký tự. Giới hạn trên là do BCrypt cắt cụt đầu vào ở 72 byte.",
                example = "MatKhau123", minLength = 8, maxLength = 72)
        @NotBlank(message = "Mật khẩu không được để trống")
        @Size(min = 8, max = 72, message = "Mật khẩu phải từ 8 đến 72 ký tự")
        String password,

        @Schema(description = "Họ tên hiển thị", example = "Nguyễn Văn A", maxLength = 100)
        @NotBlank(message = "Họ tên không được để trống")
        @Size(max = 100, message = "Họ tên tối đa 100 ký tự")
        String fullName) {
}
