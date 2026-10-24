package com.incidentops.user_service.service;

import com.incidentops.user_service.dto.AuthResponse;
import com.incidentops.user_service.dto.LoginRequest;

public interface AuthService {
    AuthResponse login(LoginRequest request);
}