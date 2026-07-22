package com.smartfinance.backend.integraciones.exception;

/**
 * Se lanza al pedir un código de vínculo nuevo cuando el usuario autenticado ya tiene un chat de
 * Telegram vinculado — no tiene sentido generar otro código mientras el vínculo actual siga
 * activo (ver {@code TelegramLinkService#generateLinkCode}).
 *
 * <p>Mapeada a {@code HTTP 400} por {@code GlobalExceptionHandler}. Este endpoint lo llama la app
 * directamente (con JWT), no n8n, así que el mensaje lo lee el usuario en la propia interfaz.
 */
public class TelegramAlreadyLinkedException extends RuntimeException {

    public TelegramAlreadyLinkedException(String message) {
        super(message);
    }
}
