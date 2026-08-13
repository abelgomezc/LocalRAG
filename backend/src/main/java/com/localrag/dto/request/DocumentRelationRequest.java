package com.localrag.dto.request;

import jakarta.validation.constraints.NotNull;

public class DocumentRelationRequest {
    @NotNull
    private Long sourceDocumentId;

    @NotNull
    private Long targetDocumentId;

    private String description;

    public DocumentRelationRequest() {
    }

    public DocumentRelationRequest(Long sourceDocumentId, Long targetDocumentId, String description) {
        this.sourceDocumentId = sourceDocumentId;
        this.targetDocumentId = targetDocumentId;
        this.description = description;
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
