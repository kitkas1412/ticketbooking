package me.kitkas1412.ticketbooking.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirements;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import me.kitkas1412.ticketbooking.dto.request.LoginRequest;
import me.kitkas1412.ticketbooking.dto.request.RegisterRequest;
import me.kitkas1412.ticketbooking.dto.response.ApiResponse;
import me.kitkas1412.ticketbooking.dto.response.LoginResponse;
import me.kitkas1412.ticketbooking.service.AuthService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

// Lưu ý khi đọc file: annotation @ApiResponse của Swagger trùng tên với DTO
// ApiResponse của dự án. DTO xuất hiện trong mọi chữ ký hàm nên nó giữ import,
// còn annotation phải viết đầy đủ package — Java không có alias import.
@RestController
@RequestMapping("/api/auth")
@Tag(name = "Auth", description = "Đăng ký tài khoản và lấy access token")
// Hai endpoint dưới đây là cửa vào của hệ thống, chưa thể có token. Gỡ yêu cầu
// bảo mật mặc định khai báo ở OpenApiConfig để Swagger UI không hiện ổ khoá.
@SecurityRequirements
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @Operation(
            summary = "Đăng ký tài khoản mới",
            description = """
                    Tạo tài khoản với vai trò USER và trả luôn access token, client không
                    phải gọi thêm /login. Vai trò không nhận từ client — mọi tài khoản
                    đăng ký qua API đều là USER.
                    """)
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "201", description = "Đăng ký thành công, kèm access token"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "400", description = "Dữ liệu không hợp lệ (email sai định dạng, mật khẩu dưới 8 ký tự...)"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "409", description = "Email đã được đăng ký")
    })
    @PostMapping("/register")
    public ResponseEntity<ApiResponse<LoginResponse>> register(@Valid @RequestBody RegisterRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(authService.register(request)));
    }

    @Operation(
            summary = "Đăng nhập",
            description = """
                    Trả về access token dạng JWT. Copy `data.accessToken` rồi dán vào nút
                    **Authorize** ở đầu trang để gọi được các endpoint cần đăng nhập.
                    """)
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200", description = "Đăng nhập thành công"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "401", description = "Email hoặc mật khẩu không đúng")
    })
    @PostMapping("/login")
    public ResponseEntity<ApiResponse<LoginResponse>> login(@Valid @RequestBody LoginRequest request) {
        return ResponseEntity.ok(ApiResponse.success(authService.login(request)));
    }
}
