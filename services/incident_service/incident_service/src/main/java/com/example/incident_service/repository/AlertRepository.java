
package com.example.incident_service.repository;

import com.example.incident_service.entity.Alert;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

public interface AlertRepository extends JpaRepository<Alert, Long> {

    // Retrieve alerts assigned to a team.
    List<Alert> findByTeamIgnoreCase(String team);

    // Retrieve alerts received after a given time.
    List<Alert> findByReceivedAtGreaterThanEqual(
            Instant receivedAt
    );

    // Retrieve team alerts received after a given time.
    List<Alert> findByTeamIgnoreCaseAndReceivedAtGreaterThanEqual(
            String team,
            Instant receivedAt
    );

    // Existing method retained for compatibility.
    Optional<Alert> findFirstByFingerprintAndStatusIgnoreCaseOrderByIdAsc(
            String fingerprint,
            String status
    );

    // Retrieve all firing alerts with this fingerprint,
    // newest database record first.
    //
    // The controller will select the firing alert
    // corresponding to the incident's occurrence.
    List<Alert> findByFingerprintAndStatusIgnoreCaseOrderByIdDesc(
            String fingerprint,
            String status
    );
}
