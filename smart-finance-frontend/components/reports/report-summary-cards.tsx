"use client"

import { PiggyBank, TrendingDown, TrendingUp } from "lucide-react"
import type { MonthlyReport } from "@/lib/types/report"

interface ReportSummaryCardsProps {
  report: MonthlyReport
}

export function ReportSummaryCards({ report }: ReportSummaryCardsProps) {
  return (
    <div className="grid grid-cols-1 md:grid-cols-3 gap-4">
      <div className="rounded-xl bg-card border border-border p-5">
        <div className="flex items-center gap-3">
          <div className="flex h-10 w-10 shrink-0 items-center justify-center rounded-lg bg-success/10">
            <TrendingUp className="h-5 w-5 text-success" />
          </div>
          <div className="min-w-0">
            <p className="text-sm text-muted-foreground">Total Ingresos</p>
            <p className="text-xl font-bold text-success truncate">
              ${report.totalIncome.toLocaleString("es-MX")}
            </p>
          </div>
        </div>
      </div>

      <div className="rounded-xl bg-card border border-border p-5">
        <div className="flex items-center gap-3">
          <div className="flex h-10 w-10 shrink-0 items-center justify-center rounded-lg bg-destructive/10">
            <TrendingDown className="h-5 w-5 text-destructive" />
          </div>
          <div className="min-w-0">
            <p className="text-sm text-muted-foreground">Total Gastos</p>
            <p className="text-xl font-bold text-destructive truncate">
              ${report.totalExpense.toLocaleString("es-MX")}
            </p>
            <p className="text-xs text-muted-foreground">
              {Math.round(report.expenseRatio * 100)}% de tus ingresos
            </p>
          </div>
        </div>
      </div>

      <div className="rounded-xl bg-card border border-border p-5">
        <div className="flex items-center gap-3">
          <div className="flex h-10 w-10 shrink-0 items-center justify-center rounded-lg bg-primary/10">
            <PiggyBank className="h-5 w-5 text-primary" />
          </div>
          <div className="min-w-0">
            <p className="text-sm text-muted-foreground">Ahorro</p>
            <p className="text-xl font-bold text-primary truncate">
              ${report.savings.toLocaleString("es-MX")}
            </p>
          </div>
        </div>
      </div>
    </div>
  )
}
