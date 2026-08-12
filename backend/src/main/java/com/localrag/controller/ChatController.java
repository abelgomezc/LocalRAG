package com.localrag.controller;

import com.localrag.dto.request.ChatRequest;
import com.localrag.dto.response.ChatResponse;
import com.localrag.rag.RagQueryService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/chat")
public class ChatController {

    private final RagQueryService ragQueryService;

    public ChatController(RagQueryService ragQueryService) {
        this.ragQueryService = ragQueryService;
    }

    @PostMapping
    public ResponseEntity<ChatResponse> chat(@Valid @RequestBody ChatRequest request) {
        String question = request.getQuestion();
        if (question == null || question.isBlank()) {
            throw new IllegalArgumentException("La pregunta no puede estar vacía.");
        }

        ChatResponse response = ragQueryService.ask(question);
        return ResponseEntity.ok(response);
    }
}
