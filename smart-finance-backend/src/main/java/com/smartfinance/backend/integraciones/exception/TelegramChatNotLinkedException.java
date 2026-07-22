package com.smartfinance.backend.integraciones.exception;

/**
 * Se lanza cuando llega un mensaje de Telegram desde un {@code chatId} que todavía no fue
 * vinculado a ningún usuario (ver {@code TelegramExpenseService#registerFromMessage}).
 *
 * <p>Mapeada a {@code HTTP 404} por {@code GlobalExceptionHandler}. El mensaje lo reenvía n8n
 * directamente al usuario de Telegram, por lo que debe ser un texto amigable en español que
 * explique cómo vincular la cuenta.
 */
public class TelegramChatNotLinkedException extends RuntimeException {

    public TelegramChatNotLinkedException(String message) {
        super(message);
    }
}
