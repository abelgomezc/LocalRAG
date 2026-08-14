package com.localrag.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
public class HomeController {

    @GetMapping
    public ResponseEntity<Map<String, Object>> home() {
        return ResponseEntity.ok(Map.of(
                "message", "LocalRAG API",
                "frontend", "http://localhost:5173",
                "health", "/api/health",
                "docs", "/api/documents",
                "chat", "/api/chat"
        ));
    }
}
