"use client"

import { useEffect, useMemo, useState } from "react"
import { FileBarChart } from "lucide-react"
import { toast } from "sonner"
import { AppLayout } from "@/components/layout/app-layout"
import { ExpensesByCategoryChart } from "@/components/dashboard/expenses-by-category-chart"
import { IncomeExpensesChart } from "@/components/dashboard/income-expenses-chart"
import { Empty, EmptyDescription, EmptyHeader, EmptyMedia, EmptyTitle } from "@/components/ui/empty"
import { Skeleton } from "@/components/ui/skeleton"
import { ReportHeader } from "@/components/reports/report-header"
import { ReportMovementsTable } from "@/components/reports/report-movements-table"
import { ReportSummaryCards } from "@/components/reports/report-summary-cards"
import { useMonthlyReport, useReportMovements } from "@/hooks/use-report"
import { toastApiError } from "@/lib/api-client"
import { triggerBlobDownload } from "@/lib/download"
import { exportReport } from "@/lib/services/report.service"
import { ALL_PERIODS_VALUE, getCurrentPeriodValue, getRecentPeriodOptions } from "@/lib/utils/period"

const periodOptions = getRecentPeriodOptions().filter((option) => option.value !== ALL_PERIODS_VALUE)

function parsePeriod(period: string): { year: number; month: number } {
  const [year, month] = period.split("-").map(Number)
  return { year, month }
}

export default function ReportesPage() {
  const [period, setPeriod] = useState(getCurrentPeriodValue())
  const [isExporting, setIsExporting] = useState(false)
  const { year, month } = useMemo(() => parsePeriod(period), [period])

  const { report, isLoading, error } = useMonthlyReport({ year, month })
  const { movements, isLoading: isLoadingMovements, error: movementsError } = useReportMovements({
    year,
    month,
  })

  useEffect(() => {
    if (error) {
      toast.error(error)
    }
  }, [error])

  useEffect(() => {
    if (movementsError) {
      toast.error(movementsError)
    }
  }, [movementsError])

  const handleExport = async () => {
    setIsExporting(true)
    try {
      const { blob, filename } = await exportReport(year, month, "csv")
      triggerBlobDownload(blob, filename)
      toast.success("Reporte exportado correctamente")
    } catch (error) {
      toastApiError(error, "No fue posible exportar el reporte")
    } finally {
      setIsExporting(false)
    }
  }

  const hasData = !!report && (report.totalIncome > 0 || report.totalExpense > 0)

  return (
    <AppLayout>
      <div className="space-y-6">
        <ReportHeader
          period={period}
          onPeriodChange={setPeriod}
          periodOptions={periodOptions}
          onExport={() => void handleExport()}
          isExporting={isExporting}
          isLoading={isLoading}
        />

        {isLoading ? (
          <div className="space-y-6">
            <div className="grid grid-cols-1 md:grid-cols-3 gap-4">
              <Skeleton className="h-24 w-full" />
              <Skeleton className="h-24 w-full" />
              <Skeleton className="h-24 w-full" />
            </div>
            <div className="grid grid-cols-1 lg:grid-cols-2 gap-6">
              <Skeleton className="h-[340px] w-full" />
              <Skeleton className="h-[340px] w-full" />
            </div>
          </div>
        ) : !report || !hasData ? (
          <Empty>
            <EmptyHeader>
              <EmptyMedia variant="icon">
                <FileBarChart />
              </EmptyMedia>
              <EmptyTitle>No hay movimientos en este periodo</EmptyTitle>
              <EmptyDescription>
                Registra ingresos o gastos en el periodo seleccionado para ver aqui tu reporte financiero.
              </EmptyDescription>
            </EmptyHeader>
          </Empty>
        ) : (
          <>
            {/* Summary Cards */}
            <ReportSummaryCards report={report} />

            {/* Charts Row */}
            <div className="grid grid-cols-1 lg:grid-cols-2 gap-6">
              <IncomeExpensesChart series={report.monthlySeries} />
              <ExpensesByCategoryChart topCategories={report.topCategories} />
            </div>

            {/* Movements Table */}
            <ReportMovementsTable movements={movements} isLoadingMovements={isLoadingMovements} />
          </>
        )}
      </div>
    </AppLayout>
  )
}
