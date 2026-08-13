package com.localrag.service;

import com.localrag.entity.Documento;
import com.localrag.rag.RagDocumentService;
import com.localrag.repository.DocumentoRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DocumentoServiceTest {

    @Mock
    private DocumentoRepository documentoRepository;

    @Mock
    private RagDocumentService ragDocumentService;

    @InjectMocks
    private DocumentoService documentoService;

    @Test
    void listDocuments_shouldReturnAll() {
        Documento doc = new Documento("test.pdf", "application/pdf", 1024L, "PROCESSED", 10);
        when(documentoRepository.findAll()).thenReturn(List.of(doc));

        var result = documentoService.listDocuments();

        assertEquals(1, result.size());
        assertEquals("test.pdf", result.get(0).getNombreArchivo());
    }

    @Test
    void hasProcessedDocuments_shouldReturnTrueWhenProcessedExists() {
        Documento doc = new Documento("test.pdf", "application/pdf", 1024L, "PROCESSED", 10);
        when(documentoRepository.findAll()).thenReturn(List.of(doc));

        assertTrue(documentoService.hasProcessedDocuments());
    }

    @Test
    void deleteDocument_shouldCallRagService() {
        documentoService.deleteDocument(1L);

        verify(ragDocumentService).deleteDocument(1L);
    }
}
