package com.localrag.repository;

import com.localrag.entity.DocumentoChunk;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface DocumentoChunkRepository extends JpaRepository<DocumentoChunk, Long> {
    List<DocumentoChunk> findByDocumentoId(String documentoId);

    void deleteByDocumentoId(String documentoId);

    @Query(value = """
            SELECT dc.*,
                   ts_rank(dc.content_tsv, query) AS rank
            FROM documento_chunks dc,
                 plainto_tsquery('spanish', :query) query
            WHERE dc.content_tsv @@ query
            ORDER BY rank DESC
            LIMIT :limit
            """, nativeQuery = true)
    List<DocumentoChunk> searchFullText(@Param("query") String query, @Param("limit") int limit);
}
