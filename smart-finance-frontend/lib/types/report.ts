import type { MonthlySeriesPoint, TopCategory } from "./analysis"
import type { PaymentMethodType } from "./expense"

/**
 * Response shape of `GET /api/reports/monthly`. Reuses {@link TopCategory} and
 * {@link MonthlySeriesPoint} from the analysis types since the backend's `MonthlyReportResponse`
 * intentionally mirrors `AnalysisSummaryResponse`'s shape for those fields (see
 * `MonthlyReportResponse` javadoc on the backend).
 */
export interface MonthlyReport {
  periodYear: number
  periodMonth: number
  totalIncome: number
  totalExpense: number
  savings: number
  /** Fraction, not a percentage — multiply by 100 before rendering (e.g. 0.70 -> "70%"). */
  expenseRatio: number
  /** Fraction, not a percentage — multiply by 100 before rendering. */
  debtRatio: number
  topCategories: TopCategory[]
  monthlySeries: MonthlySeriesPoint[]
}

export const REPORT_EXPORT_FORMATS = ["csv", "json"] as const

export type ReportExportFormat = (typeof REPORT_EXPORT_FORMATS)[number]

/**
 * A single row returned by `GET /api/reports/movements`. Same shape as the rows in the
 * `GET /api/reports/export?format=json` payload, but meant for on-screen rendering rather
 * than a file download.
 */
export interface ReportMovementRow {
  date: string
  type: "INCOME" | "EXPENSE"
  categoryName: string | null
  description: string | null
  amount: number
  paymentMethod: PaymentMethodType | null
}
