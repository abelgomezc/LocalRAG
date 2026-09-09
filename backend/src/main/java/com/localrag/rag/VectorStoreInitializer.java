package com.localrag.rag;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.localrag.entity.DocumentoChunk;
import com.localrag.repository.DocumentoChunkRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Component
public class VectorStoreInitializer implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(VectorStoreInitializer.class);
    private static final int BATCH_SIZE = 50;

    private final DocumentoChunkRepository chunkRepository;
    private final VectorStore vectorStore;
    private final ObjectMapper objectMapper;

    public VectorStoreInitializer(DocumentoChunkRepository chunkRepository,
                                  VectorStore vectorStore,
                                  ObjectMapper objectMapper) {
        this.chunkRepository = chunkRepository;
        this.vectorStore = vectorStore;
        this.objectMapper = objectMapper;
    }

    @Override
    public void run(ApplicationArguments args) {
        log.info("[INIT] Initializing vector store from database...");
        try {
            List<DocumentoChunk> allChunks = chunkRepository.findAll();
            if (allChunks.isEmpty()) {
                log.info("[INIT] No chunks found in database, vector store is empty");
                return;
            }

            log.info("[INIT] Found {} chunks in database, loading into vector store in batches of {}...", allChunks.size(), BATCH_SIZE);

            int loaded = 0;
            for (int i = 0; i < allChunks.size(); i += BATCH_SIZE) {
                int end = Math.min(i + BATCH_SIZE, allChunks.size());
                List<DocumentoChunk> batch = allChunks.subList(i, end);

                List<Document> documents = new ArrayList<>();
                for (DocumentoChunk chunk : batch) {
                    try {
                        Map<String, Object> metadata = parseMetadata(chunk.getMetadatos());
                        metadata.put("fileName", chunk.getDocumentoId());
                        metadata.put("chunkNumber", chunk.getChunkNumero());

                        Document doc = Document.builder()
                                .id(chunk.getId().toString())
                                .text(chunk.getContenido())
                                .metadata(metadata)
                                .build();
                        documents.add(doc);
                    } catch (Exception e) {
                        log.warn("[INIT] Error processing chunk {}: {}", chunk.getId(), e.getMessage());
                    }
                }

                if (!documents.isEmpty()) {
                    vectorStore.add(documents);
                    loaded += documents.size();
                    log.info("[INIT] Loaded batch [{}/{}] (cumulative: {} chunks)", end, allChunks.size(), loaded);
                }
            }

            log.info("[INIT] Vector store loaded with {} documents", loaded);
        } catch (Exception e) {
            log.error("[INIT] Error loading vector store from database: {}", e.getMessage(), e);
        }
    }

    private Map<String, Object> parseMetadata(String json) {
        if (json == null || json.trim().isEmpty() || "{}".equals(json.trim())) {
            return new HashMap<>();
        }
        try {
            Map<String, Object> map = objectMapper.readValue(json, new TypeReference<Map<String, Object>>() {});
            for (Map.Entry<String, Object> entry : map.entrySet()) {
                if (entry.getValue() instanceof Long) {
                    Long val = (Long) entry.getValue();
                    if (val >= Integer.MIN_VALUE && val <= Integer.MAX_VALUE) {
                        entry.setValue(val.intValue());
                    }
                }
            }
            return map;
        } catch (Exception e) {
            log.warn("[INIT] Error parsing metadata JSON: {}", e.getMessage());
            return new HashMap<>();
        }
    }
}
