package com.example.incident_service.repository;

import com.example.incident_service.entity.Alert;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.Instant;
import java.util.List;

public interface AlertRepository extends JpaRepository<Alert, Long> {

    List<Alert> findByTeamIgnoreCase(String team);

    List<Alert> findByReceivedAtGreaterThanEqual(Instant receivedAt);

    List<Alert> findByTeamIgnoreCaseAndReceivedAtGreaterThanEqual(
            String team,
            Instant receivedAt
    );
}