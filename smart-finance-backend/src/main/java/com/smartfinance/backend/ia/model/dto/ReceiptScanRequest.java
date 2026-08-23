package com.smartfinance.backend.ia.model.dto;

import jakarta.validation.constraints.NotBlank;

/**
 * Payload de {@code POST /api/receipts/scan}: la foto de un recibo tomada desde la cámara de la
 * app móvil, autenticado por JWT estándar — sin ninguna dependencia del bot de Telegram.
 *
 * @param imageDataUri la imagen del recibo como data URI {@code data:image/jpeg;base64,...} —
 *                      mismo formato que ya usa {@code TelegramReceiptRequest#imageUrl} para la
 *                      captura de recibos por Telegram (ver {@code ReceiptExtractionService}). Sin
 *                      {@code @Size}: una imagen en base64 puede superar ampliamente cualquier
 *                      límite pensado para texto libre.
 */
public record ReceiptScanRequest(
        @NotBlank(message = "La imagen es obligatoria")
        String imageDataUri
) {
}
