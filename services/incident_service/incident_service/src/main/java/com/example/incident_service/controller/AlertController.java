
package com.example.incident_service.controller;

import com.example.incident_service.entity.Alert;
import com.example.incident_service.repository.AlertRepository;
import com.example.incident_service.security.IncidentUserPrincipal;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/alerts")
@RequiredArgsConstructor
public class AlertController {

    private final AlertRepository alertRepository;

    @GetMapping
    public List<Alert> getAlerts(
            @AuthenticationPrincipal IncidentUserPrincipal user) {

        if (user.role().equals("ADMIN")) {
            return alertRepository.findAll();
        }

        return alertRepository.findByTeamIgnoreCase(user.team());
    }
}
