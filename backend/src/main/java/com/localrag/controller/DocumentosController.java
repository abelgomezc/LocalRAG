package com.localrag.controller;

import com.localrag.dto.request.DocumentRelationRequest;
import com.localrag.dto.response.DocumentRelationResponse;
import com.localrag.dto.response.DocumentoResponse;
import com.localrag.dto.response.DocumentoUploadResponse;
import com.localrag.service.DocumentoService;
import com.localrag.service.DocumentRelationService;
import jakarta.validation.constraints.NotNull;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

@RestController
@RequestMapping("/api/documents")
public class DocumentosController {

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
            if (contentType == null || !isSupportedContentType(contentType)) {
                throw new IllegalArgumentException("Tipo de archivo no soportado: " + contentType + ". Solo se permiten PDF, TXT y Markdown.");
            }

            File tempFile = File.createTempFile("upload-", file.getOriginalFilename());
            file.transferTo(tempFile);

            try {
                DocumentoUploadResponse response = documentoService.uploadDocument(
                        tempFile,
                        file.getOriginalFilename(),
                        contentType,
                        file.getSize()
                );
                responses.add(response);
            } finally {
                tempFile.delete();
            }
        }

        return ResponseEntity.status(HttpStatus.CREATED).body(responses);
    }

    private boolean isSupportedContentType(String contentType) {
        return contentType.equalsIgnoreCase("application/pdf")
                || contentType.equalsIgnoreCase("text/plain")
                || contentType.equalsIgnoreCase("text/markdown")
                || contentType.equalsIgnoreCase("text/x-markdown")
                || contentType.equalsIgnoreCase("text/md");
    }

    @GetMapping
    public ResponseEntity<List<DocumentoResponse>> listDocuments() {
        return ResponseEntity.ok(documentoService.listDocuments());
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteDocument(@PathVariable Long id) {
        documentoService.deleteDocument(id);
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
