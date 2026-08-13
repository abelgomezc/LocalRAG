package com.localrag.entity;

import jakarta.persistence.*;

@Entity
@Table(name = "document_relations")
public class DocumentRelation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "source_document_id", nullable = false)
    private Long sourceDocumentId;

    @Column(name = "target_document_id", nullable = false)
    private Long targetDocumentId;

    @Column(name = "description", length = 255)
    private String description;

    public DocumentRelation() {
    }

    public DocumentRelation(Long sourceDocumentId, Long targetDocumentId, String description) {
        this.sourceDocumentId = sourceDocumentId;
        this.targetDocumentId = targetDocumentId;
        this.description = description;
    }

    public Long getId() {
        return id;
    }

    public Long getSourceDocumentId() {
        return sourceDocumentId;
    }

    public void setSourceDocumentId(Long sourceDocumentId) {
        this.sourceDocumentId = sourceDocumentId;
    }

    public Long getTargetDocumentId() {
        return targetDocumentId;
    }

    public void setTargetDocumentId(Long targetDocumentId) {
        this.targetDocumentId = targetDocumentId;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }
}
