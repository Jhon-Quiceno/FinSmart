package com.smartfinance.backend.integraciones.model.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Payload de {@code POST /api/integrations/telegram/receipts}, enviado por n8n con la foto de un
 * recibo que el usuario le envió al bot.
 *
 * @param chatId   identificador del chat de Telegram que envió la foto
 * @param imageUrl la imagen del recibo, como URL {@code https://} o data URI
 *                 {@code data:image/jpeg;base64,...} — n8n envía la foto como data URI, ya que el
 *                 proveedor de IA acepta ambas formas indistintamente en su campo
 *                 {@code image_url.url} (ver {@code ReceiptExtractionService}). Sin
 *                 {@code @Size}: una imagen en base64 puede superar ampliamente cualquier límite
 *                 pensado para texto libre (ver {@code TelegramExpenseRequest#text}).
 */
public record TelegramReceiptRequest(
        @NotBlank(message = "El chatId es obligatorio")
        @Size(max = 50, message = "El chatId no puede superar 50 caracteres")
        String chatId,
        @NotBlank(message = "La imagen es obligatoria")
        String imageUrl
) {
}
