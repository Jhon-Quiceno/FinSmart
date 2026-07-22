package com.smartfinance.backend.extractos.exception;

/**
 * El texto extraído del archivo de extracto quedó en blanco (por ejemplo, un PDF escaneado como
 * imagen sin capa de texto), por lo que no hay nada que enviarle al proveedor de IA.
 *
 * <p>Mapeada a {@code HTTP 422} por {@code GlobalExceptionHandler}.
 */
public class EmptyStatementTextException extends RuntimeException {

    public EmptyStatementTextException(String message) {
        super(message);
    }
}
