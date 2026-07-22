package com.smartfinance.backend.integraciones.exception;

/**
 * Se lanza al confirmar un vínculo de Telegram con un código inexistente, ya usado o expirado
 * (ver {@code TelegramLinkCodeStore}, TTL de 10 minutos y consumo de un solo uso).
 *
 * <p>Mapeada a {@code HTTP 400} por {@code GlobalExceptionHandler}. El mensaje lo reenvía n8n
 * directamente al usuario de Telegram, por lo que debe ser un texto amigable en español.
 */
public class InvalidLinkCodeException extends RuntimeException {

    public InvalidLinkCodeException(String message) {
        super(message);
    }
}
