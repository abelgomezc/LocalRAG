package com.localrag.service;

import com.localrag.dto.response.DocumentoResponse;
import com.localrag.dto.response.DocumentoUploadResponse;
import com.localrag.entity.Documento;
import com.localrag.entity.DocumentoChunk;
import com.localrag.rag.RagDocumentService;
import com.localrag.repository.DocumentoRepository;
import com.localrag.repository.DocumentoChunkRepository;
import com.localrag.repository.ConversationRepository;
import com.localrag.repository.MessageRepository;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.SimpleVectorStore;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class DocumentoService {

    private static final int MAX_DOCUMENTS = 5;

    private final DocumentoRepository documentoRepository;
    private final RagDocumentService ragDocumentService;
    private final DocumentoChunkRepository chunkRepository;
    private final ConversationRepository conversationRepository;
    private final MessageRepository messageRepository;

    public DocumentoService(DocumentoRepository documentoRepository, RagDocumentService ragDocumentService, DocumentoChunkRepository chunkRepository, ConversationRepository conversationRepository, MessageRepository messageRepository) {
        this.documentoRepository = documentoRepository;
        this.ragDocumentService = ragDocumentService;
        this.chunkRepository = chunkRepository;
        this.conversationRepository = conversationRepository;
        this.messageRepository = messageRepository;
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

    public void deleteAllDocuments() {
        ragDocumentService.deleteAllDocuments();
        messageRepository.deleteAll();
        conversationRepository.deleteAll();
    }

    public boolean hasProcessedDocuments() {
        return documentoRepository.findAll().stream()
                .anyMatch(d -> "PROCESSED".equals(d.getEstado()));
    }

    public String getDocumentContent(Long id) {
        Documento documento = documentoRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Documento no encontrado"));
        List<DocumentoChunk> chunks = chunkRepository.findByDocumentoIdOrderByChunkNumeroAsc(documento.getNombreArchivo());
        return chunks.stream()
                .map(DocumentoChunk::getContenido)
                .collect(Collectors.joining("\n\n"));
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
