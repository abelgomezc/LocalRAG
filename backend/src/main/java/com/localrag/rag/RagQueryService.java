package com.localrag.rag;

import com.localrag.dto.response.ChatResponse;
import com.localrag.dto.response.ChatResponse.Source;
import com.localrag.entity.DocumentoChunk;
import com.localrag.entity.DocumentRelation;
import com.localrag.entity.Conversation;
import com.localrag.entity.Message;
import com.localrag.exception.OllamaConnectionException;
import com.localrag.exception.RagException;
import com.localrag.repository.ConversationRepository;
import com.localrag.repository.DocumentoChunkRepository;
import com.localrag.repository.MessageRepository;
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
    private static final int MAX_HISTORY_TURNS = 6;

    private final ChatClient chatClient;
    private final VectorStore vectorStore;
    private final DocumentRelationService relationService;
    private final DocumentoChunkRepository chunkRepository;
    private final ConversationRepository conversationRepository;
    private final MessageRepository messageRepository;

    @Value("${rag.top-k:5}")
    private int topK;

    @Value("${rag.crag-min-score:0.3}")
    private double cragMinScore;

    @Value("${rag.max-iterations:3}")
    private int maxIterations;

    public RagQueryService(ChatClient.Builder chatClientBuilder, VectorStore vectorStore, DocumentRelationService relationService, DocumentoChunkRepository chunkRepository, ConversationRepository conversationRepository, MessageRepository messageRepository) {
        this.chatClient = chatClientBuilder.build();
        this.vectorStore = vectorStore;
        this.relationService = relationService;
        this.chunkRepository = chunkRepository;
        this.conversationRepository = conversationRepository;
        this.messageRepository = messageRepository;
    }

    public ChatResponse ask(String question, String language, String conversationId) {
        String convId = conversationId != null && !conversationId.isBlank() ? conversationId : UUID.randomUUID().toString();
        try {
            Conversation conversation = conversationRepository.findByConversationId(convId)
                    .orElseGet(() -> {
                        Conversation c = new Conversation();
                        c.setConversationId(convId);
                        c.setCreatedAt(java.time.LocalDateTime.now());
                        c.setUpdatedAt(java.time.LocalDateTime.now());
                        return conversationRepository.save(c);
                    });
            conversation.setUpdatedAt(java.time.LocalDateTime.now());
            conversationRepository.save(conversation);

            List<Message> history = messageRepository.findByConversationIdOrderByCreatedAtAsc(convId);
            String historyContext = buildHistoryContext(history);

            // === AGENTIC RAG: loop con estrategias ===
            String normalizedQuery = rewriteQuery(question);
            List<Document> relevantDocs = new ArrayList<>();
            String context = "";
            String relationsContext = "";
            String answer = "";
            List<Source> sources = new ArrayList<>();
            String strategy = "initial";
            int iterations = 0;

            while (iterations < maxIterations) {
                iterations++;
                log.info("[AGENT] Iteración {} con estrategia '{}'", iterations, strategy);

                // === CRAG: buscar y evaluar calidad ===
                relevantDocs = hybridSearch(normalizedQuery, topK);
                if (relevantDocs.size() > topK) {
                    relevantDocs = relevantDocs.stream().limit(topK).collect(Collectors.toList());
                }

                double qualityScore = evaluateRetrievalQuality(relevantDocs);
                log.info("[CRAG] Calidad de retrieval: {} (minimo {})", qualityScore, cragMinScore);

                if (qualityScore < cragMinScore && iterations < maxIterations) {
                    log.warn("[CRAG] Calidad baja, corrigiendo query...");
                    normalizedQuery = correctQuery(question, relevantDocs, strategy);
                    strategy = "corrected";
                    continue;
                }

                context = buildContext(relevantDocs);
                relationsContext = buildRelationsContext(relevantDocs);

                // === SELF-RAG: LLM genera con reflexion ===
                log.info("[SELF-RAG] Generando respuesta con auto-reflexion...");
                SelfRagResult result = generateWithSelfRag(question, context, relationsContext, historyContext, language, iterations);

                if (result.needsMoreContext && iterations < maxIterations) {
                    log.info("[SELF-RAG] LLM solicita mas contexto, iterando...");
                    normalizedQuery = result.refinedQuery;
                    strategy = "selfrag-more-context";
                    continue;
                }

                answer = result.answer;
                sources = buildSources(relevantDocs);

                // === AGENTIC RAG: evaluar si la respuesta es buena ===
                if (iterations < maxIterations) {
                    boolean isGood = evaluateAnswerQuality(answer, question, context);
                    if (!isGood) {
                        log.warn("[AGENT] Respuesta de baja calidad, intentando otra estrategia...");
                        normalizedQuery = rewriteQuery(question + " (reformulada para mayor precision)");
                        strategy = "reformulated";
                        continue;
                    }
                }

                break;
            }

            // Guardar historial
            Message userMessage = new Message();
            userMessage.setConversationId(convId);
            userMessage.setRole("user");
            userMessage.setContent("Usuario: " + question);
            userMessage.setCreatedAt(java.time.LocalDateTime.now());
            messageRepository.save(userMessage);

            Message assistantMessage = new Message();
            assistantMessage.setConversationId(convId);
            assistantMessage.setRole("assistant");
            assistantMessage.setContent("Asistente: " + answer);
            assistantMessage.setCreatedAt(java.time.LocalDateTime.now());
            messageRepository.save(assistantMessage);

            pruneOldMessages(convId);

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
        final int MAX_CHUNK_CHARS = 3000;
        for (Document doc : documents) {
            String text = doc.getText();
            if (text != null && text.length() > MAX_CHUNK_CHARS) {
                text = text.substring(0, MAX_CHUNK_CHARS) + "...[truncado]";
            }
            sb.append("Chunk: ").append(text).append("\n\n");
        }
        return sb.toString();
    }

    private String buildRelationsContext(List<Document> relevantDocs) {
        List<DocumentRelation> relations = relationService.listAllRelations();
        if (relations.isEmpty()) {
            return "";
        }

        Set<Long> relevantDocIds = relevantDocs.stream()
                .map(doc -> doc.getMetadata() != null ? doc.getMetadata().get("fileName") : null)
                .filter(Objects::nonNull)
                .map(Object::toString)
                .filter(name -> !name.equals("unknown"))
                .map(name -> {
                    try {
                        return Long.parseLong(name);
                    } catch (NumberFormatException e) {
                        return null;
                    }
                })
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());

        List<DocumentRelation> filtered = relations.stream()
                .filter(r -> relevantDocIds.contains(r.getSourceDocumentId()) || relevantDocIds.contains(r.getTargetDocumentId()))
                .collect(Collectors.toList());

        if (filtered.isEmpty()) {
            return "";
        }

        StringBuilder sb = new StringBuilder();
        sb.append("RELACIONES ENTRE DOCUMENTOS:\n");
        for (DocumentRelation relation : filtered) {
            sb.append(String.format("- Documento %d se relaciona con Documento %d: %s\n",
                    relation.getSourceDocumentId(),
                    relation.getTargetDocumentId(),
                    relation.getDescription() != null ? relation.getDescription() : "Sin descripcion"));
        }
        return sb.toString();
    }

    private String buildHistoryContext(List<Message> history) {
        if (history == null || history.isEmpty()) {
            return "";
        }
        StringBuilder sb = new StringBuilder();
        sb.append("HISTORIAL DE CONVERSACION:\n");
        int count = 0;
        int start = Math.max(0, history.size() - MAX_HISTORY_TURNS);
        for (int i = start; i < history.size(); i++) {
            sb.append(history.get(i).getContent()).append("\n");
            count++;
            if (count >= MAX_HISTORY_TURNS) break;
        }
        return sb.toString();
    }

    private void pruneOldMessages(String conversationId) {
        List<Message> all = messageRepository.findByConversationIdOrderByCreatedAtAsc(conversationId);
        if (all.size() > MAX_HISTORY_TURNS * 2) {
            List<Message> toDelete = all.subList(0, all.size() - MAX_HISTORY_TURNS * 2);
            messageRepository.deleteAll(toDelete);
            messageRepository.flush();
        }
    }

    private String generateAnswer(String question, String context, String relationsContext, String historyContext, String language) {
        String lang = language != null && !language.isBlank() ? language : "es";
        String prompt = String.format("""
                Eres un asistente especializado en responder preguntas utilizando exclusivamente el contexto recuperado de los documentos.

                %s

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
                - Cuando la respuesta involucre varios documentos, indica explicitamente cuales documentos se estan cruzando.
                - Si hay RELACIONES ENTRE DOCUMENTOS, usalas para enriquecer la respuesta cuando la pregunta cruce temas de multiples documentos.
                """, historyContext.isEmpty() ? "" : historyContext + "\n", relationsContext.isEmpty() ? "" : relationsContext + "\n", context, question, lang);

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

    // === CRAG: Evaluar calidad del retrieval ===
    private double evaluateRetrievalQuality(List<Document> docs) {
        if (docs == null || docs.isEmpty()) return 0.0;

        double totalScore = 0.0;
        int count = 0;
        for (Document doc : docs) {
            String text = doc.getText();
            if (text != null && !text.isBlank()) {
                totalScore += 1.0;
                count++;
            }
        }
        return count == 0 ? 0.0 : (double) count / docs.size();
    }

    private String correctQuery(String originalQuery, List<Document> poorResults, String strategy) {
        String prompt = String.format("""
                La siguiente consulta devolvio resultados de baja calidad.
                Consulta original: %s
                Estrategia: %s
                
                Reformula la consulta para mejorar la busqueda.
                Devuelve SOLO la nueva consulta, sin explicaciones.
                """, originalQuery, strategy);

        try {
            return chatClient.prompt()
                    .user(prompt)
                    .call()
                    .content()
                    .trim();
        } catch (Exception e) {
            log.warn("[CRAG] Error corrigiendo query: {}", e.getMessage());
            return originalQuery;
        }
    }

    // === SELF-RAG: LLM genera con auto-reflexion ===
    private static class SelfRagResult {
        String answer;
        boolean needsMoreContext;
        String refinedQuery;
    }

    private SelfRagResult generateWithSelfRag(String question, String context, String relationsContext,
                                              String historyContext, String language, int iteration) {
        String lang = language != null && !language.isBlank() ? language : "es";
        String prompt = String.format("""
                Eres un asistente que responde preguntas usando contexto de documentos.
                INSTRUCCIONES DE AUTO-REFLEXION:
                - Antes de responder, evalua si el contexto es suficiente.
                - Si el contexto es insuficiente, escribe [NEEDS_MORE] seguido de la consulta reformulada.
                - Si el contexto es suficiente, responde directamente.
                - Si usas multiples documentos, mencionalos explicitamente.
                
                %s
                
                %s
                
                CONTEXTO:
                %s
                
                PREGUNTA:
                %s
                
                INSTRUCCIONES:
                - Responde en idioma %s.
                - Usa [NEEDS_MORE] <query> si necesitas mas contexto.
                - No inventes informacion.
                - Si el contexto no contiene suficiente informacion, dilo claramente.
                """, historyContext.isEmpty() ? "" : historyContext + "\n",
                relationsContext.isEmpty() ? "" : relationsContext + "\n",
                context, question, lang);

        try {
            String raw = chatClient.prompt()
                    .user(prompt)
                    .call()
                    .content()
                    .trim();

            SelfRagResult result = new SelfRagResult();
            if (raw.startsWith("[NEEDS_MORE]")) {
                result.needsMoreContext = true;
                result.refinedQuery = raw.substring("[NEEDS_MORE]".length()).trim();
                if (result.refinedQuery.isBlank()) {
                    result.refinedQuery = question;
                }
                return result;
            }

            result.answer = raw;
            result.needsMoreContext = false;
            return result;
        } catch (Exception e) {
            log.error("[SELF-RAG] Error generando respuesta: {}", e.getMessage());
            SelfRagResult result = new SelfRagResult();
            result.answer = "Error al generar respuesta.";
            result.needsMoreContext = false;
            return result;
        }
    }

    // === AGENTIC RAG: Evaluar calidad de la respuesta ===
    private boolean evaluateAnswerQuality(String answer, String question, String context) {
        if (answer == null || answer.length() < 10) return false;
        if (answer.toLowerCase().contains("no lo se") ||
            answer.toLowerCase().contains("no sé") ||
            answer.toLowerCase().contains("no suficiente") ||
            answer.toLowerCase().contains("error")) {
            return false;
        }
        return true;
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
