package com.example.incident_service.controller;

import com.example.incident_service.dto.AnalyticsOverviewResponse;
import com.example.incident_service.security.IncidentUserPrincipal;
import com.example.incident_service.service.AnalyticsService;

import lombok.RequiredArgsConstructor;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/analytics")
@RequiredArgsConstructor
public class AnalyticsController {

    private final AnalyticsService analyticsService;

    @GetMapping("/overview")
    public AnalyticsOverviewResponse getOverview(
            @RequestParam(defaultValue = "24h") String range,
            @AuthenticationPrincipal IncidentUserPrincipal user
    ) {
        String team = "ADMIN".equalsIgnoreCase(user.role())
                ? null
                : user.team();

        return analyticsService.getOverview(range, team);
    }
}