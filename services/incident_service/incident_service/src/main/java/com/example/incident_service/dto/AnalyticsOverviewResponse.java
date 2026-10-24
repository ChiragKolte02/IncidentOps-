package com.example.incident_service.dto;

import java.time.Instant;
import java.util.List;

public record AnalyticsOverviewResponse(
        String range,
        List<AlertPoint> alerts,
        List<IncidentPoint> incidents,
        long openIncidents
) {
    public record AlertPoint(
            Instant time,
            long firing,
            long resolved
    ) {}

    public record IncidentPoint(
            Instant time,
            long created,
            long resolved
    ) {}
}