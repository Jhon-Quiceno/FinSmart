package com.smartfinance.backend.integraciones.service;

import com.smartfinance.backend.integraciones.exception.TelegramImplausibleMovementException;
import com.smartfinance.backend.integraciones.exception.TelegramMessageParseException;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Interpreta el texto libre de un mensaje de Telegram como monto + descripción de un movimiento.
 *
 * <p>Es un port exacto a Java de {@code parseQuickAddInput} (frontend,
 * {@code components/quick-add/quick-add-command.tsx}): toma el primer token numérico del texto
 * (patrón {@value #AMOUNT_REGEX}), le quita separadores de miles ({@code .} y {@code ,}), y usa
 * el resto del texto (con espacios colapsados) como descripción. A diferencia de la versión del
 * frontend —que tolera la ausencia de monto porque el usuario sigue completando el formulario a
 * mano—, este parser exige un monto válido y mayor a cero, porque un mensaje de Telegram se
 * procesa de una sola vez sin posibilidad de completar campos faltantes.
 *
 * <p>Además aplica un filtro barato de plausibilidad (sin llamada a IA) contra mensajes sin
 * sentido — ver {@link #validatePlausibility}. Este filtro corre antes de gastar cualquier llamada
 * al proveedor de IA en clasificar el movimiento.
 */
@Component
public class TelegramMessageParser {

    private static final String AMOUNT_REGEX = "\\d[\\d.,]*";
    private static final Pattern AMOUNT_PATTERN = Pattern.compile(AMOUNT_REGEX);
    private static final String INVALID_AMOUNT_MESSAGE =
            "No pude identificar el monto del gasto. Enviá el monto junto con una breve descripción, "
                    + "por ejemplo: \"Uber 15000\".";
    private static final String NO_LETTERS_MESSAGE =
            "No pude entender ese mensaje. Escribí una descripción junto con el monto, por ejemplo: "
                    + "\"Uber 15000\".";
    private static final String AMOUNT_TOO_LOW_MESSAGE =
            "Ese monto parece muy bajo. Verificá el mensaje e intentá de nuevo.";
    private static final String AMOUNT_TOO_HIGH_MESSAGE =
            "Ese monto parece demasiado alto. Si es correcto, registralo desde la app.";

    // Límites deliberadamente generosos, no una regla de negocio estricta: solo buscan filtrar
    // errores de tipeo o ruido evidente (un monto menor a cualquier moneda real, o mayor a lo
    // plausible para una transacción personal), sin rechazar movimientos legítimos.
    private static final BigDecimal MIN_PLAUSIBLE_AMOUNT = BigDecimal.valueOf(100);
    private static final BigDecimal MAX_PLAUSIBLE_AMOUNT = BigDecimal.valueOf(500_000_000);

    /**
     * @param text texto libre del mensaje de Telegram
     * @return el monto y la descripción interpretados
     * @throws TelegramMessageParseException si el texto no contiene un monto numérico mayor a cero
     * @throws TelegramImplausibleMovementException si el mensaje, aunque tiene un monto válido,
     *         resulta implausible como movimiento real (ver {@link #validatePlausibility})
     */
    public ParsedMessage parse(String text) {
        String trimmed = text.trim();
        Matcher matcher = AMOUNT_PATTERN.matcher(trimmed);

        if (!matcher.find()) {
            throw new TelegramMessageParseException(INVALID_AMOUNT_MESSAGE);
        }

        String numericToken = matcher.group();
        // El patrón exige que el primer carácter sea un dígito, así que siempre queda al menos
        // ese dígito tras quitar separadores de miles: el parseo nunca puede lanzar
        // NumberFormatException por una cadena vacía.
        BigDecimal amount = new BigDecimal(numericToken.replace(".", "").replace(",", ""));
        if (amount.signum() <= 0) {
            throw new TelegramMessageParseException(INVALID_AMOUNT_MESSAGE);
        }

        String description = (trimmed.substring(0, matcher.start()) + trimmed.substring(matcher.end()))
                .replaceAll("\\s+", " ")
                .trim();

        ParsedMessage parsed = new ParsedMessage(amount, description.isEmpty() ? trimmed : description);
        validatePlausibility(parsed);
        return parsed;
    }

    /**
     * Filtro heurístico barato, sin llamada a IA, contra mensajes sin sentido: una descripción sin
     * ninguna letra suele ser ruido (por ejemplo, dos números sueltos: {@code "15000 12345"}), y un
     * monto fuera de {@value #MIN_PLAUSIBLE_AMOUNT}–{@value #MAX_PLAUSIBLE_AMOUNT} casi siempre es
     * un error de tipeo antes que un movimiento real.
     */
    private static void validatePlausibility(ParsedMessage parsed) {
        if (!containsLetter(parsed.description())) {
            throw new TelegramImplausibleMovementException(NO_LETTERS_MESSAGE);
        }
        if (parsed.amount().compareTo(MIN_PLAUSIBLE_AMOUNT) < 0) {
            throw new TelegramImplausibleMovementException(AMOUNT_TOO_LOW_MESSAGE);
        }
        if (parsed.amount().compareTo(MAX_PLAUSIBLE_AMOUNT) > 0) {
            throw new TelegramImplausibleMovementException(AMOUNT_TOO_HIGH_MESSAGE);
        }
    }

    /** Unicode-aware: cuenta letras acentuadas del español ({@code á}, {@code ñ}, etc.) como letras. */
    private static boolean containsLetter(String text) {
        return text.chars().anyMatch(Character::isLetter);
    }

    /**
     * @param amount      monto interpretado, siempre mayor a cero
     * @param description descripción interpretada, nunca vacía (cae al texto completo si no
     *                    queda nada tras quitar el token numérico)
     */
    public record ParsedMessage(BigDecimal amount, String description) {
    }
}
