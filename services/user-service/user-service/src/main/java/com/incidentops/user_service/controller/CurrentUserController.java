
package com.incidentops.user_service.controller;

import com.incidentops.user_service.dto.UserResponse;
import com.incidentops.user_service.entity.User;
import com.incidentops.user_service.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class CurrentUserController {

    private final UserRepository userRepository;

    @GetMapping("/me")
    public UserResponse getCurrentUser(Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()) {
            throw new ResponseStatusException(
                    HttpStatus.UNAUTHORIZED, "Authentication required");
        }

        User user = userRepository.findByUsername(authentication.getName())
                .filter(User::isEnabled)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.UNAUTHORIZED, "User is unavailable"));

        return new UserResponse(
                user.getId(),
                user.getUsername(),
                user.getRole(),
                user.getTeam(),
                user.isEnabled(),
                user.getCreatedAt()
        );
    }
}
