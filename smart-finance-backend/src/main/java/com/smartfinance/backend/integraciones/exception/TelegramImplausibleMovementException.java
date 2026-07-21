package com.smartfinance.backend.integraciones.exception;

/**
 * Se lanza cuando el texto de un mensaje de Telegram, aunque contiene un monto interpretable,
 * resulta implausible como movimiento financiero real (ver {@code TelegramMessageParser#parse}):
 * una descripción sin ninguna letra, o un monto fuera de un rango razonable para una transacción
 * personal.
 *
 * <p>Es un filtro heurístico barato, sin llamada a IA, pensado para descartar ruido evidente
 * (dos números sueltos, montos con un dígito de más o de menos) antes de gastar una llamada al
 * proveedor de IA en clasificar el movimiento.
 *
 * <p>Mapeada a {@code HTTP 422} por {@code GlobalExceptionHandler}. El mensaje lo reenvía n8n
 * directamente al usuario de Telegram, por lo que debe ser un texto amigable en español.
 */
public class TelegramImplausibleMovementException extends RuntimeException {

    public TelegramImplausibleMovementException(String message) {
        super(message);
    }
}
