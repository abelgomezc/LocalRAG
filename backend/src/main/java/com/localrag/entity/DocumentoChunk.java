package com.localrag.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "documento_chunks")
public class DocumentoChunk {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "documento_id", nullable = false, length = 255)
    private String documentoId;

    @Column(name = "chunk_numero", nullable = false)
    private Integer chunkNumero;

    @Column(columnDefinition = "TEXT")
    private String contenido;

    @Column(name = "metadatos", columnDefinition = "JSONB")
    private String metadatos;

    @Column(name = "fecha_creacion", nullable = false)
    private LocalDateTime fechaCreacion;

    @PrePersist
    protected void onCreate() {
        this.fechaCreacion = LocalDateTime.now();
    }

    public DocumentoChunk() {
    }

    public Long getId() {
        return id;
    }

    public String getDocumentoId() {
        return documentoId;
    }

    public void setDocumentoId(String documentoId) {
        this.documentoId = documentoId;
    }

    public Integer getChunkNumero() {
        return chunkNumero;
    }

    public void setChunkNumero(Integer chunkNumero) {
        this.chunkNumero = chunkNumero;
    }

    public String getContenido() {
        return contenido;
    }

    public void setContenido(String contenido) {
        this.contenido = contenido;
    }

    public String getMetadatos() {
        return metadatos;
    }

    public void setMetadatos(String metadatos) {
        this.metadatos = metadatos;
    }

    public LocalDateTime getFechaCreacion() {
        return fechaCreacion;
    }
}
