package com.smartfinance.backend.config;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * Enables Spring's {@code @Scheduled} method execution for the Sprint 5 (Batch 3) native
 * automations ({@code PaymentReminderJob}, {@code WeeklySummaryJob},
 * {@code InactivityReminderJob}, {@code MonthEndPredictionJob}) that replace the original n8n
 * workflows (see {@code docs/sprints/sprint5.md}, architecture decision 1).
 *
 * <p>Gated behind {@code app.jobs.enabled} (defaults to {@code true}) so integration tests and
 * local runs that don't need background jobs firing can disable the whole scheduling
 * infrastructure with a single property, instead of disabling each job individually.
 */
@Configuration
@EnableScheduling
@ConditionalOnProperty(prefix = "app.jobs", name = "enabled", havingValue = "true", matchIfMissing = true)
public class SchedulingConfig {
}
