package com.localrag.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public class ChatRequest {
    @NotBlank(message = "La pregunta no puede estar vacía.")
    @Size(min = 3, max = 2000, message = "La pregunta debe tener entre 3 y 2000 caracteres.")
    private String question;

    public ChatRequest() {
    }

    public ChatRequest(String question) {
        this.question = question;
    }

    public String getQuestion() {
        return question;
    }

    public void setQuestion(String question) {
        this.question = question;
    }
}
