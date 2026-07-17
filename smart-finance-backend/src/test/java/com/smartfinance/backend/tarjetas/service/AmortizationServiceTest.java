package com.smartfinance.backend.tarjetas.service;

import com.smartfinance.backend.tarjetas.model.entity.CardFranchise;
import com.smartfinance.backend.tarjetas.model.entity.CreditCard;
import com.smartfinance.backend.tarjetas.model.entity.Installment;
import com.smartfinance.backend.tarjetas.model.entity.InstallmentStatus;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

class AmortizationServiceTest {

    private final AmortizationService amortizationService = new AmortizationService();

    @Test
    void buildScheduleShouldMatchRappiExampleExactly() {
        // Caso real de docs/rediseno-deudas-tarjetas.md: TV a 3 cuotas de $700.000, tasa 2,1%
        // E.M. Números verificados a mano por la revisión de diseño (#229).
        CreditCard card = buildCard(5);
        List<Installment> schedule = amortizationService.buildSchedule(
                BigDecimal.valueOf(700_000), 3, new BigDecimal("0.021"), LocalDate.of(2026, 6, 1), card
        );

        Assertions.assertEquals(3, schedule.size());

        Installment first = schedule.get(0);
        Assertions.assertEquals(1, first.getNumber());
        Assertions.assertEquals(0, new BigDecimal("233333.33").compareTo(first.getCapitalAmount()));
        Assertions.assertEquals(0, new BigDecimal("14700.00").compareTo(first.getInterestAmount()));

        Installment second = schedule.get(1);
        Assertions.assertEquals(2, second.getNumber());
        Assertions.assertEquals(0, new BigDecimal("233333.33").compareTo(second.getCapitalAmount()));
        Assertions.assertEquals(0, new BigDecimal("9800.00").compareTo(second.getInterestAmount()));

        Installment third = schedule.get(2);
        Assertions.assertEquals(3, third.getNumber());
        Assertions.assertEquals(0, new BigDecimal("233333.34").compareTo(third.getCapitalAmount()));
        Assertions.assertEquals(0, new BigDecimal("4900.00").compareTo(third.getInterestAmount()));

        BigDecimal totalCapital = schedule.stream()
                .map(Installment::getCapitalAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        Assertions.assertEquals(0, new BigDecimal("700000.00").compareTo(totalCapital));

        schedule.forEach(installment -> Assertions.assertEquals(InstallmentStatus.PENDING, installment.getStatus()));
    }

    @Test
    void buildScheduleShouldProduceStrictlyDecreasingInterest() {
        CreditCard card = buildCard(10);
        List<Installment> schedule = amortizationService.buildSchedule(
                BigDecimal.valueOf(700_000), 3, new BigDecimal("0.021"), LocalDate.of(2026, 6, 1), card
        );

        Assertions.assertTrue(schedule.get(0).getInterestAmount().compareTo(schedule.get(1).getInterestAmount()) > 0);
        Assertions.assertTrue(schedule.get(1).getInterestAmount().compareTo(schedule.get(2).getInterestAmount()) > 0);
    }

    @Test
    void buildScheduleShouldAssignConsecutiveMonthlyDueDatesStartingAtNextCutoff() {
        // cutoffDay=15, compra el 1/6/2026 -> primera cuota vence el próximo 15/6/2026 (no pasó
        // todavía), siguientes +1 mes cada una.
        CreditCard card = buildCard(15);
        List<Installment> schedule = amortizationService.buildSchedule(
                BigDecimal.valueOf(300_000), 3, new BigDecimal("0.02"), LocalDate.of(2026, 6, 1), card
        );

        Assertions.assertEquals(LocalDate.of(2026, 6, 15), schedule.get(0).getDueDate());
        Assertions.assertEquals(LocalDate.of(2026, 7, 15), schedule.get(1).getDueDate());
        Assertions.assertEquals(LocalDate.of(2026, 8, 15), schedule.get(2).getDueDate());
    }

    @Test
    void buildScheduleShouldRollFirstDueDateToNextMonthWhenCutoffAlreadyPassed() {
        // cutoffDay=5, compra el 10/6/2026 (después del corte de junio) -> primera cuota vence en
        // julio, no en junio.
        CreditCard card = buildCard(5);
        List<Installment> schedule = amortizationService.buildSchedule(
                BigDecimal.valueOf(100_000), 2, new BigDecimal("0.02"), LocalDate.of(2026, 6, 10), card
        );

        Assertions.assertEquals(LocalDate.of(2026, 7, 5), schedule.get(0).getDueDate());
        Assertions.assertEquals(LocalDate.of(2026, 8, 5), schedule.get(1).getDueDate());
    }

    @Test
    void buildScheduleShouldClampCutoffToLastDayOfShorterMonth() {
        // cutoffDay=31, compra el 5/2/2026 (febrero tiene 28 días en 2026) -> primer corte cae el
        // 28/2/2026, no el 31 (spec "Cierre de ciclo en meses de distinta duración").
        CreditCard card = buildCard(31);
        List<Installment> schedule = amortizationService.buildSchedule(
                BigDecimal.valueOf(100_000), 2, new BigDecimal("0.02"), LocalDate.of(2026, 2, 5), card
        );

        Assertions.assertEquals(LocalDate.of(2026, 2, 28), schedule.get(0).getDueDate());
    }

    @Test
    void buildScheduleShouldRejectSingleInstallmentWithoutNegativeCapital() {
        // n=1 es un caso límite válido (aunque en la práctica CardMovementService nunca llama a
        // este método con installmentCount=1 — ver spec "Compra a 1 cuota"): capitalPerInstallment
        // = amount, sin residuo, sin resguardo disparado.
        CreditCard card = buildCard(5);
        List<Installment> schedule = amortizationService.buildSchedule(
                BigDecimal.valueOf(50_000), 1, new BigDecimal("0.02"), LocalDate.of(2026, 6, 1), card
        );

        Assertions.assertEquals(1, schedule.size());
        Assertions.assertEquals(0, new BigDecimal("50000.00").compareTo(schedule.get(0).getCapitalAmount()));
        Assertions.assertEquals(0, new BigDecimal("1000.00").compareTo(schedule.get(0).getInterestAmount()));
    }

    @Test
    void buildScheduleShouldRejectWhenLastInstallmentCapitalWouldBeNegative() {
        // Caso exacto de la revisión de diseño (warning #1): P=$0,35, n=48.
        // capitalPerInstallment = 0.35/48 redondeado HALF_UP a 2 decimales = 0.01
        // 0.01 * 48 = 0.48 > 0.35 -> roundingRemainder = 0.35 - 0.48 = -0.13
        // capital de la última cuota = 0.01 + (-0.13) = -0.12 (negativo) -> debe rechazar
        CreditCard card = buildCard(5);

        IllegalArgumentException exception = Assertions.assertThrows(
                IllegalArgumentException.class,
                () -> amortizationService.buildSchedule(
                        new BigDecimal("0.35"), 48, new BigDecimal("0.02"), LocalDate.of(2026, 6, 1), card
                )
        );
        Assertions.assertEquals(
                "El monto de la compra es demasiado bajo para la cantidad de cuotas seleccionada",
                exception.getMessage()
        );
    }

    @Test
    void buildScheduleShouldRejectWhenCapitalPerInstallmentRoundsToZero() {
        // P=$0,001, n=2 -> capitalPerInstallment = 0.001/2 = 0.0005, redondeado HALF_UP a 2
        // decimales = 0.00 -> capital fijo cero, rechazado antes de construir ninguna cuota.
        CreditCard card = buildCard(5);

        IllegalArgumentException exception = Assertions.assertThrows(
                IllegalArgumentException.class,
                () -> amortizationService.buildSchedule(
                        new BigDecimal("0.001"), 2, new BigDecimal("0.02"), LocalDate.of(2026, 6, 1), card
                )
        );
        Assertions.assertEquals(
                "El monto de la compra es demasiado bajo para la cantidad de cuotas seleccionada",
                exception.getMessage()
        );
    }

    @Test
    void buildScheduleShouldRoundRemainderToLastInstallmentSoCapitalSumsExactlyToAmount() {
        // 100/3 no es exacto: capitalPerInstallment=33.33, residuo=100-99.99=0.01, va a la última.
        CreditCard card = buildCard(5);
        List<Installment> schedule = amortizationService.buildSchedule(
                BigDecimal.valueOf(100), 3, new BigDecimal("0.01"), LocalDate.of(2026, 6, 1), card
        );

        Assertions.assertEquals(0, new BigDecimal("33.33").compareTo(schedule.get(0).getCapitalAmount()));
        Assertions.assertEquals(0, new BigDecimal("33.33").compareTo(schedule.get(1).getCapitalAmount()));
        Assertions.assertEquals(0, new BigDecimal("33.34").compareTo(schedule.get(2).getCapitalAmount()));

        BigDecimal totalCapital = schedule.stream()
                .map(Installment::getCapitalAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        Assertions.assertEquals(0, new BigDecimal("100.00").compareTo(totalCapital));
    }

    private CreditCard buildCard(int cutoffDay) {
        CreditCard card = new CreditCard();
        card.setId(1L);
        card.setName("Tarjeta Visa");
        card.setFranchise(CardFranchise.VISA);
        card.setCreditLimit(BigDecimal.valueOf(5_000_000));
        card.setMonthlyRate(BigDecimal.valueOf(0.021));
        card.setCutoffDay(cutoffDay);
        card.setPaymentDueDay(20);
        card.setCurrentBalance(BigDecimal.ZERO);
        return card;
    }
}
