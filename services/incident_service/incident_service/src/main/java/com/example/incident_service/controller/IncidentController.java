
package com.example.incident_service.controller;

import com.example.incident_service.dto.IncidentResponse;
import com.example.incident_service.entity.Alert;
import com.example.incident_service.entity.Incident;
import com.example.incident_service.repository.AlertRepository;
import com.example.incident_service.repository.IncidentRepository;
import com.example.incident_service.security.IncidentUserPrincipal;
import com.example.incident_service.service.AiInvestigationService;

import tools.jackson.databind.JsonNode;

import jakarta.transaction.Transactional;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/api/incidents")
public class IncidentController {

    private final IncidentRepository incidentRepository;
    private final AlertRepository alertRepository;
    private final AiInvestigationService aiInvestigationService;

    public IncidentController(
            IncidentRepository incidentRepository,
            AlertRepository alertRepository,
            AiInvestigationService aiInvestigationService
    ) {
        this.incidentRepository = incidentRepository;
        this.alertRepository = alertRepository;
        this.aiInvestigationService = aiInvestigationService;
    }

    // --------------------------------------------------
    // GET ALL INCIDENTS
    // --------------------------------------------------

    @GetMapping
    public List<IncidentResponse> getAllIncidents(
            @AuthenticationPrincipal IncidentUserPrincipal user
    ) {

        List<Incident> incidents =
                user.role().equals("ADMIN")
                        ? incidentRepository.findAll()
                        : incidentRepository.findByTeamIgnoreCase(
                        user.team()
                );

        return incidents.stream()
                .map(this::toResponse)
                .toList();
    }

    // --------------------------------------------------
    // GET INCIDENT BY ID
    // --------------------------------------------------

    @GetMapping("/{id}")
    public ResponseEntity<IncidentResponse> getIncidentById(
            @PathVariable Long id,
            @AuthenticationPrincipal IncidentUserPrincipal user
    ) {

        return findVisibleIncident(id, user)
                .map(this::toResponse)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    // --------------------------------------------------
    // GET OPEN INCIDENTS
    // --------------------------------------------------

    @GetMapping("/open")
    public List<IncidentResponse> getOpenIncidents(
            @AuthenticationPrincipal IncidentUserPrincipal user
    ) {

        List<Incident> incidents =
                user.role().equals("ADMIN")
                        ? incidentRepository.findByStatus("OPEN")
                        : incidentRepository.findByStatusAndTeamIgnoreCase(
                        "OPEN",
                        user.team()
                );

        return incidents.stream()
                .map(this::toResponse)
                .toList();
    }

    // --------------------------------------------------
    // CLAIM INCIDENT
    // --------------------------------------------------

    @PostMapping("/{id}/claim")
    @Transactional
    public ResponseEntity<IncidentResponse> claimIncident(
            @PathVariable Long id,
            @AuthenticationPrincipal IncidentUserPrincipal user
    ) {

        Incident incident = findVisibleIncident(id, user)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Incident not found"
                ));

