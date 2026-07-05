import { apiClient } from "../api-client"
import type { MonthlyReport, ReportExportFormat, ReportMovementRow } from "../types/report"

export interface MonthlyReportFilters {
  year?: number
  month?: number
}

export async function getMonthlyReport(filters: MonthlyReportFilters = {}): Promise<MonthlyReport> {
  const response = await apiClient.get<MonthlyReport>("/api/reports/monthly", {
    params: filters,
  })

  return response.data
}

export async function getReportMovements(
  filters: MonthlyReportFilters = {},
): Promise<ReportMovementRow[]> {
  const response = await apiClient.get<ReportMovementRow[]>("/api/reports/movements", {
    params: filters,
  })

  return response.data
}

export interface ReportExportResult {
  blob: Blob
  filename: string
}

const CONTENT_DISPOSITION_FILENAME_REGEX = /filename="?([^";]+)"?/i

function resolveFilename(
  contentDisposition: string | undefined,
  year: number,
  month: number,
  format: ReportExportFormat,
): string {
  const match = contentDisposition?.match(CONTENT_DISPOSITION_FILENAME_REGEX)
  if (match?.[1]) {
    return match[1]
  }

  return `finsmart-${year}-${String(month).padStart(2, "0")}.${format}`
}

/**
 * Downloads the monthly movements export as a blob. Kept as a thin API wrapper — triggering the
 * actual browser download (object URL + synthetic anchor click) is the caller's responsibility,
 * matching how other services stay pure API calls.
 */
export async function exportReport(
  year: number,
  month: number,
  format: ReportExportFormat = "csv",
): Promise<ReportExportResult> {
  const response = await apiClient.get<Blob>("/api/reports/export", {
    params: { year, month, format },
    responseType: "blob",
  })

  return {
    blob: response.data,
    filename: resolveFilename(response.headers["content-disposition"], year, month, format),
  }
}
