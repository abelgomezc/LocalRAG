package com.localrag.controller;

import com.localrag.dto.request.ChatRequest;
import com.localrag.dto.response.ChatResponse;
import com.localrag.rag.RagQueryService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ChatControllerTest {

    @Mock
    private RagQueryService ragQueryService;

    @InjectMocks
    private ChatController chatController;

    @Test
    void chat_shouldReturnResponse() {
        ChatRequest request = new ChatRequest("What is Java?", "es");
        ChatResponse mockResponse = new ChatResponse("Java is a language.", List.of());
        when(ragQueryService.ask("What is Java?", "es", null)).thenReturn(mockResponse);

        var response = chatController.chat(request);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals("Java is a language.", response.getBody().getAnswer());
    }

    @Test
    void chat_shouldThrowOnBlankQuestion() {
        ChatRequest request = new ChatRequest("   ", "es");
        assertThrows(IllegalArgumentException.class, () -> chatController.chat(request));
    }
}
