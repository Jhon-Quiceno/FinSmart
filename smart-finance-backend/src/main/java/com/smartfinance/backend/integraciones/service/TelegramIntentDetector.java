package com.smartfinance.backend.integraciones.service;

import com.smartfinance.backend.extractos.service.dedup.DescriptionSimilarity;

import java.util.List;

/**
 * Cheap, AI-free heuristic that decides whether a free-text Telegram message is a
 * financial-summary QUERY (e.g. {@code "¿Cuánto gasté en comida?"}, {@code "resumen"}) rather than
 * a movement REGISTRATION attempt (e.g. {@code "Uber 15000"}).
 *
 * <p>Every text message already spends one AI call ({@code AiCategorizationService#classifyMovement})
 * when registering a movement; running this keyword check first means only messages that actually
 * look like a query pay for the second AI call ({@code FinancialSummaryQueryService#parseQuery}),
 * instead of every single message paying for both.
 *
 * <p>Stateless utility class, same shape as {@link DescriptionSimilarity}: no AI call, no database
 * access, pure text heuristic.
 */
public final class TelegramIntentDetector {

    /**
     * Keywords checked against the accent/case-normalized text (see
     * {@link DescriptionSimilarity#normalize}). {@code "total"} alone already covers the
     * "gasté/gané/ingresé en total" phrasings, since they all contain it as a substring.
     */
    private static final List<String> SUMMARY_KEYWORDS = List.of("cuanto", "resumen", "balance", "total");

    private TelegramIntentDetector() {
    }

    /**
     * @param text free-text Telegram message
     * @return {@code true} if {@code text} looks like a financial-summary question rather than a
     *         movement registration attempt
     */
    public static boolean looksLikeSummaryQuery(String text) {
        if (text == null) {
            return false;
        }
        // Se chequea sobre el texto crudo, ANTES de normalizar: DescriptionSimilarity#normalize
        // reemplaza "?" por un espacio (no es alfanumérico), así que normalizar primero perdería
        // esta señal.
        if (text.contains("?")) {
            return true;
        }

        String normalized = DescriptionSimilarity.normalize(text);
        return SUMMARY_KEYWORDS.stream().anyMatch(normalized::contains);
    }
}
