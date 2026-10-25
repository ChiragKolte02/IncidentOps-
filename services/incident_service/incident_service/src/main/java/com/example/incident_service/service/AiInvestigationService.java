
package com.example.incident_service.service;

import tools.jackson.databind.JsonNode;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;
import org.springframework.web.server.ResponseStatusException;

import java.time.Duration;
import java.time.Instant;
import java.util.Map;

@Service
public class AiInvestigationService {

    private final RestClient restClient;
    private final String internalApiKey;

    public AiInvestigationService(
            @Value("${ai.agent.url:http://localhost:8000}")
            String aiAgentUrl,

            @Value("${ai.agent.internal-api-key:}")
            String internalApiKey
    ) {
        this.internalApiKey = internalApiKey;

        this.restClient = RestClient.builder()
                .baseUrl(aiAgentUrl)
                .requestFactory(createRequestFactory())
                .build();
    }

    private org.springframework.http.client.ClientHttpRequestFactory
    createRequestFactory() {

        var factory =
                new org.springframework.http.client.SimpleClientHttpRequestFactory();

        factory.setConnectTimeout(Duration.ofSeconds(5));
        factory.setReadTimeout(Duration.ofSeconds(90));

        return factory;
    }

    public JsonNode investigate(
            String incidentNumber,
            String alertName,
            String team,
            String instance,
            Instant startsAt
    ) {

        // Ensure the internal API key is configured.
        if (internalApiKey == null || internalApiKey.isBlank()) {
            throw new ResponseStatusException(
                    HttpStatus.SERVICE_UNAVAILABLE,
                    "AI Agent integration is not configured"
            );
        }

        // Construct the request expected by FastAPI.
        Map<String, Object> requestBody = Map.of(
                "incident_number", incidentNumber,
                "alert_name", alertName,
                "team", team,
                "instance", instance,
                "starts_at", startsAt.toString()
        );

        try {

            JsonNode response = restClient.post()
                    .uri("/ai/investigate")
                    .header(
                            "X-Internal-API-Key",
                            internalApiKey
                    )
                    .header(
                            HttpHeaders.ACCEPT,
                            MediaType.APPLICATION_JSON_VALUE
                    )
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(requestBody)
                    .retrieve()
                    .body(JsonNode.class);

            // Handle an empty AI Agent response.
            if (response == null || response.isNull()) {
                throw new ResponseStatusException(
                        HttpStatus.BAD_GATEWAY,
                        "AI Agent returned an empty response"
                );
            }

            return response;

        } catch (ResponseStatusException exception) {

            throw exception;

        } catch (RestClientResponseException exception) {

            // Log the upstream HTTP status and validation error.
            // Do not log the internal API key.
            System.err.println(
                    "[AI INVESTIGATION ERROR] Incident: "
                            + incidentNumber
            );

            System.err.println(
                    "[AI INVESTIGATION ERROR] HTTP status: "
                            + exception.getStatusCode()
            );

            System.err.println(
                    "[AI INVESTIGATION ERROR] FastAPI response: "
                            + exception.getResponseBodyAsString()
            );

            throw new ResponseStatusException(
                    HttpStatus.BAD_GATEWAY,
                    "AI Agent investigation request failed",
                    exception
            );

        } catch (RestClientException exception) {

            // Connection failures, timeouts, and other client errors.
            System.err.println(
                    "[AI INVESTIGATION ERROR] Incident: "
                            + incidentNumber
            );

            System.err.println(
                    "[AI INVESTIGATION ERROR] Request failure: "
                            + exception.getClass().getSimpleName()
            );

            throw new ResponseStatusException(
                    HttpStatus.BAD_GATEWAY,
                    "AI Agent investigation request failed",
                    exception
            );
        }
    }
}