        if (!"OPEN".equalsIgnoreCase(incident.getStatus())) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Only OPEN incidents can be claimed"
            );
        }

        if (incident.getOwnerUserId() != null) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Incident is already claimed"
            );
        }

        int updated;

        if (user.role().equals("ADMIN")) {
            updated = incidentRepository.claimAnyIncident(
                    id,
                    user.userId(),
                    user.username()
            );
        } else {
            updated = incidentRepository.claimTeamIncident(
                    id,
                    user.userId(),
                    user.username(),
                    user.team()
            );
        }

        if (updated != 1) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Incident was claimed by another user or is no longer OPEN"
            );
        }

        Incident claimed = incidentRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Incident not found"
                ));

        return ResponseEntity.ok(toResponse(claimed));
    }

    // --------------------------------------------------
    // AI INVESTIGATION
    // --------------------------------------------------

    @PostMapping("/{id}/investigate")
    public ResponseEntity<JsonNode> investigateIncident(
            @PathVariable Long id,
            @AuthenticationPrincipal IncidentUserPrincipal user
    ) {

        // 1. Verify incident visibility and team authorization.
        Incident incident = findVisibleIncident(id, user)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Incident not found"
                ));

        // 2. Retrieve the exact firing alert for new incidents.
        Alert firingAlert = findSourceFiringAlert(incident);

        // 3. Validate the alert timestamp.
        if (firingAlert.getStartsAt() == null
                || firingAlert.getStartsAt().isBlank()) {

            throw new ResponseStatusException(
                    HttpStatus.UNPROCESSABLE_ENTITY,
                    "Original alert timestamp is missing"
            );
        }

        // 4. Normalize the alert timestamp to UTC.
        Instant startsAt = parseAlertTimestamp(
                firingAlert.getStartsAt()
        );

        // 5. Validate monitoring information.
        if (incident.getTeam() == null
                || incident.getTeam().isBlank()
                || incident.getInstance() == null
                || incident.getInstance().isBlank()) {

            throw new ResponseStatusException(
                    HttpStatus.UNPROCESSABLE_ENTITY,
                    "Incident monitoring information is incomplete"
            );
        }

        if (incident.getIncidentNumber() == null
                || incident.getIncidentNumber().isBlank()
                || firingAlert.getAlertName() == null
                || firingAlert.getAlertName().isBlank()) {

            throw new ResponseStatusException(
                    HttpStatus.UNPROCESSABLE_ENTITY,
                    "Incident or alert identification is incomplete"
            );
        }

        // 6. Send the correct alert data to the AI Agent.
        JsonNode report = aiInvestigationService.investigate(
                incident.getIncidentNumber(),
                firingAlert.getAlertName(),
                incident.getTeam(),
                incident.getInstance(),
                startsAt
        );

        return ResponseEntity.ok(report);
    }

    // --------------------------------------------------
    // FIND SOURCE FIRING ALERT
    // --------------------------------------------------

    private Alert findSourceFiringAlert(Incident incident) {

        Long sourceAlertId = incident.getSourceAlertId();

        // New incidents have an exact source alert ID.
        if (sourceAlertId != null) {

            Alert sourceAlert = alertRepository.findById(sourceAlertId)
                    .orElseThrow(() -> new ResponseStatusException(
                            HttpStatus.UNPROCESSABLE_ENTITY,
                            "Source firing alert not found"
                    ));

            if (!"firing".equalsIgnoreCase(sourceAlert.getStatus())) {

                throw new ResponseStatusException(
                        HttpStatus.UNPROCESSABLE_ENTITY,
                        "Source alert is not a firing alert"
                );
            }

            if (!incident.getFingerprint().equals(
                    sourceAlert.getFingerprint())) {

                throw new ResponseStatusException(
                        HttpStatus.UNPROCESSABLE_ENTITY,
                        "Source alert fingerprint does not match incident"
                );
            }

            return sourceAlert;
        }

        // Legacy incidents created before sourceAlertId existed.
        // Their createdAt field contains the original alert timestamp.
        if (incident.getCreatedAt() == null
                || incident.getCreatedAt().isBlank()) {

            throw new ResponseStatusException(
                    HttpStatus.UNPROCESSABLE_ENTITY,
                    "Legacy incident has no original alert timestamp"
            );
        }

        Instant incidentStart = parseAlertTimestamp(
                incident.getCreatedAt()
        );

        List<Alert> firingAlerts = alertRepository
                .findByFingerprintAndStatusIgnoreCaseOrderByIdDesc(
                        incident.getFingerprint(),
                        "firing"
                );

        // Match the original event timestamp, not just fingerprint.
        for (Alert alert : firingAlerts) {

            if (alert.getStartsAt() == null
                    || alert.getStartsAt().isBlank()) {
                continue;
            }

            try {
                Instant alertStart = parseAlertTimestamp(
                        alert.getStartsAt()
                );

                if (incidentStart.equals(alertStart)) {
                    return alert;
                }

            } catch (ResponseStatusException ignored) {
                // Skip malformed legacy alert timestamps.
            }
        }

        throw new ResponseStatusException(
                HttpStatus.UNPROCESSABLE_ENTITY,
                "No firing alert matching this incident occurrence was found"
        );
    }

    // --------------------------------------------------
    // TIMESTAMP NORMALIZATION
    // --------------------------------------------------

    private Instant parseAlertTimestamp(String rawTimestamp) {

        String value = rawTimestamp.trim();

        // Standard ISO-8601 timestamp.
        try {
            return Instant.parse(value);
        } catch (DateTimeParseException ignored) {
        }

        // PostgreSQL timestamp with explicit UTC offset.
        // Example: 2026-09-24 14:57:59.457659+00
        try {

            String normalized = value.replaceFirst(
                    "^(\\d{4}-\\d{2}-\\d{2})\\s+",
                    "$1T"
            );

            normalized = normalized.replaceFirst(
                    "([+-]\\d{2})$",
                    "$1:00"
            );

            return OffsetDateTime.parse(normalized).toInstant();

        } catch (DateTimeParseException ignored) {
        }

        throw new ResponseStatusException(
                HttpStatus.UNPROCESSABLE_ENTITY,
                "Unsupported or invalid alert timestamp format: " + value
        );
    }

    // --------------------------------------------------
    // TEAM AUTHORIZATION
    // --------------------------------------------------

    private Optional<Incident> findVisibleIncident(
            Long id,
            IncidentUserPrincipal user
    ) {

        if (user.role().equals("ADMIN")) {
            return incidentRepository.findById(id);
        }

        return incidentRepository.findByIdAndTeamIgnoreCase(
                id,
                user.team()
        );
    }

    // --------------------------------------------------
    // INCIDENT RESPONSE
    // --------------------------------------------------

    private IncidentResponse toResponse(Incident incident) {

        return new IncidentResponse(
                incident.getId(),
                incident.getIncidentNumber(),
                incident.getTitle(),
                incident.getStatus(),
                incident.getSeverity(),
                incident.getTeam(),
                incident.getInstance(),
                incident.getFingerprint(),
                incident.getCreatedAt(),
                incident.getOwnerUserId(),
                incident.getOwnerUsername()
        );
    }
}
