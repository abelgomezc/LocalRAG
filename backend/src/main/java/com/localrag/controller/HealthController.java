package com.localrag.controller;

import com.localrag.dto.response.HealthResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/health")
public class HealthController {

    private final JdbcTemplate jdbcTemplate;

    @Value("${spring.ai.ollama.base-url}")
    private String ollamaBaseUrl;

    public HealthController(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @GetMapping
    public ResponseEntity<HealthResponse> health() {
        String databaseStatus = "DOWN";
        try {
            jdbcTemplate.queryForObject("SELECT 1", Integer.class);
            databaseStatus = "UP";
        } catch (Exception e) {
            databaseStatus = "DOWN";
        }

        String ollamaStatus = "DOWN";
        try {
            java.net.http.HttpClient client = java.net.http.HttpClient.newHttpClient();
            java.net.http.HttpRequest request = java.net.http.HttpRequest.newBuilder()
                    .uri(java.net.URI.create(ollamaBaseUrl + "/api/tags"))
                    .timeout(java.time.Duration.ofSeconds(2))
                    .build();
            java.net.http.HttpResponse<String> response = client.send(request, java.net.http.HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() == 200) {
                ollamaStatus = "UP";
            }
        } catch (Exception e) {
            ollamaStatus = "DOWN";
        }

        return ResponseEntity.ok(new HealthResponse("UP", ollamaStatus, databaseStatus));
    }
}
