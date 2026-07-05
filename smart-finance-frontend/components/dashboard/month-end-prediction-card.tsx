"use client"

import { AlertTriangle, CalendarClock, TrendingDown, Wallet } from "lucide-react"
import { cn } from "@/lib/utils"
import { Skeleton } from "@/components/ui/skeleton"
import { usePrediction } from "@/hooks/use-analysis"

function formatCurrency(value: number): string {
  return `$${value.toLocaleString("es-MX", { minimumFractionDigits: 2 })}`
}

export function MonthEndPredictionCard() {
  const { data: prediction, isLoading, error } = usePrediction()

  if (isLoading) {
    return (
      <div className="rounded-xl bg-card border border-border p-6">
        <Skeleton className="h-6 w-48" />
        <Skeleton className="mt-4 h-10 w-56" />
        <Skeleton className="mt-6 h-20 w-full" />
      </div>
    )
  }

  if (error || !prediction) {
    return (
      <div className="rounded-xl bg-card border border-border p-6">
        <div className="flex items-center gap-2 mb-2">
          <div className="flex h-10 w-10 items-center justify-center rounded-lg bg-primary/10">
            <CalendarClock className="h-5 w-5 text-primary" />
          </div>
          <span className="text-sm font-medium text-muted-foreground">Prediccion fin de mes</span>
        </div>
        <p className="text-sm text-muted-foreground">
          No fue posible calcular la prediccion en este momento.
        </p>
      </div>
    )
  }

  const isAlert = prediction.alert

  return (
    <div className="relative overflow-hidden rounded-xl bg-card border border-border p-6">
      <div
        className={cn(
          "absolute inset-0 pointer-events-none bg-gradient-to-br to-transparent",
          isAlert ? "from-destructive/5" : "from-primary/5",
        )}
      />

      <div className="relative">
        <div className="flex items-center justify-between mb-4">
          <div className="flex items-center gap-2">
            <div
              className={cn(
                "flex h-10 w-10 items-center justify-center rounded-lg",
                isAlert ? "bg-destructive/10" : "bg-primary/10",
              )}
            >
              <CalendarClock className={cn("h-5 w-5", isAlert ? "text-destructive" : "text-primary")} />
            </div>
            <span className="text-sm font-medium text-muted-foreground">Prediccion fin de mes</span>
          </div>
          {isAlert && (
            <div className="flex items-center gap-1 rounded-full bg-destructive/10 px-2 py-1 text-xs font-medium text-destructive">
              <AlertTriangle className="h-3 w-3" />
              <span>Riesgo de sobregasto</span>
            </div>
          )}
        </div>

        <p className="text-xs text-muted-foreground mb-1">Balance proyectado</p>
        <p className={cn("text-3xl font-bold mb-6", isAlert ? "text-destructive" : "text-foreground")}>
          {formatCurrency(prediction.projectedBalance)}
        </p>

        <div className="grid grid-cols-2 gap-4">
          <div className="flex items-center gap-3">
            <div className="flex h-8 w-8 items-center justify-center rounded-lg bg-destructive/10">
              <TrendingDown className="h-4 w-4 text-destructive" />
            </div>
            <div>
              <p className="text-xs text-muted-foreground">Gasto proyectado</p>
              <p className="text-sm font-semibold text-foreground">
                {formatCurrency(prediction.projectedExpense)}
              </p>
            </div>
          </div>
          <div className="flex items-center gap-3">
            <div className="flex h-8 w-8 items-center justify-center rounded-lg bg-secondary">
              <Wallet className="h-4 w-4 text-muted-foreground" />
            </div>
            <div>
              <p className="text-xs text-muted-foreground">Promedio diario</p>
              <p className="text-sm font-semibold text-foreground">
                {formatCurrency(prediction.avgDailySpend)}
              </p>
            </div>
          </div>
          <div>
            <p className="text-xs text-muted-foreground">Maximo diario recomendado</p>
            <p className="text-sm font-semibold text-foreground">
              {formatCurrency(prediction.recommendedDailyMax)}
            </p>
          </div>
          <div>
            <p className="text-xs text-muted-foreground">Dias restantes</p>
            <p className="text-sm font-semibold text-foreground">{prediction.daysRemaining}</p>
          </div>
        </div>
      </div>
    </div>
  )
}
