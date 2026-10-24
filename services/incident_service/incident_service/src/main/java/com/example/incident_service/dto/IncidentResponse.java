
package com.example.incident_service.dto;

public record IncidentResponse(
        Long id,
        String incidentNumber,
        String title,
        String status,
        String severity,
        String team,
        String instance,
        String fingerprint,
        String createdAt,
        Long ownerUserId,
        String ownerUsername
) {
}
