"use client"

import { CreditCard, Calendar, Percent, AlertTriangle } from "lucide-react"
import { cn } from "@/lib/utils"

export interface Debt {
  id: number
  name: string
  totalAmount: number
  paidAmount: number
  interestRate: number
  dueDate: string
  minimumPayment: number
  priority: "high" | "medium" | "low"
}

interface DebtCardProps {
  debt: Debt
}

export function DebtCard({ debt }: DebtCardProps) {
  const progress = (debt.paidAmount / debt.totalAmount) * 100
  const remaining = debt.totalAmount - debt.paidAmount

  const priorityStyles = {
    high: "bg-destructive/10 text-destructive border-destructive/30",
    medium: "bg-warning/10 text-warning border-warning/30",
    low: "bg-success/10 text-success border-success/30",
  }

  const priorityLabels = {
    high: "Alta",
    medium: "Media",
    low: "Baja",
  }

  return (
    <div className="rounded-xl bg-card border border-border p-5 hover:border-primary/30 transition-smooth">
      <div className="flex items-start justify-between mb-4">
        <div className="flex items-center gap-3">
          <div className="flex h-10 w-10 items-center justify-center rounded-lg bg-warning/10">
            <CreditCard className="h-5 w-5 text-warning" />
          </div>
          <div>
            <h3 className="text-sm font-semibold text-foreground">{debt.name}</h3>
            <p className="text-xs text-muted-foreground">
              Pago minimo: ${debt.minimumPayment.toLocaleString("es-MX")}
            </p>
          </div>
        </div>
        <span className={cn(
          "text-xs font-medium px-2 py-1 rounded-md border",
          priorityStyles[debt.priority]
        )}>
          {priorityLabels[debt.priority]}
        </span>
      </div>

      {/* Progress bar */}
      <div className="mb-4">
        <div className="flex items-center justify-between mb-2">
          <span className="text-xs text-muted-foreground">Progreso de pago</span>
          <span className="text-xs font-medium text-foreground">{progress.toFixed(1)}%</span>
        </div>
        <div className="h-2 rounded-full bg-secondary overflow-hidden">
          <div
            className={cn(
              "h-full rounded-full transition-all duration-500",
              progress >= 75 ? "bg-success" : progress >= 50 ? "bg-warning" : "bg-primary"
            )}
            style={{ width: `${progress}%` }}
          />
        </div>
      </div>

      {/* Details */}
      <div className="grid grid-cols-2 gap-3">
        <div className="flex items-center gap-2">
          <div className="flex h-7 w-7 items-center justify-center rounded-md bg-secondary">
            <CreditCard className="h-3.5 w-3.5 text-muted-foreground" />
          </div>
          <div>
            <p className="text-xs text-muted-foreground">Total</p>
            <p className="text-sm font-semibold text-foreground">
              ${debt.totalAmount.toLocaleString("es-MX")}
            </p>
          </div>
        </div>

        <div className="flex items-center gap-2">
          <div className="flex h-7 w-7 items-center justify-center rounded-md bg-secondary">
            <AlertTriangle className="h-3.5 w-3.5 text-muted-foreground" />
          </div>
          <div>
            <p className="text-xs text-muted-foreground">Restante</p>
            <p className="text-sm font-semibold text-destructive">
              ${remaining.toLocaleString("es-MX")}
            </p>
          </div>
        </div>

        <div className="flex items-center gap-2">
          <div className="flex h-7 w-7 items-center justify-center rounded-md bg-secondary">
            <Percent className="h-3.5 w-3.5 text-muted-foreground" />
          </div>
          <div>
            <p className="text-xs text-muted-foreground">Interes</p>
            <p className="text-sm font-semibold text-foreground">{debt.interestRate}%</p>
          </div>
        </div>

        <div className="flex items-center gap-2">
          <div className="flex h-7 w-7 items-center justify-center rounded-md bg-secondary">
            <Calendar className="h-3.5 w-3.5 text-muted-foreground" />
          </div>
          <div>
            <p className="text-xs text-muted-foreground">Vencimiento</p>
            <p className="text-sm font-semibold text-foreground">{debt.dueDate}</p>
          </div>
        </div>
      </div>

      {/* Action button */}
      <button className="mt-4 w-full rounded-lg border border-primary/30 bg-primary/5 py-2 text-sm font-medium text-primary hover:bg-primary/10 transition-smooth">
        Realizar Pago
      </button>
    </div>
  )
}
