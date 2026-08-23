package com.smartfinance.backend.ia.model.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Payload de {@code POST /api/receipts/scan}: la foto de un recibo tomada desde la cámara de la
 * app móvil, autenticado por JWT estándar — sin ninguna dependencia del bot de Telegram.
 *
 * @param imageDataUri la imagen del recibo como data URI {@code data:image/jpeg;base64,...} —
 *                      mismo formato que ya usa {@code TelegramReceiptRequest#imageUrl} para la
 *                      captura de recibos por Telegram (ver {@code ReceiptExtractionService}). A
 *                      diferencia de {@code TelegramReceiptRequest} (sin tope, porque n8n ya
 *                      controla el tamaño aguas arriba), acá sí hay un {@code @Size}: el cliente es
 *                      la app móvil tomando una foto con la cámara, un tamaño acotado y predecible
 *                      (~11MB crudos en base64), y sin tope un usuario autenticado podría bufferizar
 *                      payloads arbitrariamente grandes en memoria antes de que se valide nada.
 */
public record ReceiptScanRequest(
        @NotBlank(message = "La imagen es obligatoria")
        @Size(max = 15_000_000, message = "La imagen es demasiado grande")
        String imageDataUri
) {
}
