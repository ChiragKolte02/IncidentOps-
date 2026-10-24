package com.example.incident_service.service;

import com.example.incident_service.dto.AnalyticsOverviewResponse;
import com.example.incident_service.entity.Alert;
import com.example.incident_service.entity.Incident;
import com.example.incident_service.repository.AlertRepository;
import com.example.incident_service.repository.IncidentRepository;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

@Service
public class AnalyticsService {

    private static final List<String> ACTIVE_STATUSES =
            List.of("OPEN", "IN_PROGRESS");

    private final AlertRepository alertRepository;
    private final IncidentRepository incidentRepository;

    public AnalyticsService(
            AlertRepository alertRepository,
            IncidentRepository incidentRepository
    ) {
        this.alertRepository = alertRepository;
        this.incidentRepository = incidentRepository;
    }

    public AnalyticsOverviewResponse getOverview(
            String requestedRange,
            String team
    ) {
        String range = normalizeRange(requestedRange);

        Instant now = Instant.now();
        boolean hourly = "24h".equals(range);

        // Determine the first bucket in the selected period.
        Instant firstBucket;

        if (hourly) {
            // Current hour plus the previous 23 hours.
            firstBucket = now.truncatedTo(ChronoUnit.HOURS)
                    .minus(23, ChronoUnit.HOURS);
        } else {
            int days = "7d".equals(range) ? 7 : 30;

            // Start at midnight UTC for the first displayed day.
            firstBucket = now.atZone(ZoneOffset.UTC)
                    .toLocalDate()
                    .atStartOfDay(ZoneOffset.UTC)
                    .toInstant()
                    .minus(days - 1L, ChronoUnit.DAYS);
        }

        // Fetch alerts, with team filtering for non-admin users.
        List<Alert> alerts = team == null
                ? alertRepository.findByReceivedAtGreaterThanEqual(
                firstBucket
        )
                : alertRepository
                .findByTeamIgnoreCaseAndReceivedAtGreaterThanEqual(
                        team,
                        firstBucket
                );

        // Fetch incidents opened during the selected period.
        List<Incident> createdIncidents = team == null
                ? incidentRepository.findByOpenedAtGreaterThanEqual(
                firstBucket
        )
                : incidentRepository
                .findByTeamIgnoreCaseAndOpenedAtGreaterThanEqual(
                        team,
                        firstBucket
                );

        // Fetch incidents resolved during the selected period.
        List<Incident> resolvedIncidents = team == null
                ? incidentRepository.findByResolvedAtGreaterThanEqual(
                firstBucket
        )
                : incidentRepository
                .findByTeamIgnoreCaseAndResolvedAtGreaterThanEqual(
                        team,
                        firstBucket
                );

        List<Instant> buckets = buildBuckets(
                firstBucket,
                now,
                hourly
        );

        // Each alert bucket stores:
        // index 0 = firing, index 1 = resolved.
        Map<Instant, long[]> alertCounts = new HashMap<>();

        // Each incident bucket stores:
        // index 0 = created, index 1 = resolved.
        Map<Instant, long[]> incidentCounts = new HashMap<>();

        for (Instant bucket : buckets) {
            alertCounts.put(bucket, new long[2]);
            incidentCounts.put(bucket, new long[2]);
        }

        // Aggregate alert events.
        for (Alert alert : alerts) {
            Instant receivedAt = alert.getReceivedAt();

            // Old rows may not have this timestamp.
            if (receivedAt == null) {
                continue;
            }

            Instant bucket = bucketFor(receivedAt, hourly);
            long[] counts = alertCounts.get(bucket);

            if (counts == null) {
                continue;
            }

            if ("firing".equalsIgnoreCase(alert.getStatus())) {
                counts[0]++;
            } else if (
                    "resolved".equalsIgnoreCase(alert.getStatus())
            ) {
                counts[1]++;
            }
        }

        // Aggregate incident creation events.
        for (Incident incident : createdIncidents) {
            Instant openedAt = incident.getOpenedAt();

            if (openedAt == null) {
                continue;
            }

            long[] counts = incidentCounts.get(
                    bucketFor(openedAt, hourly)
            );

            if (counts != null) {
                counts[0]++;
            }
        }

        // Aggregate incident resolution events.
        for (Incident incident : resolvedIncidents) {
            Instant resolvedAt = incident.getResolvedAt();

            if (resolvedAt == null) {
                continue;
            }

            long[] counts = incidentCounts.get(
                    bucketFor(resolvedAt, hourly)
            );

            if (counts != null) {
                counts[1]++;
            }
        }

        // Convert the aggregated data into response records.
        List<AnalyticsOverviewResponse.AlertPoint> alertPoints =
                new ArrayList<>();

        List<AnalyticsOverviewResponse.IncidentPoint> incidentPoints =
                new ArrayList<>();

        for (Instant bucket : buckets) {
            long[] alertValues = alertCounts.get(bucket);
            long[] incidentValues = incidentCounts.get(bucket);

            alertPoints.add(
                    new AnalyticsOverviewResponse.AlertPoint(
                            bucket,
                            alertValues[0],
                            alertValues[1]
                    )
            );

            incidentPoints.add(
                    new AnalyticsOverviewResponse.IncidentPoint(
                            bucket,
                            incidentValues[0],
                            incidentValues[1]
                    )
            );
        }

        // Current open/in-progress incident count.
        long openIncidents = team == null
                ? incidentRepository.countByStatusIn(ACTIVE_STATUSES)
                : incidentRepository.countByTeamIgnoreCaseAndStatusIn(
                team,
                ACTIVE_STATUSES
        );

        return new AnalyticsOverviewResponse(
                range,
                alertPoints,
                incidentPoints,
                openIncidents
        );
    }

    private String normalizeRange(String requestedRange) {
        String range = requestedRange == null
                ? "24h"
                : requestedRange.trim().toLowerCase(Locale.ROOT);

        if (!List.of("24h", "7d", "30d").contains(range)) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Invalid range. Use 24h, 7d, or 30d."
            );
        }

        return range;
    }

    private List<Instant> buildBuckets(
            Instant firstBucket,
            Instant now,
            boolean hourly
    ) {
        List<Instant> buckets = new ArrayList<>();

        Instant lastBucket = hourly
                ? now.truncatedTo(ChronoUnit.HOURS)
                : now.atZone(ZoneOffset.UTC)
                .toLocalDate()
                .atStartOfDay(ZoneOffset.UTC)
                .toInstant();

        ChronoUnit unit = hourly
                ? ChronoUnit.HOURS
                : ChronoUnit.DAYS;

        Instant current = firstBucket;

        while (!current.isAfter(lastBucket)) {
            buckets.add(current);
            current = current.plus(1, unit);
        }

        return buckets;
    }

    private Instant bucketFor(
            Instant timestamp,
            boolean hourly
    ) {
        if (hourly) {
            return timestamp.truncatedTo(ChronoUnit.HOURS);
        }

        return timestamp.atZone(ZoneOffset.UTC)
                .toLocalDate()
                .atStartOfDay(ZoneOffset.UTC)
                .toInstant();
    }
}