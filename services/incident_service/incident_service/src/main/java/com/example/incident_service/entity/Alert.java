package com.example.incident_service.entity;
import java.time.Instant;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "alerts")
@Getter
@Setter
@NoArgsConstructor
public class Alert {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String alertName;

    @Column(nullable = false)
    private String status;

    private String instance;

    private String severity;

    private String team;

    @Column(nullable = false)
    private String fingerprint;

    private String summary;

    @Column(columnDefinition = "TEXT")
    private String description;

    private String startsAt;
    
    @Column(name = "received_at")
    private Instant receivedAt;
}