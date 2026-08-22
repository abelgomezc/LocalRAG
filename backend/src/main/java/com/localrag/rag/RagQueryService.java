package com.localrag.rag;

import com.localrag.dto.response.ChatResponse;
import com.localrag.dto.response.ChatResponse.Source;
import com.localrag.entity.DocumentoChunk;
import com.localrag.entity.DocumentRelation;
import com.localrag.exception.OllamaConnectionException;
import com.localrag.exception.RagException;
import com.localrag.repository.DocumentoChunkRepository;
import com.localrag.service.DocumentRelationService;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Service
public class RagQueryService {

    private static final Logger log = LoggerFactory.getLogger(RagQueryService.class);

    private final ChatClient chatClient;
    private final VectorStore vectorStore;
    private final DocumentRelationService relationService;
    private final DocumentoChunkRepository chunkRepository;

    @Value("${rag.top-k:5}")
    private int topK;

    public RagQueryService(ChatClient.Builder chatClientBuilder, VectorStore vectorStore, DocumentRelationService relationService, DocumentoChunkRepository chunkRepository) {
        this.chatClient = chatClientBuilder.build();
        this.vectorStore = vectorStore;
        this.relationService = relationService;
        this.chunkRepository = chunkRepository;
    }

    public ChatResponse ask(String question, String language) {
        try {
            String normalizedQuery = rewriteQuery(question);
            log.debug("[QUERY] Original: '{}' -> Normalizada: '{}'", question, normalizedQuery);

            List<Document> relevantDocs = hybridSearch(normalizedQuery, topK);
            if (relevantDocs.size() > topK) {
                relevantDocs = relevantDocs.stream().limit(topK).collect(Collectors.toList());
            }

            String context = buildContext(relevantDocs);
            String relationsContext = buildRelationsContext();

            String answer;
            try {
                answer = CompletableFuture.supplyAsync(() ->
                        generateAnswer(question, context, relationsContext, language)
                ).get(120, TimeUnit.SECONDS);
            } catch (java.util.concurrent.TimeoutException e) {
                log.error("[CHAT] Timeout al generar respuesta para: {}", question);
                throw new OllamaConnectionException("El modelo tardo demasiado en responder. Intenta nuevamente.");
            }

            List<Source> sources = buildSources(relevantDocs);
            return new ChatResponse(answer, sources);
        } catch (Exception e) {
            log.error("[CHAT] Error en consulta: {}", e.getMessage());
            throw new OllamaConnectionException("Error al consultar Ollama: " + e.getMessage());
        }
    }

    private String rewriteQuery(String question) {
        String prompt = String.format("""
                Reformula la siguiente consulta para optimizar la busqueda en documentos.
                Reglas:
                1. Corrige errores de ortografia y gramatica.
                2. Expande con sinonimos relevantes del dominio.
                3. Normaliza terminos tecnicos.
                4. Manten el idioma original.
                5. Devuelve SOLO la consulta reformulada, sin comillas, sin prefijos, sin explicaciones.
                
                Consulta original: %s
                """, question);

        try {
            String rewritten = chatClient.prompt()
                    .user(prompt)
                    .call()
                    .content()
                    .trim();
            if (rewritten.isBlank() || rewritten.equalsIgnoreCase(question)) {
                return question;
            }
            return rewritten;
        } catch (Exception e) {
            log.warn("[QUERY REWRITE] Error al reescribir consulta, usando original: {}", e.getMessage());
            return question;
        }
    }

    private List<Document> hybridSearch(String normalizedQuery, int limit) {
        List<Document> vectorResults = new ArrayList<>(vectorStore.similaritySearch(normalizedQuery));
        List<DocumentoChunk> fullTextResults = chunkRepository.searchFullText(normalizedQuery, limit * 2);

        Map<String, ScoredDocument> combined = new LinkedHashMap<>();

        for (int i = 0; i < vectorResults.size(); i++) {
            Document doc = vectorResults.get(i);
            String key = getDocumentKey(doc);
            combined.put(key, new ScoredDocument(doc, 1.0 - (i / (double) Math.max(vectorResults.size(), 1)), "vector"));
        }

        for (int i = 0; i < fullTextResults.size(); i++) {
            DocumentoChunk chunk = fullTextResults.get(i);
            String key = chunk.getDocumentoId() + "_" + chunk.getChunkNumero();
            Document doc = toDocument(chunk);
            double score = 1.0 - (i / (double) Math.max(fullTextResults.size(), 1));
            combined.merge(key, new ScoredDocument(doc, score, "fulltext"), (existing, incoming) -> {
                existing.score = Math.max(existing.score, incoming.score);
                existing.source = "hybrid";
                return existing;
            });
        }

        return combined.values().stream()
                .sorted(Comparator.comparingDouble((ScoredDocument sd) -> sd.score).reversed())
                .limit(limit)
                .map(sd -> sd.document)
                .collect(Collectors.toList());
    }

    private String getDocumentKey(Document doc) {
        Map<String, Object> metadata = doc.getMetadata();
        String fileName = metadata != null ? (String) metadata.get("fileName") : "unknown";
        Object chunkNumber = metadata != null ? metadata.get("chunkNumber") : null;
        return fileName + "_" + (chunkNumber != null ? chunkNumber : "0");
    }

    private Document toDocument(DocumentoChunk chunk) {
        Map<String, Object> metadata = new HashMap<>();
        metadata.put("fileName", chunk.getDocumentoId());
        metadata.put("chunkNumber", chunk.getChunkNumero());
        metadata.put("fileType", "unknown");
        return Document.builder()
                .text(chunk.getContenido())
                .metadata(metadata)
                .build();
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

    private static class ScoredDocument {
        Document document;
        double score;
        String source;

        ScoredDocument(Document document, double score, String source) {
            this.document = document;
            this.score = score;
            this.source = source;
        }
    }
}
