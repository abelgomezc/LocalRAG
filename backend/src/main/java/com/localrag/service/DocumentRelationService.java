package com.localrag.service;

import com.localrag.dto.response.DocumentRelationResponse;
import com.localrag.entity.DocumentRelation;
import com.localrag.entity.Documento;
import com.localrag.repository.DocumentRelationRepository;
import com.localrag.repository.DocumentoRepository;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
public class DocumentRelationService {

    private final DocumentRelationRepository relationRepository;
    private final DocumentoRepository documentoRepository;

    public DocumentRelationService(DocumentRelationRepository relationRepository, DocumentoRepository documentoRepository) {
        this.relationRepository = relationRepository;
        this.documentoRepository = documentoRepository;
    }

    public DocumentRelationResponse createRelation(Long sourceDocumentId, Long targetDocumentId, String description) {
        DocumentRelation relation = new DocumentRelation(sourceDocumentId, targetDocumentId, description);
        relation = relationRepository.save(relation);
        return toResponse(relation);
    }

    public List<DocumentRelationResponse> listRelations() {
        return relationRepository.findAll().stream()
                .map(this::toResponse)
                .toList();
    }

    public List<DocumentRelation> listAllRelations() {
        return relationRepository.findAll();
    }

    public List<DocumentRelationResponse> listRelationsByDocument(Long documentId) {
        return relationRepository.findBySourceDocumentId(documentId).stream()
                .map(this::toResponse)
                .toList();
    }

    public void deleteRelation(Long relationId) {
        relationRepository.deleteById(relationId);
    }

    private DocumentRelationResponse toResponse(DocumentRelation relation) {
        Documento source = documentoRepository.findById(relation.getSourceDocumentId()).orElse(null);
        Documento target = documentoRepository.findById(relation.getTargetDocumentId()).orElse(null);
        String sourceName = source != null ? source.getNombreArchivo() : "Desconocido";
        String targetName = target != null ? target.getNombreArchivo() : "Desconocido";
        return new DocumentRelationResponse(
                relation.getId(),
                relation.getSourceDocumentId(),
                sourceName,
                relation.getTargetDocumentId(),
                targetName,
                relation.getDescription()
        );
    }
}
