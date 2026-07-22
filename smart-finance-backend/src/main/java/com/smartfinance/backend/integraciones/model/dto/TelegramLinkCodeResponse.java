package com.smartfinance.backend.integraciones.model.dto;

/**
 * Respuesta de {@code POST /api/integrations/telegram/link-code}: código de un solo uso que el
 * usuario debe enviarle al bot de Telegram para vincular su cuenta.
 *
 * @param code             código alfanumérico de 8 caracteres generado por
 *                         {@code TelegramLinkCodeStore#issue}
 * @param expiresInSeconds tiempo de vida del código en segundos antes de expirar
 */
public record TelegramLinkCodeResponse(
        String code,
        long expiresInSeconds
) {
}
