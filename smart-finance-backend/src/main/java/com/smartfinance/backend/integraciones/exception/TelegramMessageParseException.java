package com.smartfinance.backend.integraciones.exception;

/**
 * Se lanza cuando el texto de un mensaje de Telegram no contiene un monto interpretable como
 * gasto (ver {@code TelegramMessageParser#parse}).
 *
 * <p>Mapeada a {@code HTTP 422} por {@code GlobalExceptionHandler}. El mensaje lo reenvía n8n
 * directamente al usuario de Telegram, por lo que debe ser un texto amigable en español que
 * explique el formato esperado.
 */
public class TelegramMessageParseException extends RuntimeException {

    public TelegramMessageParseException(String message) {
        super(message);
    }
}
