
package com.example.incident_service.controller;

import com.example.incident_service.dto.IncidentResponse;
import com.example.incident_service.entity.Incident;
import com.example.incident_service.repository.IncidentRepository;
import com.example.incident_service.security.IncidentUserPrincipal;
import jakarta.transaction.Transactional;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@RestController
@RequestMapping("/api/incidents")
public class IncidentController {

    private final IncidentRepository incidentRepository;

    public IncidentController(IncidentRepository incidentRepository) {
        this.incidentRepository = incidentRepository;
    }

    @GetMapping
    public List<IncidentResponse> getAllIncidents(
            @AuthenticationPrincipal IncidentUserPrincipal user) {

        List<Incident> incidents =
                user.role().equals("ADMIN")
                        ? incidentRepository.findAll()
                        : incidentRepository.findByTeamIgnoreCase(user.team());

        return incidents.stream().map(this::toResponse).toList();
    }

    @GetMapping("/{id}")
    public ResponseEntity<IncidentResponse> getIncidentById(
            @PathVariable Long id,
            @AuthenticationPrincipal IncidentUserPrincipal user) {

        return findVisibleIncident(id, user)
                .map(this::toResponse)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/open")
    public List<IncidentResponse> getOpenIncidents(
            @AuthenticationPrincipal IncidentUserPrincipal user) {

        List<Incident> incidents =
                user.role().equals("ADMIN")
                        ? incidentRepository.findByStatus("OPEN")
                        : incidentRepository.findByStatusAndTeamIgnoreCase(
                        "OPEN", user.team());

        return incidents.stream().map(this::toResponse).toList();
    }

    @PostMapping("/{id}/claim")
    @Transactional
    public ResponseEntity<IncidentResponse> claimIncident(
            @PathVariable Long id,
            @AuthenticationPrincipal IncidentUserPrincipal user) {

        Incident incident = findVisibleIncident(id, user)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Incident not found"));

        if (!"OPEN".equalsIgnoreCase(incident.getStatus())) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Only OPEN incidents can be claimed");
        }

        if (incident.getOwnerUserId() != null) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Incident is already claimed");
        }

        int updated;

        if (user.role().equals("ADMIN")) {
            updated = incidentRepository.claimAnyIncident(
                    id, user.userId(), user.username());
        } else {
            updated = incidentRepository.claimTeamIncident(
                    id, user.userId(), user.username(), user.team());
        }

        if (updated != 1) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Incident was claimed by another user or is no longer OPEN");
        }

        Incident claimed = incidentRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Incident not found"));

        return ResponseEntity.ok(toResponse(claimed));
    }

    private java.util.Optional<Incident> findVisibleIncident(
            Long id, IncidentUserPrincipal user) {

        if (user.role().equals("ADMIN")) {
            return incidentRepository.findById(id);
        }

        return incidentRepository.findByIdAndTeamIgnoreCase(
                id, user.team());
    }

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
