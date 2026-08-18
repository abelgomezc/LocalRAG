package com.localrag.dto.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.time.LocalDateTime;

public class DocumentoResponse {
    private Long id;

    @JsonProperty("fileName")
    private String nombreArchivo;

    @JsonProperty("fileType")
    private String tipoArchivo;

    @JsonProperty("fileSize")
    private Long tamanoBytes;

    @JsonProperty("status")
    private String estado;

    @JsonProperty("totalChunks")
    private Integer totalChunks;

    @JsonProperty("createdAt")
    private LocalDateTime fechaCreacion;

    @JsonProperty("updatedAt")
    private LocalDateTime fechaActualizacion;

    public DocumentoResponse() {
    }

    public DocumentoResponse(Long id, String nombreArchivo, String tipoArchivo, Long tamanoBytes, String estado, Integer totalChunks, LocalDateTime fechaCreacion, LocalDateTime fechaActualizacion) {
        this.id = id;
        this.nombreArchivo = nombreArchivo;
        this.tipoArchivo = tipoArchivo;
        this.tamanoBytes = tamanoBytes;
        this.estado = estado;
        this.totalChunks = totalChunks;
        this.fechaCreacion = fechaCreacion;
        this.fechaActualizacion = fechaActualizacion;
    }

    public Long getId() {
        return id;
    }

    @JsonProperty("fileName")
    public String getNombreArchivo() {
        return nombreArchivo;
    }

    @JsonProperty("fileType")
    public String getTipoArchivo() {
        return tipoArchivo;
    }

    @JsonProperty("fileSize")
    public Long getTamanoBytes() {
        return tamanoBytes;
    }

    @JsonProperty("status")
    public String getEstado() {
        return estado;
    }

    @JsonProperty("totalChunks")
    public Integer getTotalChunks() {
        return totalChunks;
    }

    @JsonProperty("createdAt")
    public LocalDateTime getFechaCreacion() {
        return fechaCreacion;
    }

    @JsonProperty("updatedAt")
    public LocalDateTime getFechaActualizacion() {
        return fechaActualizacion;
    }
}
