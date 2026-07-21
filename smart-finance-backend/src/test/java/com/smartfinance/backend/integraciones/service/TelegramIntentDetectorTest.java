package com.smartfinance.backend.integraciones.service;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class TelegramIntentDetectorTest {

    @Test
    void looksLikeSummaryQueryReturnsFalseForARegistrationLookingMessage() {
        assertThat(TelegramIntentDetector.looksLikeSummaryQuery("Uber 15000")).isFalse();
    }

    @Test
    void looksLikeSummaryQueryReturnsTrueForAQuestionWithAQuestionMark() {
        assertThat(TelegramIntentDetector.looksLikeSummaryQuery("¿Cuánto gasté en comida?")).isTrue();
    }

    @Test
    void looksLikeSummaryQueryIsAccentAndPunctuationInsensitive() {
        assertThat(TelegramIntentDetector.looksLikeSummaryQuery("cuanto gaste")).isTrue();
    }

    @Test
    void looksLikeSummaryQueryReturnsTrueForResumen() {
        assertThat(TelegramIntentDetector.looksLikeSummaryQuery("resumen")).isTrue();
    }

    @Test
    void looksLikeSummaryQueryReturnsTrueForBalance() {
        assertThat(TelegramIntentDetector.looksLikeSummaryQuery("balance")).isTrue();
    }

    @Test
    void looksLikeSummaryQueryReturnsFalseForAnIncomeRegistrationLookingMessage() {
        assertThat(TelegramIntentDetector.looksLikeSummaryQuery("Me pagaron 50000")).isFalse();
    }

    @Test
    void looksLikeSummaryQueryReturnsTrueForTheWordCuantoAlone() {
        assertThat(TelegramIntentDetector.looksLikeSummaryQuery("cuanto")).isTrue();
    }

    @Test
    void looksLikeSummaryQueryReturnsTrueWhenTheTotalKeywordIsMentioned() {
        assertThat(TelegramIntentDetector.looksLikeSummaryQuery("gaste en total esta semana")).isTrue();
    }

    @Test
    void looksLikeSummaryQueryReturnsFalseForNullText() {
        assertThat(TelegramIntentDetector.looksLikeSummaryQuery(null)).isFalse();
    }
}
