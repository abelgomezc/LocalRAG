package com.localrag.controller;

import com.localrag.dto.response.DocumentoResponse;
import com.localrag.dto.response.DocumentoUploadResponse;
import com.localrag.service.DocumentoService;
import jakarta.validation.constraints.NotNull;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.util.List;

@RestController
@RequestMapping("/api/documents")
public class DocumentosController {

    private final DocumentoService documentoService;

    public DocumentosController(DocumentoService documentoService) {
        this.documentoService = documentoService;
    }

    @PostMapping(value = "/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<DocumentoUploadResponse> uploadDocument(@RequestParam("file") @NotNull MultipartFile file) throws IOException {
        if (file.isEmpty()) {
            throw new IllegalArgumentException("El archivo está vacío.");
        }

        File tempFile = File.createTempFile("upload-", file.getOriginalFilename());
        file.transferTo(tempFile);

        DocumentoUploadResponse response = documentoService.uploadDocument(
                tempFile,
                file.getOriginalFilename(),
                file.getContentType(),
                file.getSize()
        );

        tempFile.delete();
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
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
}
