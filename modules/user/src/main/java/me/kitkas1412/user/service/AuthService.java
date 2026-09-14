package me.kitkas1412.user.service;


import me.kitkas1412.user.dto.request.LoginRequest;
import me.kitkas1412.user.dto.request.RegisterRequest;
import me.kitkas1412.user.dto.response.LoginResponse;

/**
 * Interface đăng ký, đăng nhập và cấp access token.
 */
public interface AuthService {

    // Xác thực thông tin đăng nhập và phát access token.
    LoginResponse login(LoginRequest request);

    /** Tạo tài khoản mới với vai trò USER và trả luôn access token. */
    LoginResponse register(RegisterRequest request);
}
