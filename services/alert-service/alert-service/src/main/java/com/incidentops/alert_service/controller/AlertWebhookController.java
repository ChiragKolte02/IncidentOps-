package com.incidentops.alert_service.controller;

import com.incidentops.alert_service.dto.AlertEvent;
import com.incidentops.alert_service.dto.AlertmanagerWebhookRequest;
import com.incidentops.alert_service.service.AlertProcessingService;
import com.incidentops.alert_service.service.KafkaProducerService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/webhook")
public class AlertWebhookController {

    private final AlertProcessingService alertProcessingService;
    private final KafkaProducerService kafkaProducerService;

    public AlertWebhookController(
            AlertProcessingService alertProcessingService,
            KafkaProducerService kafkaProducerService) {

        this.alertProcessingService = alertProcessingService;
        this.kafkaProducerService = kafkaProducerService;
    }

    @PostMapping("/alerts")
    public ResponseEntity<AlertEvent> receiveAlert(
            @RequestBody AlertmanagerWebhookRequest request) {

        System.out.println(
                "========== ALERTMANAGER REQUEST RECEIVED =========="
        );

        AlertEvent event =
                alertProcessingService.processAlert(request);

        System.out.println("Alert Name: " + event.alertName());
        System.out.println("Fingerprint: " + event.fingerprint());

        kafkaProducerService.sendAlert(event);

        System.out.println(
                "==================================================="
        );

        return ResponseEntity.ok(event);
    }
}