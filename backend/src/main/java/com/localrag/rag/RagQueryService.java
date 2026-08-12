package com.localrag.rag;

import com.localrag.dto.response.ChatResponse;
import com.localrag.dto.response.ChatResponse.Source;
import com.localrag.exception.OllamaConnectionException;
import com.localrag.exception.RagException;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class RagQueryService {

    private final ChatClient chatClient;
    private final VectorStore vectorStore;

    @Value("${rag.top-k:5}")
    private int topK;

    public RagQueryService(ChatClient.Builder chatClientBuilder, VectorStore vectorStore) {
        this.chatClient = chatClientBuilder.build();
        this.vectorStore = vectorStore;
    }

    public ChatResponse ask(String question) {
        try {
            List<Document> relevantDocs = vectorStore.similaritySearch(question);
            if (relevantDocs.size() > topK) {
                relevantDocs = relevantDocs.stream().limit(topK).collect(Collectors.toList());
            }

            String context = buildContext(relevantDocs);
            String answer = generateAnswer(question, context);
            List<Source> sources = buildSources(relevantDocs);

            return new ChatResponse(answer, sources);
        } catch (Exception e) {
            throw new OllamaConnectionException("Error al consultar Ollama: " + e.getMessage());
        }
    }

    private String buildContext(List<Document> documents) {
        StringBuilder sb = new StringBuilder();
        for (Document doc : documents) {
            sb.append("Chunk: ").append(doc.getText()).append("\n\n");
        }
        return sb.toString();
    }

    private String generateAnswer(String question, String context) {
        String prompt = String.format("""
                Eres un asistente especializado en responder preguntas utilizando exclusivamente el contexto recuperado de los documentos.

                CONTEXTO:
                %s

                PREGUNTA:
                %s

                INSTRUCCIONES:
                - Responde utilizando principalmente el contexto proporcionado.
                - No inventes información.
                - Si el contexto no contiene suficiente información, dilo claramente.
                - Responde de forma clara y concisa.
                """, context, question);

        return chatClient.prompt()
                .user(prompt)
                .call()
                .content();
    }

    private List<Source> buildSources(List<Document> documents) {
        return documents.stream()
                .map(doc -> {
                    Map<String, Object> metadata = doc.getMetadata();
                    String fileName = metadata != null ? (String) metadata.get("fileName") : "unknown";
                    Object pageNumber = metadata != null ? metadata.get("pageNumber") : null;
                    Object chunkNumber = metadata != null ? metadata.get("chunkNumber") : null;

                    Integer page = pageNumber instanceof Integer ? (Integer) pageNumber : null;
                    Integer chunk = chunkNumber instanceof Integer ? (Integer) chunkNumber : null;

                    return new Source(fileName, page, chunk);
                })
                .collect(Collectors.toList());
    }
}
