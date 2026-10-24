package com.incidentops.alert_service.service;

import com.incidentops.alert_service.dto.AlertEvent;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

@Service
public class KafkaProducerService {

    private static final String TOPIC = "alert-events";

    private final KafkaTemplate<String, AlertEvent> kafkaTemplate;

    public KafkaProducerService(
            KafkaTemplate<String, AlertEvent> kafkaTemplate) {

        this.kafkaTemplate = kafkaTemplate;
    }

    public void sendAlert(AlertEvent event) {

        kafkaTemplate.send(
                TOPIC,
                event.fingerprint(),
                event
        );

        System.out.println(
                "Published alert to Kafka: " +
                        event.alertName() +
                        " | fingerprint=" +
                        event.fingerprint()
        );
    }
}