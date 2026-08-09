package com.relivio.auth.service;

import com.relivio.auth.dto.AuthResponse;
import com.relivio.auth.dto.LoginRequest;
import com.relivio.auth.dto.RegisterRequest;
import com.relivio.auth.dto.UserResponse;
import com.relivio.auth.entity.User;

import java.util.List;

public interface AuthService {

    AuthResponse register(RegisterRequest request);

    AuthResponse registerVolunteer(RegisterRequest request);

    AuthResponse login(LoginRequest request);

    UserResponse getById(Long id);

    User getByIdentity(String phone);

    void logout(String token);

    List<UserResponse> getAllUsers();
}
