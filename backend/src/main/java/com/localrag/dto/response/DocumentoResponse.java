package com.localrag.dto.response;

import java.time.LocalDateTime;

public class DocumentoResponse {
    private Long id;
    private String nombreArchivo;
    private String tipoArchivo;
    private Long tamanoBytes;
    private String estado;
    private Integer totalChunks;
    private LocalDateTime fechaCreacion;
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

    public String getNombreArchivo() {
        return nombreArchivo;
    }

    public String getTipoArchivo() {
        return tipoArchivo;
    }

    public Long getTamanoBytes() {
        return tamanoBytes;
    }

    public String getEstado() {
        return estado;
    }

    public Integer getTotalChunks() {
        return totalChunks;
    }

    public LocalDateTime getFechaCreacion() {
        return fechaCreacion;
    }

    public LocalDateTime getFechaActualizacion() {
        return fechaActualizacion;
    }
}
