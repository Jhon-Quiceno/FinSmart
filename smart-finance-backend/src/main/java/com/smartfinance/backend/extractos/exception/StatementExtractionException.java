package com.smartfinance.backend.extractos.exception;

/**
 * Falla genérica al extraer los movimientos de un extracto bancario: el proveedor de IA
 * respondió, pero {@code StatementAiExtractionService} no pudo interpretar la respuesta como una
 * lista de movimientos válida.
 *
 * <p>Mapeada a {@code HTTP 422} por {@code GlobalExceptionHandler}.
 */
public class StatementExtractionException extends RuntimeException {

    public StatementExtractionException(String message) {
        super(message);
    }

    public StatementExtractionException(String message, Throwable cause) {
        super(message, cause);
    }
}
