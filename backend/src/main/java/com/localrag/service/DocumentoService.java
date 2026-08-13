package com.localrag.service;

import com.localrag.dto.response.DocumentoResponse;
import com.localrag.dto.response.DocumentoUploadResponse;
import com.localrag.entity.Documento;
import com.localrag.rag.RagDocumentService;
import com.localrag.repository.DocumentoRepository;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class DocumentoService {

    private static final int MAX_DOCUMENTS = 5;

    private final DocumentoRepository documentoRepository;
    private final RagDocumentService ragDocumentService;

    public DocumentoService(DocumentoRepository documentoRepository, RagDocumentService ragDocumentService) {
        this.documentoRepository = documentoRepository;
        this.ragDocumentService = ragDocumentService;
    }

    public DocumentoUploadResponse uploadDocument(File file, String fileName, String contentType, long size) {
        ragDocumentService.processAndStore(file, fileName, contentType, size);
        Documento documento = documentoRepository.findByNombreArchivo(fileName)
                .orElseThrow(() -> new RuntimeException("Error al recuperar documento procesado"));
        return new DocumentoUploadResponse(
                documento.getId(),
                documento.getNombreArchivo(),
                documento.getEstado(),
                documento.getTotalChunks()
        );
    }

    public long countDocuments() {
        return documentoRepository.count();
    }

    public List<DocumentoResponse> listDocuments() {
        return documentoRepository.findAll().stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    public void deleteDocument(Long id) {
        ragDocumentService.deleteDocument(id);
    }

    public boolean hasProcessedDocuments() {
        return documentoRepository.findAll().stream()
                .anyMatch(d -> "PROCESSED".equals(d.getEstado()));
    }

    private DocumentoResponse toResponse(Documento documento) {
        return new DocumentoResponse(
                documento.getId(),
                documento.getNombreArchivo(),
                documento.getTipoArchivo(),
                documento.getTamanoBytes(),
                documento.getEstado(),
                documento.getTotalChunks(),
                documento.getFechaCreacion(),
                documento.getFechaActualizacion()
        );
    }
}
