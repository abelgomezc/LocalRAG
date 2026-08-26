package com.localrag.controller;

import com.localrag.dto.request.DocumentRelationRequest;
import com.localrag.dto.response.DocumentRelationResponse;
import com.localrag.dto.response.DocumentoResponse;
import com.localrag.dto.response.DocumentoUploadResponse;
import com.localrag.exception.DocumentProcessingException;
import com.localrag.service.DocumentoService;
import com.localrag.service.DocumentRelationService;
import jakarta.validation.constraints.NotNull;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

@RestController
@RequestMapping("/api/documents")
public class DocumentosController {

    private static final Logger log = LoggerFactory.getLogger(DocumentosController.class);

    private final DocumentoService documentoService;
    private final DocumentRelationService relationService;
    private static final int MAX_DOCUMENTS = 5;

    public DocumentosController(DocumentoService documentoService, DocumentRelationService relationService) {
        this.documentoService = documentoService;
        this.relationService = relationService;
    }

    @PostMapping(value = "/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<List<DocumentoUploadResponse>> uploadDocuments(@RequestParam("files") @NotNull List<MultipartFile> files) throws IOException {
        if (files == null || files.isEmpty()) {
            throw new IllegalArgumentException("No se enviaron archivos.");
        }

        long currentCount = documentoService.countDocuments();
        if (currentCount + files.size() > MAX_DOCUMENTS) {
            throw new IllegalArgumentException(String.format(
                "Limite de documentos alcanzado. Actualmente tienes %d/%d documentos. Elimina alguno antes de subir nuevos.",
                currentCount, MAX_DOCUMENTS
            ));
        }

        List<DocumentoUploadResponse> responses = new ArrayList<>();
        for (MultipartFile file : files) {
            if (file.isEmpty()) {
                continue;
            }

            String contentType = file.getContentType();
            String originalName = file.getOriginalFilename();
            if (contentType == null || !isSupportedContentType(contentType)) {
                String resolvedType = resolveFileType(contentType, originalName);
                if (resolvedType == null || resolvedType.isBlank()) {
                    throw new IllegalArgumentException("Tipo de archivo no soportado: " + contentType + ". Solo se permiten PDF, TXT, Markdown, Word, Excel y CSV.");
                }
                contentType = resolvedType;
            }

            Path uploadDir = getUploadDir();
            Path target = uploadDir.resolve(originalName);
            file.transferTo(target.toFile());

            try {
                DocumentoUploadResponse response = documentoService.uploadDocument(
                        target.toFile(),
                        originalName,
                        contentType,
                        file.getSize()
                );
                responses.add(response);
            } catch (Exception e) {
                Files.deleteIfExists(target);
                log.error("[UPLOAD] Error procesando {}: {}", originalName, e.getMessage(), e);
                throw new DocumentProcessingException("No fue posible procesar el documento: " + e.getMessage());
            }
        }

        return ResponseEntity.status(HttpStatus.CREATED).body(responses);
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

    private boolean isSupportedContentType(String contentType) {
        if (contentType == null) return false;
        return contentType.equalsIgnoreCase("application/pdf")
                || contentType.equalsIgnoreCase("text/plain")
                || contentType.equalsIgnoreCase("text/markdown")
                || contentType.equalsIgnoreCase("text/x-markdown")
                || contentType.equalsIgnoreCase("text/md")
                || contentType.equalsIgnoreCase("application/vnd.openxmlformats-officedocument.wordprocessingml.document")
                || contentType.equalsIgnoreCase("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet")
                || contentType.equalsIgnoreCase("text/csv")
                || contentType.equalsIgnoreCase("application/octet-stream");
    }

    private String resolveFileType(String contentType, String fileName) {
        if (contentType != null && !contentType.equalsIgnoreCase("application/octet-stream") && !contentType.isBlank()) {
            return contentType;
        }
        String name = fileName.toLowerCase();
        if (name.endsWith(".pdf")) return "application/pdf";
        if (name.endsWith(".txt")) return "text/plain";
        if (name.endsWith(".md") || name.endsWith(".markdown")) return "text/markdown";
        if (name.endsWith(".docx")) return "application/vnd.openxmlformats-officedocument.wordprocessingml.document";
        if (name.endsWith(".xlsx")) return "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet";
        if (name.endsWith(".csv")) return "text/csv";
        return contentType;
    }

    @GetMapping
    public ResponseEntity<List<DocumentoResponse>> listDocuments() {
        return ResponseEntity.ok(documentoService.listDocuments());
    }

    @GetMapping("/{id}/content")
    public ResponseEntity<String> getDocumentContent(@PathVariable Long id) {
        return ResponseEntity.ok(documentoService.getDocumentContent(id));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteDocument(@PathVariable Long id) {
        documentoService.deleteDocument(id);
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping
    public ResponseEntity<Void> deleteAllDocuments() {
        documentoService.deleteAllDocuments();
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/relations")
    public ResponseEntity<DocumentRelationResponse> createRelation(@RequestBody DocumentRelationRequest request) {
        DocumentRelationResponse response = relationService.createRelation(
                request.getSourceDocumentId(),
                request.getTargetDocumentId(),
                request.getDescription()
        );
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/relations")
    public ResponseEntity<List<DocumentRelationResponse>> listRelations() {
        return ResponseEntity.ok(relationService.listRelations());
    }

    @DeleteMapping("/relations/{relationId}")
    public ResponseEntity<Void> deleteRelation(@PathVariable Long relationId) {
        relationService.deleteRelation(relationId);
        return ResponseEntity.noContent().build();
    }
}

