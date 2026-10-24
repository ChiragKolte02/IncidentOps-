package com.incidentops.user_service.dto;

import com.incidentops.user_service.enums.Role;
import com.incidentops.user_service.enums.Team;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class AuthResponse {

    private String token;
    private String tokenType;
    private Long userId;
    private String username;
    private Role role;
    private Team team;
}