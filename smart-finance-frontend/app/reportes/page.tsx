"use client"

import { useEffect, useMemo, useState } from "react"
import { Download, FileBarChart, PiggyBank, TrendingDown, TrendingUp } from "lucide-react"
import { toast } from "sonner"
import { AppLayout } from "@/components/layout/app-layout"
import { ExpensesByCategoryChart } from "@/components/dashboard/expenses-by-category-chart"
import { IncomeExpensesChart } from "@/components/dashboard/income-expenses-chart"
import { Button } from "@/components/ui/button"
import { Empty, EmptyDescription, EmptyHeader, EmptyMedia, EmptyTitle } from "@/components/ui/empty"
import { Select, SelectContent, SelectItem, SelectTrigger, SelectValue } from "@/components/ui/select"
import { Skeleton } from "@/components/ui/skeleton"
import { Table, TableBody, TableCell, TableHead, TableHeader, TableRow } from "@/components/ui/table"
import { useMonthlyReport, useReportMovements } from "@/hooks/use-report"
import { toastApiError } from "@/lib/api-client"
import { exportReport } from "@/lib/services/report.service"
import type { PaymentMethodType } from "@/lib/types/expense"
import type { ReportMovementRow } from "@/lib/types/report"
import { ALL_PERIODS_VALUE, getCurrentPeriodValue, getRecentPeriodOptions } from "@/lib/utils/period"

const periodOptions = getRecentPeriodOptions().filter((option) => option.value !== ALL_PERIODS_VALUE)

const movementTypeLabels: Record<ReportMovementRow["type"], string> = {
  INCOME: "Ingreso",
  EXPENSE: "Gasto",
}

const paymentMethodLabels: Record<PaymentMethodType, string> = {
  CASH: "Efectivo",
  DEBIT_CARD: "Tarjeta de Debito",
  CREDIT_CARD: "Tarjeta de Credito",
  TRANSFER: "Transferencia",
  OTHER: "Otro",
}

function parsePeriod(period: string): { year: number; month: number } {
  const [year, month] = period.split("-").map(Number)
  return { year, month }
}

function triggerBlobDownload(blob: Blob, filename: string) {
  const url = window.URL.createObjectURL(blob)
  const anchor = document.createElement("a")
  anchor.href = url
  anchor.download = filename
  document.body.appendChild(anchor)
  anchor.click()
  anchor.remove()
  window.URL.revokeObjectURL(url)
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
        {/* Header */}
        <div className="flex flex-col sm:flex-row sm:items-center sm:justify-between gap-4">
          <div>
            <h1 className="text-2xl font-bold text-foreground">Reportes</h1>
            <p className="text-sm text-muted-foreground">
              Analiza tu comportamiento financiero
            </p>
          </div>
          <div className="flex items-center gap-3">
            <Select value={period} onValueChange={setPeriod}>
              <SelectTrigger className="w-[200px]">
                <SelectValue placeholder="Selecciona un periodo" />
              </SelectTrigger>
              <SelectContent>
                {periodOptions.map((option) => (
                  <SelectItem key={option.value} value={option.value}>
                    {option.label}
                  </SelectItem>
                ))}
              </SelectContent>
            </Select>
            <Button
              className="gap-2 bg-primary text-primary-foreground hover:bg-primary/90"
              onClick={() => void handleExport()}
              disabled={isExporting || isLoading}
            >
              <Download className="h-4 w-4" />
              {isExporting ? "Exportando..." : "Exportar CSV"}
            </Button>
          </div>
        </div>

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
            <div className="grid grid-cols-1 md:grid-cols-3 gap-4">
              <div className="rounded-xl bg-card border border-border p-5">
                <div className="flex items-center gap-3">
                  <div className="flex h-10 w-10 items-center justify-center rounded-lg bg-success/10">
                    <TrendingUp className="h-5 w-5 text-success" />
                  </div>
                  <div>
                    <p className="text-sm text-muted-foreground">Total Ingresos</p>
                    <p className="text-xl font-bold text-success">
                      ${report.totalIncome.toLocaleString("es-MX")}
                    </p>
                  </div>
                </div>
              </div>

              <div className="rounded-xl bg-card border border-border p-5">
                <div className="flex items-center gap-3">
                  <div className="flex h-10 w-10 items-center justify-center rounded-lg bg-destructive/10">
                    <TrendingDown className="h-5 w-5 text-destructive" />
                  </div>
                  <div>
                    <p className="text-sm text-muted-foreground">Total Gastos</p>
                    <p className="text-xl font-bold text-destructive">
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
                  <div className="flex h-10 w-10 items-center justify-center rounded-lg bg-primary/10">
                    <PiggyBank className="h-5 w-5 text-primary" />
                  </div>
                  <div>
                    <p className="text-sm text-muted-foreground">Ahorro</p>
                    <p className="text-xl font-bold text-primary">
                      ${report.savings.toLocaleString("es-MX")}
                    </p>
                  </div>
                </div>
              </div>
            </div>

            {/* Charts Row */}
            <div className="grid grid-cols-1 lg:grid-cols-2 gap-6">
              <IncomeExpensesChart series={report.monthlySeries} />
              <ExpensesByCategoryChart topCategories={report.topCategories} />
            </div>

            {/* Movements Table */}
            <div className="space-y-3">
              <h2 className="text-lg font-semibold text-foreground">Movimientos del periodo</h2>
              <div className="rounded-xl border border-border bg-card">
                <Table>
                  <TableHeader>
                    <TableRow>
                      <TableHead>Fecha</TableHead>
                      <TableHead>Tipo</TableHead>
                      <TableHead>Categoria</TableHead>
                      <TableHead>Descripcion</TableHead>
                      <TableHead>Monto</TableHead>
                      <TableHead>Metodo de pago</TableHead>
                    </TableRow>
                  </TableHeader>
                  <TableBody>
                    {isLoadingMovements &&
                      Array.from({ length: 4 }).map((_, index) => (
                        <TableRow key={`movement-skeleton-${index}`}>
                          <TableCell colSpan={6}>
                            <Skeleton className="h-8 w-full" />
                          </TableCell>
                        </TableRow>
                      ))}

                    {!isLoadingMovements && movements.length === 0 && (
                      <TableRow>
                        <TableCell colSpan={6} className="py-6 text-center text-muted-foreground">
                          No hay movimientos para los filtros seleccionados.
                        </TableCell>
                      </TableRow>
                    )}

                    {!isLoadingMovements &&
                      movements.map((movement, index) => (
                        <TableRow key={`${movement.date}-${index}`}>
                          <TableCell>{movement.date}</TableCell>
                          <TableCell>{movementTypeLabels[movement.type]}</TableCell>
                          <TableCell>{movement.categoryName || "Sin categoria"}</TableCell>
                          <TableCell>{movement.description || "Sin descripcion"}</TableCell>
                          <TableCell
                            className={
                              movement.type === "INCOME"
                                ? "font-semibold text-success"
                                : "font-semibold text-destructive"
                            }
                          >
                            {movement.type === "INCOME" ? "+" : "-"}$
                            {movement.amount.toLocaleString("es-MX")}
                          </TableCell>
                          <TableCell>
                            {movement.paymentMethod ? paymentMethodLabels[movement.paymentMethod] : "-"}
                          </TableCell>
                        </TableRow>
                      ))}
                  </TableBody>
                </Table>
              </div>
            </div>
          </>
        )}
      </div>
    </AppLayout>
  )
}
