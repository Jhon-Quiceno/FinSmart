package com.smartfinance.backend.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Clock;

/**
 * Provides the {@link Clock} injected into every date-sensitive Sprint 5 (Batch 3) automation —
 * the scheduled jobs, {@code OverspendAlertListener} and {@code MonthEndPredictionService} — so
 * tests can substitute a fixed clock instead of depending on {@link java.time.LocalDate#now()}.
 *
 * <p>No other part of the codebase depends on this bean; existing code keeps calling
 * {@code LocalDate.now()}/{@code Instant.now()} directly and is out of scope for this change.
 */
@Configuration
public class ClockConfig {

    @Bean
    public Clock clock() {
        return Clock.systemDefaultZone();
    }
}
