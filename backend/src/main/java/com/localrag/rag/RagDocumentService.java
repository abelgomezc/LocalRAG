package com.localrag.rag;

import com.localrag.entity.Documento;
import com.localrag.exception.DocumentProcessingException;
import com.localrag.exception.DocumentNotFoundException;
import com.localrag.repository.DocumentoRepository;
import org.springframework.ai.document.Document;
import org.springframework.ai.reader.pdf.PagePdfDocumentReader;
import org.springframework.ai.transformer.splitter.TextSplitter;
import org.springframework.ai.transformer.splitter.TokenTextSplitter;
import org.springframework.ai.vectorstore.SimpleVectorStore;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.FileSystemResource;
import org.springframework.stereotype.Service;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class RagDocumentService {

    private static final Logger log = LoggerFactory.getLogger(RagDocumentService.class);

    private final DocumentoRepository documentoRepository;
    private final VectorStore vectorStore;
    private final TextSplitter textSplitter;

    @Value("${rag.chunk-size:1000}")
    private int chunkSize;

    @Value("${rag.top-k:5}")
    private int topK;

    public RagDocumentService(DocumentoRepository documentoRepository, VectorStore vectorStore, TextSplitter textSplitter) {
        this.documentoRepository = documentoRepository;
        this.vectorStore = vectorStore;
        this.textSplitter = textSplitter;
    }

    public void processAndStore(File file, String fileName, String fileType, long fileSize) {
        try {
            List<Document> documents = readDocuments(file, fileType);
            List<Document> chunks = splitDocuments(documents);
            addMetadata(chunks, fileName, fileType);
            vectorStore.add(chunks);

            Documento existing = documentoRepository.findByNombreArchivo(fileName).orElse(null);
            if (existing != null) {
                existing.setTipoArchivo(fileType);
                existing.setTamanoBytes(fileSize);
                existing.setEstado("PROCESSED");
                existing.setTotalChunks(chunks.size());
                documentoRepository.save(existing);
            } else {
                Documento documento = new Documento();
                documento.setNombreArchivo(fileName);
                documento.setTipoArchivo(fileType);
                documento.setTamanoBytes(fileSize);
                documento.setEstado("PROCESSED");
                documento.setTotalChunks(chunks.size());
                documentoRepository.save(documento);
            }
        } catch (Exception e) {
            log.error("[INGEST] Error procesando {}: {}", fileName, e.getMessage());
            throw new DocumentProcessingException("No fue posible procesar el documento: " + e.getMessage());
        }
    }

    public void deleteDocument(Long documentId) {
        try {
            Documento documento = documentoRepository.findById(documentId)
                    .orElseThrow(() -> new DocumentNotFoundException(documentId));
            String fileName = documento.getNombreArchivo();

            documentoRepository.delete(documento);

            try {
                Path uploadDir = getUploadDir();
                Files.deleteIfExists(uploadDir.resolve(fileName));
            } catch (IOException e) {
                log.warn("[DELETE] No se pudo eliminar el archivo {}: {}", fileName, e.getMessage());
            }

            log.info("[DELETE] Documento {} eliminado.", documentId);
        } catch (Exception e) {
            log.error("[DELETE] Error eliminando documento id {}: {}", documentId, e.getMessage());
            throw e;
        }
    }

    public void deleteAllDocuments() {
        try {
            List<Documento> todos = documentoRepository.findAll();
            log.info("[CLEAN] Eliminando {} documentos del sistema...", todos.size());

            Path uploadDir = getUploadDir();
            for (Documento doc : todos) {
                try {
                    Files.deleteIfExists(uploadDir.resolve(doc.getNombreArchivo()));
                } catch (IOException e) {
                    log.warn("[CLEAN] No se pudo eliminar el archivo {}: {}", doc.getNombreArchivo(), e.getMessage());
                }
            }

            documentoRepository.deleteAll();
            log.info("[CLEAN] Sistema limpiado correctamente.");
        } catch (Exception e) {
            log.error("[CLEAN] Error al limpiar el sistema: {}", e.getMessage());
            throw new DocumentProcessingException("Error al limpiar el sistema: " + e.getMessage());
        }
    }

    private Path getUploadDir() {
        String userDir = System.getProperty("user.dir");
        Path uploadDir = Paths.get(userDir, "uploads");
        if (!Files.exists(uploadDir)) {
            try {
                Files.createDirectories(uploadDir);
            } catch (IOException e) {
                throw new DocumentProcessingException("No se pudo crear el directorio de uploads: " + uploadDir);
            }
        }
        return uploadDir;
    }

    private List<Document> readDocuments(File file, String fileType) throws IOException {
        if (fileType.equalsIgnoreCase("application/pdf")) {
            PagePdfDocumentReader reader = new PagePdfDocumentReader(new FileSystemResource(file));
            return reader.read();
        } else if (isTextFile(fileType)) {
            String content = new String(Files.readAllBytes(file.toPath()));
            Document doc = Document.builder()
                    .text(content)
                    .metadata("source", file.getAbsolutePath())
                    .build();
            return List.of(doc);
        } else {
            throw new DocumentProcessingException("Tipo de archivo no soportado: " + fileType);
        }
    }

    private boolean isTextFile(String contentType) {
        return contentType.equalsIgnoreCase("text/plain")
                || contentType.equalsIgnoreCase("text/markdown")
                || contentType.equalsIgnoreCase("text/x-markdown")
                || contentType.equalsIgnoreCase("text/md");
    }

    private List<Document> splitDocuments(List<Document> documents) {
        List<Document> chunks = new ArrayList<>();
        for (Document doc : documents) {
            chunks.addAll(textSplitter.split(doc));
        }
        return chunks;
    }

    private void addMetadata(List<Document> chunks, String fileName, String fileType) {
        for (int i = 0; i < chunks.size(); i++) {
            Document chunk = chunks.get(i);
            Map<String, Object> metadata = new HashMap<>(chunk.getMetadata());
            metadata.put("fileName", fileName);
            metadata.put("fileType", fileType);
            metadata.put("chunkNumber", i + 1);
            metadata.put("documentId", fileName);
            chunk.getMetadata().clear();
            chunk.getMetadata().putAll(metadata);
        }
    }

    @Configuration
    static class TextSplitterConfig {

        @Bean
        public TextSplitter textSplitter(@Value("${rag.chunk-size:1000}") int chunkSize) {
            return TokenTextSplitter.builder()
                    .withChunkSize(chunkSize)
                    .withMinChunkSizeChars(350)
                    .withMinChunkLengthToEmbed(5)
                    .withMaxNumChunks(10000)
                    .withKeepSeparator(true)
                    .build();
        }
    }
}
