package com.localrag.rag;

import com.localrag.dto.response.ChatResponse;
import com.localrag.dto.response.ChatResponse.Source;
import com.localrag.entity.DocumentRelation;
import com.localrag.exception.OllamaConnectionException;
import com.localrag.exception.RagException;
import com.localrag.service.DocumentRelationService;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class RagQueryService {

    private static final Logger log = LoggerFactory.getLogger(RagQueryService.class);

    private final ChatClient chatClient;
    private final VectorStore vectorStore;
    private final DocumentRelationService relationService;

    @Value("${rag.top-k:5}")
    private int topK;

    public RagQueryService(ChatClient.Builder chatClientBuilder, VectorStore vectorStore, DocumentRelationService relationService) {
        this.chatClient = chatClientBuilder.build();
        this.vectorStore = vectorStore;
        this.relationService = relationService;
    }

    public ChatResponse ask(String question, String language) {
        try {
            List<Document> relevantDocs = vectorStore.similaritySearch(question);
            if (relevantDocs.size() > topK) {
                relevantDocs = relevantDocs.stream().limit(topK).collect(Collectors.toList());
            }

            String context = buildContext(relevantDocs);
            String relationsContext = buildRelationsContext();
            String answer = generateAnswer(question, context, relationsContext, language);
            List<Source> sources = buildSources(relevantDocs);

            return new ChatResponse(answer, sources);
        } catch (Exception e) {
            log.error("[CHAT] Error en consulta: {}", e.getMessage());
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

    private String buildRelationsContext() {
        List<DocumentRelation> relations = relationService.listAllRelations();
        if (relations.isEmpty()) {
            return "";
        }
        StringBuilder sb = new StringBuilder();
        sb.append("RELACIONES ENTRE DOCUMENTOS:\n");
        for (DocumentRelation relation : relations) {
            sb.append(String.format("- Documento %d se relaciona con Documento %d: %s\n",
                    relation.getSourceDocumentId(),
                    relation.getTargetDocumentId(),
                    relation.getDescription() != null ? relation.getDescription() : "Sin descripcion"));
        }
        return sb.toString();
    }

    private String generateAnswer(String question, String context, String relationsContext, String language) {
        String lang = language != null && !language.isBlank() ? language : "es";
        String prompt = String.format("""
                Eres un asistente especializado en responder preguntas utilizando exclusivamente el contexto recuperado de los documentos.

                %s

                CONTEXTO:
                %s

                PREGUNTA:
                %s

                INSTRUCCIONES:
                - Responde en idioma %s.
                - Responde utilizando principalmente el contexto proporcionado.
                - No inventes informacion.
                - Si el contexto no contiene suficiente informacion, dilo claramente.
                - Responde de forma clara y concisa.
                """, relationsContext.isEmpty() ? "" : relationsContext + "\n", context, question, lang);

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
