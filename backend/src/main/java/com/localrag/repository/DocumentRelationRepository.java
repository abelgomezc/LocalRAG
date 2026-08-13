package com.localrag.repository;

import com.localrag.entity.DocumentRelation;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface DocumentRelationRepository extends JpaRepository<DocumentRelation, Long> {
    List<DocumentRelation> findBySourceDocumentId(Long sourceDocumentId);
    List<DocumentRelation> findByTargetDocumentId(Long targetDocumentId);
    boolean existsBySourceDocumentIdAndTargetDocumentId(Long sourceDocumentId, Long targetDocumentId);
}
