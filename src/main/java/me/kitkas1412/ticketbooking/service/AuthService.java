package me.kitkas1412.ticketbooking.service;

import me.kitkas1412.ticketbooking.dto.request.LoginRequest;
import me.kitkas1412.ticketbooking.dto.request.RegisterRequest;
import me.kitkas1412.ticketbooking.dto.response.LoginResponse;

public interface AuthService {

    LoginResponse login(LoginRequest request);

    /** Tạo tài khoản mới với vai trò USER và trả luôn access token. */
    LoginResponse register(RegisterRequest request);
}
