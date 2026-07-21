package com.smartfinance.backend.integraciones.model.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Payload de {@code POST /api/integrations/telegram/expenses}, enviado por n8n con el texto
 * libre que el usuario le escribió al bot (por ejemplo, {@code "Uber 15000"}).
 *
 * @param chatId identificador del chat de Telegram que envió el mensaje
 * @param text   texto libre del mensaje, del cual se interpretan monto y descripción (ver
 *               {@code TelegramMessageParser#parse})
 */
public record TelegramExpenseRequest(
        @NotBlank(message = "El chatId es obligatorio")
        @Size(max = 50, message = "El chatId no puede superar 50 caracteres")
        String chatId,
        @NotBlank(message = "El texto es obligatorio")
        @Size(max = 500, message = "El texto no puede superar 500 caracteres")
        String text
) {
}
