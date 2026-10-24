package com.incidentops.user_service.service.impl;

import com.incidentops.user_service.dto.CreateUserRequest;
import com.incidentops.user_service.dto.UserResponse;
import com.incidentops.user_service.entity.User;
import com.incidentops.user_service.enums.Role;
import com.incidentops.user_service.enums.Team;
import com.incidentops.user_service.repository.UserRepository;
import com.incidentops.user_service.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    @Transactional
    public UserResponse createTeamMember(CreateUserRequest request) {

        if (userRepository.existsByUsername(request.getUsername())) {
            throw new IllegalArgumentException("Username already exists");
        }

        if (request.getTeam() == null || request.getTeam() == Team.NONE) {
            throw new IllegalArgumentException("Team must be UNIX or DBA");
        }

        User user = new User();
        user.setUsername(request.getUsername());
        user.setPasswordHash(passwordEncoder.encode(request.getPassword()));
        user.setRole(Role.TEAM_MEMBER);
        user.setTeam(request.getTeam());
        user.setEnabled(true);

        User savedUser = userRepository.save(user);

        return toResponse(savedUser);
    }

    @Override
    @Transactional(readOnly = true)
    public List<UserResponse> getAllUsers() {
        return userRepository.findAll()
                .stream()
                .map(this::toResponse)
                .toList();
    }

    private UserResponse toResponse(User user) {
        return new UserResponse(
                user.getId(),
                user.getUsername(),
                user.getRole(),
                user.getTeam(),
                user.isEnabled(),
                user.getCreatedAt()
        );
    }
    @Override
    @Transactional
    public void deleteUser(Long userId, String adminUsername) {

        User admin = userRepository.findByUsername(adminUsername)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.UNAUTHORIZED, "Admin account not found"));

        User target = userRepository.findById(userId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "User not found"));

        if (admin.getId().equals(target.getId())) {
            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN, "You cannot delete your own account");
        }

        if (target.getRole() != Role.TEAM_MEMBER) {
            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN,
                    "Only team-member accounts can be deleted");
        }

        userRepository.delete(target);
    }
}