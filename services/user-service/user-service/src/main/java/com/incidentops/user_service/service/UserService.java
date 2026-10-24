package com.incidentops.user_service.service;

import com.incidentops.user_service.dto.CreateUserRequest;
import com.incidentops.user_service.dto.UserResponse;

import java.util.List;

public interface UserService {

    UserResponse createTeamMember(CreateUserRequest request);

    List<UserResponse> getAllUsers();
    void deleteUser(Long userId, String adminUsername);

}