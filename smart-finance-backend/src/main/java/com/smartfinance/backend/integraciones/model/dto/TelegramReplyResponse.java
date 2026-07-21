package com.smartfinance.backend.integraciones.model.dto;

/**
 * Respuesta genérica de los endpoints de Telegram: el texto que n8n debe reenviar tal cual al
 * usuario en el chat.
 *
 * @param reply mensaje en español listo para mostrarse al usuario final
 */
public record TelegramReplyResponse(
        String reply
) {
}
