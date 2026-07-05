package com.smartfinance.backend.service.jobs;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

/**
 * Shared Spanish-language formatting for the notification titles/messages built by the Sprint 5
 * (Batch 3) scheduled jobs and event listeners.
 *
 * <p>Amounts are formatted with an explicit {@link DecimalFormatSymbols} instead of an
 * {@code es-*} {@link Locale}, mirroring
 * {@code com.smartfinance.backend.service.ai.FinancialContextBuilder#formatCop} — this avoids
 * depending on the JVM having Spanish locale data bundled. Dates use {@code dd/MM/yyyy} for the
 * same reason: it reads unambiguously in Spanish without needing localized month names.
 */
final class NotificationMessageFormatter {

    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    private NotificationMessageFormatter() {
    }

    /** Formats {@code amount} as a whole-peso figure with {@code .} thousands separators (e.g. {@code "45.000"}). */
    static String formatAmount(BigDecimal amount) {
        BigDecimal safeAmount = amount != null ? amount : BigDecimal.ZERO;
        BigDecimal rounded = safeAmount.setScale(0, RoundingMode.HALF_UP);
        DecimalFormatSymbols symbols = new DecimalFormatSymbols(Locale.ROOT);
        symbols.setGroupingSeparator('.');
        DecimalFormat format = new DecimalFormat("#,###", symbols);
        return format.format(rounded);
    }

    /** Formats {@code date} as {@code dd/MM/yyyy} (e.g. {@code "14/07/2026"}). */
    static String formatDate(LocalDate date) {
        return date.format(DATE_FORMAT);
    }
}
