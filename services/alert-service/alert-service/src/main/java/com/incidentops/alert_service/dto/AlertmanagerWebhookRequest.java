package com.incidentops.alert_service.dto;

import java.util.List;
import java.util.Map;

public record AlertmanagerWebhookRequest(

        String receiver,

        String status,

        List<Alert> alerts,

        Map<String, String> commonLabels,

        Map<String, String> commonAnnotations,

        String externalURL,

        String version,

        String groupKey

) {

    public record Alert(

            String status,

            Map<String, String> labels,

            Map<String, String> annotations,

            String startsAt,

            String endsAt,

            String generatorURL,

            String fingerprint

    ) {
    }
}