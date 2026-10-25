
package com.example.incident_service.service;

import com.example.incident_service.dto.AlertEvent;
import com.example.incident_service.entity.Alert;
import com.example.incident_service.entity.Incident;
import com.example.incident_service.repository.AlertRepository;
import com.example.incident_service.repository.IncidentRepository;

import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;

@Service
public class AlertEventConsumer {

    private final AlertRepository alertRepository;
    private final IncidentRepository incidentRepository;

    public AlertEventConsumer(
            AlertRepository alertRepository,
            IncidentRepository incidentRepository
    ) {
        this.alertRepository = alertRepository;
        this.incidentRepository = incidentRepository;
    }

    @KafkaListener(
            topics = "alert-events",
            groupId = "incident-service-group"
    )
    public void consume(AlertEvent event) {

        System.out.println(
                "========== KAFKA ALERT RECEIVED =========="
        );

        System.out.println("Alert Name: " + event.alertName());
        System.out.println("Status: " + event.status());
        System.out.println("Instance: " + event.instance());
        System.out.println("Severity: " + event.severity());
        System.out.println("Team: " + event.team());
        System.out.println("Fingerprint: " + event.fingerprint());

        // --------------------------------------------------
        // STEP 1: Always save the alert
        // --------------------------------------------------

        Alert alert = new Alert();

        alert.setAlertName(event.alertName());
        alert.setStatus(event.status());
        alert.setInstance(event.instance());
        alert.setReceivedAt(Instant.now());
        alert.setSeverity(event.severity());
        alert.setTeam(event.team());
        alert.setFingerprint(event.fingerprint());
        alert.setSummary(event.summary());
        alert.setDescription(event.description());
        alert.setStartsAt(event.startsAt());

        Alert savedAlert = alertRepository.save(alert);

        System.out.println(
                "Alert saved to PostgreSQL with ID: "
                        + savedAlert.getId()
        );

        // --------------------------------------------------
        // STEP 2: Handle FIRING alert
        // --------------------------------------------------

        if ("firing".equalsIgnoreCase(event.status())) {

            incidentRepository
                    .findByFingerprintAndStatusIn(
                            event.fingerprint(),
                            List.of("OPEN", "IN_PROGRESS")
                    )
                    .ifPresentOrElse(

                            // Existing OPEN or IN_PROGRESS incident
                            existingIncident -> {

                                System.out.println(
                                        "Duplicate alert detected."
                                );

                                System.out.println(
                                        "Existing incident: "
                                                + existingIncident.getIncidentNumber()
                                );

                                System.out.println(
                                        "No new incident created."
                                );
                            },

                            // No active incident: create a new one.
                            () -> {

                                Incident incident = new Incident();

                                incident.setIncidentNumber(
                                        generateIncidentNumber()
                                );

                                incident.setTitle(
                                        event.summary()
                                );

                                incident.setStatus("OPEN");

                                incident.setSeverity(
                                        event.severity()
                                );

                                incident.setTeam(
                                        event.team()
                                );

                                incident.setInstance(
                                        event.instance()
                                );

                                incident.setFingerprint(
                                        event.fingerprint()
                                );

                                incident.setCreatedAt(
                                        event.startsAt()
                                );

                                incident.setOpenedAt(Instant.now());

                                // IMPORTANT:
                                // Link this incident to the exact
                                // firing alert saved above.
                                incident.setSourceAlertId(
                                        savedAlert.getId()
                                );

                                Incident savedIncident =
                                        incidentRepository.save(incident);

                                System.out.println(
                                        "NEW INCIDENT CREATED: "
                                                + savedIncident.getIncidentNumber()
                                );

                                System.out.println(
                                        "Source firing alert ID: "
                                                + savedIncident.getSourceAlertId()
                                );
                            }
                    );
        }

        // --------------------------------------------------
        // STEP 3: Handle RESOLVED alert
        // --------------------------------------------------

        else if ("resolved".equalsIgnoreCase(event.status())) {

            incidentRepository
                    .findByFingerprintAndStatusIn(
                            event.fingerprint(),
                            List.of("OPEN", "IN_PROGRESS")
                    )
                    .ifPresentOrElse(

                            existingIncident -> {

                                existingIncident.setStatus("RESOLVED");
                                existingIncident.setResolvedAt(
                                        Instant.now()
                                );

                                incidentRepository.save(
                                        existingIncident
                                );

                                System.out.println(
                                        "Incident RESOLVED: "
                                                + existingIncident.getIncidentNumber()
                                );
                            },

                            () -> {

                                System.out.println(
                                        "Resolved alert received, "
                                                + "but no active incident was found."
                                );
                            }
                    );
        }

        System.out.println(
                "=========================================="
        );
    }

    private String generateIncidentNumber() {

        long nextId = incidentRepository.count() + 1;

        return String.format(
                "INC-%06d",
                nextId
        );
    }
}
