package com.localrag.exception;

public class DocumentNotFoundException extends RagException {
    public DocumentNotFoundException(Long id) {
        super("Documento no encontrado con id: " + id);
    }
}
