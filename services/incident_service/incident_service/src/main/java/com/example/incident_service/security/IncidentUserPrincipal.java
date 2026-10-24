
package com.example.incident_service.security;

public record IncidentUserPrincipal(
        Long userId,
        String username,
        String role,
        String team
) {
}
