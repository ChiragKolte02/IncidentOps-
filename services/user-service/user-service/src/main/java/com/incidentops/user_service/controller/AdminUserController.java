package com.incidentops.user_service.controller;

import com.incidentops.user_service.dto.CreateUserRequest;
import com.incidentops.user_service.dto.UserResponse;
import com.incidentops.user_service.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/admin/users")
@RequiredArgsConstructor
public class AdminUserController {

    private final UserService userService;

    @PostMapping
    public UserResponse createTeamMember(
            @Valid @RequestBody CreateUserRequest request
    ) {
        return userService.createTeamMember(request);
    }

    @GetMapping
    public List<UserResponse> getAllUsers() {
        return userService.getAllUsers();
    }
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteUser(
            @PathVariable Long id,
            Authentication authentication) {

        userService.deleteUser(id, authentication.getName());

        return ResponseEntity.noContent().build();
    }
}