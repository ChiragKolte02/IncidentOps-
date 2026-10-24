package com.incidentops.alert_service.dto;



public record AlertEvent(
        String alertName,
        String status,
        String instance,
        String severity,
        String team,
        String fingerprint,
        String summary,
        String description,
        String startsAt
) {
}