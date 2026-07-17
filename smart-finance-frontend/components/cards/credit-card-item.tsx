"use client"

import { Calendar, CreditCard as CreditCardIcon, History, ListPlus, MoreVertical, Pencil, Percent, Trash2, Wallet } from "lucide-react"
import { Button } from "@/components/ui/button"
import {
  DropdownMenu,
  DropdownMenuContent,
  DropdownMenuItem,
  DropdownMenuTrigger,
} from "@/components/ui/dropdown-menu"
import { cn } from "@/lib/utils"
import type { CardFranchise, CreditCard } from "@/lib/types/credit-card"

interface CreditCardItemProps {
  card: CreditCard
  onEdit: (card: CreditCard) => void
  onDelete: (card: CreditCard) => void
  onRegisterPurchase: (card: CreditCard) => void
  onRegisterPayment: (card: CreditCard) => void
  onViewMovements: (card: CreditCard) => void
}

const franchiseLabels: Record<CardFranchise, string> = {
  VISA: "Visa",
  MASTERCARD: "Mastercard",
  AMEX: "Amex",
  DINERS: "Diners",
}

function formatCurrency(value: number): string {
  return `$${value.toLocaleString("es-MX", { minimumFractionDigits: 2, maximumFractionDigits: 2 })}`
}

export function CreditCardItem({
  card,
  onEdit,
  onDelete,
  onRegisterPurchase,
  onRegisterPayment,
  onViewMovements,
}: CreditCardItemProps) {
  const usage = card.creditLimit > 0 ? Math.min(Math.max((card.currentBalance / card.creditLimit) * 100, 0), 100) : 0
  const hasBalance = card.currentBalance > 0

  return (
    <div className="rounded-xl bg-card border border-border p-5 hover:border-primary/30 transition-smooth">
      <div className="flex items-start justify-between mb-4">
        <div className="flex items-center gap-3">
          <div className="flex h-10 w-10 items-center justify-center rounded-lg bg-primary/10">
            <CreditCardIcon className="h-5 w-5 text-primary" />
          </div>
          <div>
            <h3 className="text-sm font-semibold text-foreground">{card.name}</h3>
            <p className="text-xs text-muted-foreground">
              {card.bank ? `${card.bank} · ` : ""}
              {franchiseLabels[card.franchise]}
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
            <DropdownMenuItem onClick={() => onEdit(card)}>
              <Pencil className="h-4 w-4" />
              Editar
            </DropdownMenuItem>
            <DropdownMenuItem onClick={() => onViewMovements(card)}>
              <History className="h-4 w-4" />
              Ver movimientos
            </DropdownMenuItem>
            <DropdownMenuItem variant="destructive" onClick={() => onDelete(card)}>
              <Trash2 className="h-4 w-4" />
              Eliminar
            </DropdownMenuItem>
          </DropdownMenuContent>
        </DropdownMenu>
      </div>

      <div className="mb-4">
        <div className="flex items-center justify-between mb-2">
          <span className="text-xs text-muted-foreground">Cupo utilizado</span>
          <span className="text-xs font-medium text-foreground">{usage.toFixed(1)}%</span>
        </div>
        <div className="h-2 rounded-full bg-secondary overflow-hidden">
          <div
            className={cn(
              "h-full rounded-full transition-all duration-500",
              usage >= 90 ? "bg-destructive" : usage >= 70 ? "bg-warning" : "bg-primary",
            )}
            style={{ width: `${usage}%` }}
          />
        </div>
      </div>

      <div className="grid grid-cols-2 gap-3">
        <div className="flex items-center gap-2">
          <div className="flex h-7 w-7 items-center justify-center rounded-md bg-secondary">
            <CreditCardIcon className="h-3.5 w-3.5 text-muted-foreground" />
          </div>
          <div>
            <p className="text-xs text-muted-foreground">Cupo total</p>
            <p className="text-sm font-semibold text-foreground">{formatCurrency(card.creditLimit)}</p>
          </div>
        </div>

        <div className="flex items-center gap-2">
          <div className="flex h-7 w-7 items-center justify-center rounded-md bg-secondary">
            <Wallet className="h-3.5 w-3.5 text-muted-foreground" />
          </div>
          <div>
            <p className="text-xs text-muted-foreground">Disponible</p>
            <p className="text-sm font-semibold text-success">{formatCurrency(card.availableCredit)}</p>
          </div>
        </div>

        <div className="flex items-center gap-2">
          <div className="flex h-7 w-7 items-center justify-center rounded-md bg-secondary">
            <ListPlus className="h-3.5 w-3.5 text-muted-foreground" />
          </div>
          <div>
            <p className="text-xs text-muted-foreground">Saldo actual</p>
            <p className="text-sm font-semibold text-destructive">{formatCurrency(card.currentBalance)}</p>
          </div>
        </div>

        <div className="flex items-center gap-2">
          <div className="flex h-7 w-7 items-center justify-center rounded-md bg-secondary">
            <Percent className="h-3.5 w-3.5 text-muted-foreground" />
          </div>
          <div>
            <p className="text-xs text-muted-foreground">Tasa mensual</p>
            <p className="text-sm font-semibold text-foreground">{card.monthlyRate}%</p>
          </div>
        </div>
      </div>

      <div className="mt-3 flex items-center gap-2 text-xs text-muted-foreground">
        <Calendar className="h-3.5 w-3.5" />
        Corte dia {card.cutoffDay} · Pago dia {card.paymentDueDay}
      </div>

      <div className="mt-4 grid grid-cols-2 gap-2">
        <Button
          variant="outline"
          className="border-warning/30 bg-warning/5 text-warning hover:bg-warning/10"
          onClick={() => onRegisterPurchase(card)}
        >
          <ListPlus className="h-4 w-4" />
          Registrar compra
        </Button>
        <Button
          variant="outline"
          className="border-primary/30 bg-primary/5 text-primary hover:bg-primary/10"
          onClick={() => onRegisterPayment(card)}
          disabled={!hasBalance}
        >
          <Wallet className="h-4 w-4" />
          {hasBalance ? "Registrar pago" : "Sin saldo pendiente"}
        </Button>
      </div>
    </div>
  )
}
