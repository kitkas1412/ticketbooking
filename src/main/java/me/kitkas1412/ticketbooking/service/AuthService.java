package me.kitkas1412.ticketbooking.service;

import me.kitkas1412.ticketbooking.dto.request.LoginRequest;
import me.kitkas1412.ticketbooking.dto.response.LoginResponse;

public interface AuthService {

    LoginResponse login(LoginRequest request);
}
