package com.incidentops.alert_service.service;

import com.incidentops.alert_service.dto.AlertEvent;
import com.incidentops.alert_service.dto.AlertmanagerWebhookRequest;
import org.springframework.stereotype.Service;

import java.util.Map;

@Service
public class AlertProcessingService {

    public AlertEvent processAlert(AlertmanagerWebhookRequest request) {

        if (request.alerts() == null || request.alerts().isEmpty()) {
            throw new IllegalArgumentException("No alerts found in Alertmanager payload");
        }

        AlertmanagerWebhookRequest.Alert alert =
                request.alerts().get(0);

        Map<String, String> labels = alert.labels();
        Map<String, String> annotations = alert.annotations();

        return new AlertEvent(
                labels.get("alertname"),
                alert.status(),
                labels.get("instance"),
                labels.get("severity"),
                labels.get("team"),
                alert.fingerprint(),
                annotations.get("summary"),
                annotations.get("description"),
                alert.startsAt()
        );
    }
}