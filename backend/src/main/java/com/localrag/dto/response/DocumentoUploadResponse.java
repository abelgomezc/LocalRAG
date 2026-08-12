package com.localrag.dto.response;

public class DocumentoUploadResponse {
    private Long id;
    private String fileName;
    private String status;
    private Integer chunks;

    public DocumentoUploadResponse() {
    }

    public DocumentoUploadResponse(Long id, String fileName, String status, Integer chunks) {
        this.id = id;
        this.fileName = fileName;
        this.status = status;
        this.chunks = chunks;
    }

    public Long getId() {
        return id;
    }

    public String getFileName() {
        return fileName;
    }

    public String getStatus() {
        return status;
    }

    public Integer getChunks() {
        return chunks;
    }
}
