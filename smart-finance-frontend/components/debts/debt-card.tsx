"use client"

import { AlertTriangle, Calendar, CreditCard, History, MoreVertical, Pencil, Percent, Plus, Trash2, Wallet } from "lucide-react"
import { Button } from "@/components/ui/button"
import {
  DropdownMenu,
  DropdownMenuContent,
  DropdownMenuItem,
  DropdownMenuTrigger,
} from "@/components/ui/dropdown-menu"
import { cn } from "@/lib/utils"
import type { Debt } from "@/lib/types/debt"

interface DebtCardProps {
  debt: Debt
  onEdit: (debt: Debt) => void
  onDelete: (debt: Debt) => void
  onRegisterPayment: (debt: Debt) => void
  onRegisterCharge: (debt: Debt) => void
  onViewHistory: (debt: Debt) => void
}

function formatCurrency(value: number): string {
  return `$${value.toLocaleString("es-MX", { minimumFractionDigits: 2, maximumFractionDigits: 2 })}`
}

function formatDate(value: string | null): string {
  if (!value) return "Sin fecha"
  const date = new Date(`${value}T00:00:00`)
  if (Number.isNaN(date.getTime())) return value
  return date.toLocaleDateString("es-MX", { day: "2-digit", month: "short", year: "numeric" })
}

export function DebtCard({ debt, onEdit, onDelete, onRegisterPayment, onRegisterCharge, onViewHistory }: DebtCardProps) {
  const paidAmount = debt.totalAmount - debt.remainingAmount
  const progress = debt.totalAmount > 0 ? Math.min(Math.max((paidAmount / debt.totalAmount) * 100, 0), 100) : 0
  const isPaidOff = debt.remainingAmount <= 0

  return (
    <div className="rounded-xl bg-card border border-border p-5 hover:border-primary/30 transition-smooth">
      <div className="flex items-start justify-between mb-4">
        <div className="flex min-w-0 items-center gap-3">
          <div className="flex h-10 w-10 shrink-0 items-center justify-center rounded-lg bg-warning/10">
            <CreditCard className="h-5 w-5 text-warning" />
          </div>
          <div className="min-w-0">
            <h3 className="text-sm font-semibold text-foreground truncate">{debt.name}</h3>
            <p className="text-xs text-muted-foreground truncate">
              {isPaidOff ? "Deuda saldada" : `Restante: ${formatCurrency(debt.remainingAmount)}`}
            </p>
          </div>
        </div>
        <DropdownMenu>
          <DropdownMenuTrigger asChild>
            <Button variant="ghost" size="icon-sm" aria-label="Mas opciones">
              <MoreVertical className="h-4 w-4" />
            </Button>
          </DropdownMenuTrigger>
          <DropdownMenuContent align="end">
            <DropdownMenuItem onClick={() => onEdit(debt)}>
              <Pencil className="h-4 w-4" />
              Editar
            </DropdownMenuItem>
            <DropdownMenuItem onClick={() => onViewHistory(debt)}>
              <History className="h-4 w-4" />
              Ver historial
            </DropdownMenuItem>
            <DropdownMenuItem variant="destructive" onClick={() => onDelete(debt)}>
              <Trash2 className="h-4 w-4" />
              Eliminar
            </DropdownMenuItem>
          </DropdownMenuContent>
        </DropdownMenu>
      </div>

      <div className="mb-4">
        <div className="flex items-center justify-between mb-2">
          <span className="text-xs text-muted-foreground">Progreso de pago</span>
          <span className="text-xs font-medium text-foreground">{progress.toFixed(1)}%</span>
        </div>
        <div className="h-2 rounded-full bg-secondary overflow-hidden">
          <div
            className={cn(
              "h-full rounded-full transition-all duration-500",
              progress >= 75 ? "bg-success" : progress >= 50 ? "bg-warning" : "bg-primary",
            )}
            style={{ width: `${progress}%` }}
          />
        </div>
      </div>

      <div className="grid grid-cols-2 gap-3">
        <div className="flex items-center gap-2">
          <div className="flex h-7 w-7 shrink-0 items-center justify-center rounded-md bg-secondary">
            <CreditCard className="h-3.5 w-3.5 text-muted-foreground" />
          </div>
          <div className="min-w-0">
            <p className="text-xs text-muted-foreground">Total</p>
            <p className="text-sm font-semibold text-foreground truncate">{formatCurrency(debt.totalAmount)}</p>
          </div>
        </div>

        <div className="flex items-center gap-2">
          <div className="flex h-7 w-7 shrink-0 items-center justify-center rounded-md bg-secondary">
            <AlertTriangle className="h-3.5 w-3.5 text-muted-foreground" />
          </div>
          <div className="min-w-0">
            <p className="text-xs text-muted-foreground">Restante</p>
            <p className="text-sm font-semibold text-destructive truncate">{formatCurrency(debt.remainingAmount)}</p>
          </div>
        </div>

        <div className="flex items-center gap-2">
          <div className="flex h-7 w-7 shrink-0 items-center justify-center rounded-md bg-secondary">
            <Percent className="h-3.5 w-3.5 text-muted-foreground" />
          </div>
          <div className="min-w-0">
            <p className="text-xs text-muted-foreground">Interes</p>
            <p className="text-sm font-semibold text-foreground truncate">
              {debt.interestRate !== null ? `${debt.interestRate}%` : "N/A"}
            </p>
          </div>
        </div>

        <div className="flex items-center gap-2">
          <div className="flex h-7 w-7 shrink-0 items-center justify-center rounded-md bg-secondary">
            <Calendar className="h-3.5 w-3.5 text-muted-foreground" />
          </div>
          <div className="min-w-0">
            <p className="text-xs text-muted-foreground">Vencimiento</p>
            <p className="text-sm font-semibold text-foreground truncate">{formatDate(debt.dueDate)}</p>
          </div>
        </div>
      </div>

      <div className="mt-4 grid grid-cols-2 gap-2">
        <Button
          variant="outline"
          className="border-primary/30 bg-primary/5 text-primary hover:bg-primary/10"
          onClick={() => onRegisterPayment(debt)}
          disabled={isPaidOff}
        >
          <Wallet className="h-4 w-4" />
          {isPaidOff ? "Deuda saldada" : "Registrar pago"}
        </Button>
        <Button
          variant="outline"
          className="border-warning/30 bg-warning/5 text-warning hover:bg-warning/10"
          onClick={() => onRegisterCharge(debt)}
        >
          <Plus className="h-4 w-4" />
          Registrar cargo
        </Button>
      </div>
    </div>
  )
}
