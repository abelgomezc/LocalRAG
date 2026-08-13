package com.localrag.dto.response;

public class DocumentRelationResponse {
    private Long id;
    private Long sourceDocumentId;
    private String sourceDocumentName;
    private Long targetDocumentId;
    private String targetDocumentName;
    private String description;

    public DocumentRelationResponse() {
    }

    public DocumentRelationResponse(Long id, Long sourceDocumentId, String sourceDocumentName, Long targetDocumentId, String targetDocumentName, String description) {
        this.id = id;
        this.sourceDocumentId = sourceDocumentId;
        this.sourceDocumentName = sourceDocumentName;
        this.targetDocumentId = targetDocumentId;
        this.targetDocumentName = targetDocumentName;
        this.description = description;
    }

    public Long getId() {
        return id;
    }

    public Long getSourceDocumentId() {
        return sourceDocumentId;
    }

    public String getSourceDocumentName() {
        return sourceDocumentName;
    }

    public Long getTargetDocumentId() {
        return targetDocumentId;
    }

    public String getTargetDocumentName() {
        return targetDocumentName;
    }

    public String getDescription() {
        return description;
    }
}
