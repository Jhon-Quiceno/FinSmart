"use client"

import { Calendar, CheckCircle2, MoreVertical, Pencil, Repeat, Trash2 } from "lucide-react"
import { Button } from "@/components/ui/button"
import {
  DropdownMenu,
  DropdownMenuContent,
  DropdownMenuItem,
  DropdownMenuTrigger,
} from "@/components/ui/dropdown-menu"
import { Switch } from "@/components/ui/switch"
import { cn } from "@/lib/utils"
import type { RecurringPayment } from "@/lib/types/recurring-payment"

interface RecurringPaymentCardProps {
  recurringPayment: RecurringPayment
  onEdit: (item: RecurringPayment) => void
  onDelete: (item: RecurringPayment) => void
  onToggle: (item: RecurringPayment) => void
  onMarkAsPaid: (item: RecurringPayment) => void
  isToggling: boolean
  isMarkingAsPaid: boolean
}

const frequencyLabels: Record<RecurringPayment["frequency"], string> = {
  MONTHLY: "Mensual",
  WEEKLY: "Semanal",
}

function formatCurrency(value: number): string {
  return `$${value.toLocaleString("es-MX", { minimumFractionDigits: 2, maximumFractionDigits: 2 })}`
}

function formatDate(value: string): string {
  const date = new Date(`${value}T00:00:00`)
  if (Number.isNaN(date.getTime())) return value
  return date.toLocaleDateString("es-MX", { day: "2-digit", month: "short", year: "numeric" })
}

export function RecurringPaymentCard({
  recurringPayment,
  onEdit,
  onDelete,
  onToggle,
  onMarkAsPaid,
  isToggling,
  isMarkingAsPaid,
}: RecurringPaymentCardProps) {
  return (
    <div
      className={cn(
        "rounded-xl bg-card border border-border p-5 hover:border-primary/30 transition-smooth",
        !recurringPayment.isActive && "opacity-70",
      )}
    >
      <div className="flex items-start justify-between mb-4">
        <div className="flex items-center gap-3">
          <div className="flex h-10 w-10 items-center justify-center rounded-lg bg-primary/10">
            <Repeat className="h-5 w-5 text-primary" />
          </div>
          <div>
            <h3 className="text-sm font-semibold text-foreground">{recurringPayment.name}</h3>
            <p className="text-xs text-muted-foreground">{frequencyLabels[recurringPayment.frequency]}</p>
          </div>
        </div>
        <DropdownMenu>
          <DropdownMenuTrigger asChild>
            <Button variant="ghost" size="icon-sm" aria-label="Mas opciones">
              <MoreVertical className="h-4 w-4" />
            </Button>
          </DropdownMenuTrigger>
          <DropdownMenuContent align="end">
            <DropdownMenuItem onClick={() => onEdit(recurringPayment)}>
              <Pencil className="h-4 w-4" />
              Editar
            </DropdownMenuItem>
            <DropdownMenuItem variant="destructive" onClick={() => onDelete(recurringPayment)}>
              <Trash2 className="h-4 w-4" />
              Eliminar
            </DropdownMenuItem>
          </DropdownMenuContent>
        </DropdownMenu>
      </div>

      <div className="mb-4">
        <p className="text-2xl font-bold text-foreground">{formatCurrency(recurringPayment.amount)}</p>
      </div>

      <div className="flex items-center gap-2 mb-4">
        <div className="flex h-7 w-7 items-center justify-center rounded-md bg-secondary">
          <Calendar className="h-3.5 w-3.5 text-muted-foreground" />
        </div>
        <div>
          <p className="text-xs text-muted-foreground">Proximo pago</p>
          <p className="text-sm font-semibold text-foreground">{formatDate(recurringPayment.nextPaymentDate)}</p>
        </div>
      </div>

      <div className="flex items-center justify-between mb-4 rounded-lg bg-secondary/50 px-3 py-2">
        <span className="text-xs font-medium text-foreground">
          {recurringPayment.isActive ? "Activo" : "Inactivo"}
        </span>
        <Switch
          checked={recurringPayment.isActive}
          disabled={isToggling}
          onCheckedChange={() => onToggle(recurringPayment)}
          aria-label={recurringPayment.isActive ? "Desactivar servicio" : "Activar servicio"}
        />
      </div>

      <Button
        variant="outline"
        className="w-full border-primary/30 bg-primary/5 text-primary hover:bg-primary/10"
        onClick={() => onMarkAsPaid(recurringPayment)}
        disabled={isMarkingAsPaid || !recurringPayment.isActive}
      >
        <CheckCircle2 className="h-4 w-4" />
        Marcar como pagado
      </Button>
    </div>
  )
}
