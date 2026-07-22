package com.smartfinance.backend.integraciones.exception;

/**
 * Se lanza cuando un {@code chatId} de Telegram supera el límite de mensajes por minuto permitido
 * (ver {@code TelegramExpenseService#registerFromMessage}) — protege contra abuso/spam y contra
 * gastar la cuota de IA por un loop accidental del lado del usuario o de n8n.
 *
 * <p>Mapeada a {@code HTTP 429} por {@code GlobalExceptionHandler}. El mensaje lo reenvía n8n
 * directamente al usuario de Telegram, por lo que debe ser un texto amigable en español.
 */
public class TelegramRateLimitExceededException extends RuntimeException {

    public TelegramRateLimitExceededException(String message) {
        super(message);
    }
}
