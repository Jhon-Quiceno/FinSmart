package com.smartfinance.backend.ia.model.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Payload for {@code POST /api/ai/chat}: a single free-form message from the user to the
 * assistant.
 *
 * @param message the user's message, in any language (the assistant is instructed to reply in
 *                Spanish regardless — see {@code FinancialContextBuilder})
 */
public record ChatRequest(
        @NotBlank(message = "El mensaje no puede estar vacío")
        @Size(max = 4000, message = "El mensaje no puede superar 4000 caracteres")
        String message
) {
}
