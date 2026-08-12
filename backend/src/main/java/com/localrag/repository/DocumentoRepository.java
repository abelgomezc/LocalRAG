package com.localrag.repository;

import com.localrag.entity.Documento;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface DocumentoRepository extends JpaRepository<Documento, Long> {
    Optional<Documento> findByNombreArchivo(String nombreArchivo);
}
