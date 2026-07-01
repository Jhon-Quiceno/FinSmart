"use client"

import { useState } from "react"
import { Button } from "@/components/ui/button"
import { Dialog, DialogContent, DialogDescription, DialogHeader, DialogTitle } from "@/components/ui/dialog"
import { Skeleton } from "@/components/ui/skeleton"
import { useDebtPayments } from "@/hooks/use-debt-payments"
import type { Debt } from "@/lib/types/debt"

interface DebtPaymentHistoryDialogProps {
  open: boolean
  onOpenChange: (open: boolean) => void
  debt: Debt | null
}

const pageSize = 5

function formatCurrency(value: number): string {
  return `$${value.toLocaleString("es-MX", { minimumFractionDigits: 2, maximumFractionDigits: 2 })}`
}

function formatDate(value: string): string {
  const date = new Date(`${value}T00:00:00`)
  if (Number.isNaN(date.getTime())) return value
  return date.toLocaleDateString("es-MX", { day: "2-digit", month: "short", year: "numeric" })
}

export function DebtPaymentHistoryDialog({ open, onOpenChange, debt }: DebtPaymentHistoryDialogProps) {
  const [page, setPage] = useState(1)

  const { payments, isLoading } = useDebtPayments(open ? (debt?.id ?? null) : null, {
    page: page - 1,
    size: pageSize,
  })

  return (
    <Dialog
      open={open}
      onOpenChange={(nextOpen) => {
        onOpenChange(nextOpen)
        if (!nextOpen) setPage(1)
      }}
    >
      <DialogContent>
        <DialogHeader>
          <DialogTitle>Historial de pagos</DialogTitle>
          <DialogDescription>
            {debt ? `Abonos registrados para "${debt.name}".` : "Abonos registrados para esta deuda."}
          </DialogDescription>
        </DialogHeader>

        {isLoading ? (
          <div className="flex flex-col gap-2">
            <Skeleton className="h-12 w-full" />
            <Skeleton className="h-12 w-full" />
            <Skeleton className="h-12 w-full" />
          </div>
        ) : payments.content.length === 0 ? (
          <p className="text-sm text-muted-foreground">Todavia no hay pagos registrados.</p>
        ) : (
          <div className="flex flex-col gap-2">
            {payments.content.map((payment) => (
              <div key={payment.id} className="rounded-lg border border-border p-3">
                <div className="flex items-center justify-between">
                  <p className="text-sm font-medium text-foreground">{formatCurrency(payment.amount)}</p>
                  <p className="text-xs text-muted-foreground">{formatDate(payment.paymentDate)}</p>
                </div>
                {payment.note && <p className="mt-1 text-xs text-muted-foreground">{payment.note}</p>}
              </div>
            ))}
          </div>
        )}

        {payments.totalPages > 1 && (
          <div className="flex items-center justify-end gap-2">
            <Button variant="outline" size="sm" disabled={page <= 1} onClick={() => setPage((value) => value - 1)}>
              Anterior
            </Button>
            <span className="text-xs text-muted-foreground">
              Pagina {Math.max(payments.number + 1, page)} de {Math.max(payments.totalPages, 1)}
            </span>
            <Button
              variant="outline"
              size="sm"
              disabled={page >= Math.max(payments.totalPages, 1)}
              onClick={() => setPage((value) => value + 1)}
            >
              Siguiente
            </Button>
          </div>
        )}
      </DialogContent>
    </Dialog>
  )
}
