package com.smartfinance.backend.extractos.model.dto;

/**
 * Respuesta de {@code POST /api/statement-imports/confirm}.
 *
 * @param createdCount cantidad de ingresos/gastos creados
 */
public record StatementImportResultResponse(int createdCount) {
}
