package com.smartfinance.backend.integraciones.model.dto;

/**
 * @param linked si el usuario autenticado ya tiene un chat de Telegram vinculado
 */
public record TelegramLinkStatusResponse(boolean linked) {
}
