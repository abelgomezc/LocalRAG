package com.localrag.controller;

import com.localrag.dto.request.ChatRequest;
import com.localrag.dto.response.ChatResponse;
import com.localrag.rag.RagQueryService;
import jakarta.validation.Valid;
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
            throw new IllegalArgumentException("La pregunta no puede estar vacia.");
        }

        String language = request.getLanguage();
        if (language == null || language.isBlank()) {
            language = "es";
        }

        String conversationId = request.getConversationId();
        ChatResponse response = ragQueryService.ask(question, language, conversationId);
        return ResponseEntity.ok(response);
    }
}
