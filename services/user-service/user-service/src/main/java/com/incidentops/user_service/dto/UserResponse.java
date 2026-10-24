
package com.incidentops.user_service.dto;

import com.incidentops.user_service.enums.Role;
import com.incidentops.user_service.enums.Team;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@AllArgsConstructor
public class UserResponse {

    private Long id;
    private String username;
    private Role role;
    private Team team;
    private boolean enabled;
    private LocalDateTime createdAt;
}
