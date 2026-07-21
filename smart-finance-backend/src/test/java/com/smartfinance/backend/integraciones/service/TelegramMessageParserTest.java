package com.smartfinance.backend.integraciones.service;

import com.smartfinance.backend.integraciones.exception.TelegramImplausibleMovementException;
import com.smartfinance.backend.integraciones.exception.TelegramMessageParseException;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class TelegramMessageParserTest {

    private final TelegramMessageParser parser = new TelegramMessageParser();

    @Test
    void parseExtractsAmountAfterDescription() {
        TelegramMessageParser.ParsedMessage result = parser.parse("Uber 15000");

        assertThat(result.amount()).isEqualByComparingTo(BigDecimal.valueOf(15000));
        assertThat(result.description()).isEqualTo("Uber");
    }

    @Test
    void parseExtractsAmountBeforeDescription() {
        TelegramMessageParser.ParsedMessage result = parser.parse("15000 Uber");

        assertThat(result.amount()).isEqualByComparingTo(BigDecimal.valueOf(15000));
        assertThat(result.description()).isEqualTo("Uber");
    }

    @Test
    void parseStripsThousandsSeparatorFromAmount() {
        TelegramMessageParser.ParsedMessage result = parser.parse("15.000 uber");

        assertThat(result.amount()).isEqualByComparingTo(BigDecimal.valueOf(15000));
        assertThat(result.description()).isEqualTo("uber");
    }

    @Test
    void parseThrowsWhenTextHasNoNumber() {
        assertThatThrownBy(() -> parser.parse("Uber"))
                .isInstanceOf(TelegramMessageParseException.class);
    }

    @Test
    void parseThrowsWhenAmountIsZero() {
        assertThatThrownBy(() -> parser.parse("Uber 0"))
                .isInstanceOf(TelegramMessageParseException.class);
    }

    @Test
    void parseThrowsImplausibleMovementWhenOnlyANumberIsSentWithNoDescription() {
        // Cambio de comportamiento respecto a versiones anteriores: antes de agregar el filtro de
        // plausibilidad, este caso caía al texto completo ("15000") como descripción y se
        // registraba igual. Ahora, al no tener ninguna letra, se rechaza como ruido — igual que
        // "15000 12345" (ver parseThrowsImplausibleMovementWhenDescriptionHasNoLetters).
        assertThatThrownBy(() -> parser.parse("15000"))
                .isInstanceOf(TelegramImplausibleMovementException.class);
    }

    @Test
    void parseThrowsImplausibleMovementWhenDescriptionHasNoLetters() {
        assertThatThrownBy(() -> parser.parse("15000 12345"))
                .isInstanceOf(TelegramImplausibleMovementException.class);
    }

    @Test
    void parseThrowsImplausibleMovementWhenAmountIsBelowMinimum() {
        assertThatThrownBy(() -> parser.parse("Uber 50"))
                .isInstanceOf(TelegramImplausibleMovementException.class);
    }

    @Test
    void parseThrowsImplausibleMovementWhenAmountIsAboveMaximum() {
        assertThatThrownBy(() -> parser.parse("Casa 600000000"))
                .isInstanceOf(TelegramImplausibleMovementException.class);
    }

    @Test
    void parseAcceptsAValidExpenseMessageAtTheBoundaryOfPlausibleAmounts() {
        TelegramMessageParser.ParsedMessage result = parser.parse("Uber 15000");

        assertThat(result.amount()).isEqualByComparingTo(BigDecimal.valueOf(15000));
        assertThat(result.description()).isEqualTo("Uber");
    }

    @Test
    void parseAcceptsAnIncomeFlavoredMessageWithoutCaringAboutMovementType() {
        TelegramMessageParser.ParsedMessage result = parser.parse("Me pagaron 50000");

        assertThat(result.amount()).isEqualByComparingTo(BigDecimal.valueOf(50000));
        assertThat(result.description()).isEqualTo("Me pagaron");
    }
}
