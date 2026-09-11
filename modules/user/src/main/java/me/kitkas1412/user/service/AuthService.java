package me.kitkas1412.user.service;


import me.kitkas1412.user.dto.request.LoginRequest;
import me.kitkas1412.user.dto.request.RegisterRequest;
import me.kitkas1412.user.dto.response.LoginResponse;

public interface AuthService {

    LoginResponse login(LoginRequest request);

    /** Tạo tài khoản mới với vai trò USER và trả luôn access token. */
    LoginResponse register(RegisterRequest request);
}
