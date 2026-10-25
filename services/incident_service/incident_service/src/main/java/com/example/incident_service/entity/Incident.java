
package com.example.incident_service.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;

@Entity
@Table(name = "incidents")
@Getter
@Setter
@NoArgsConstructor
public class Incident {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String incidentNumber;

    @Column(nullable = false)
    private String title;

    @Column(nullable = false)
    private String status;

    private String severity;

    private String team;

    private String instance;

    @Column(nullable = false)
    private String fingerprint;

    private String createdAt;

    // Ownership information
    private Long ownerUserId;

    private String ownerUsername;

    @Column(name = "opened_at")
    private Instant openedAt;

    @Column(name = "resolved_at")
    private Instant resolvedAt;

    // Exact firing alert that created this incident.
    // Nullable for incidents created before this feature.
    @Column(name = "source_alert_id")
    private Long sourceAlertId;
}
