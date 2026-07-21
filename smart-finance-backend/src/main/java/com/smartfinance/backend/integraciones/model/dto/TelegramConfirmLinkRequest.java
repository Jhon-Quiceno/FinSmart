package com.smartfinance.backend.integraciones.model.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Payload de {@code POST /api/integrations/telegram/confirm-link}, enviado por n8n cuando el
 * usuario le envía al bot el código generado desde la app.
 *
 * @param code   código de un solo uso generado por {@code TelegramLinkCodeStore#issue}
 * @param chatId identificador del chat de Telegram a vincular
 */
public record TelegramConfirmLinkRequest(
        @NotBlank(message = "El código es obligatorio")
        String code,
        @NotBlank(message = "El chatId es obligatorio")
        @Size(max = 50, message = "El chatId no puede superar 50 caracteres")
        String chatId
) {
}
