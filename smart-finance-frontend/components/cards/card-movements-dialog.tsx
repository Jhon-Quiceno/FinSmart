"use client"

import { Fragment, useEffect, useState } from "react"
import { ChevronDown, ChevronRight } from "lucide-react"
import { toast } from "sonner"
import { Button } from "@/components/ui/button"
import { Dialog, DialogContent, DialogDescription, DialogHeader, DialogTitle } from "@/components/ui/dialog"
import { Skeleton } from "@/components/ui/skeleton"
import { Table, TableBody, TableCell, TableHead, TableHeader, TableRow } from "@/components/ui/table"
import { useCardMovements } from "@/hooks/use-card-movements"
import { getInstallments } from "@/lib/services/card-movement.service"
import type { CardMovement, CardMovementType, Installment, InstallmentStatus } from "@/lib/types/card-movement"
import type { CreditCard } from "@/lib/types/credit-card"

interface CardMovementsDialogProps {
  open: boolean
  onOpenChange: (open: boolean) => void
  card: CreditCard | null
}

const pageSize = 5

const movementTypeLabels: Record<CardMovementType, string> = {
  PURCHASE: "Compra",
  INSTALLMENT_PURCHASE: "Compra en cuotas",
  PAYMENT: "Pago",
  INTEREST: "Interes",
  FEE: "Cargo",
}

const installmentStatusLabels: Record<InstallmentStatus, string> = {
  PENDING: "Pendiente",
  BILLED: "Facturada",
}

function formatCurrency(value: number): string {
  return `$${value.toLocaleString("es-MX", { minimumFractionDigits: 2, maximumFractionDigits: 2 })}`
}

function formatDate(value: string): string {
  const date = new Date(`${value}T00:00:00`)
  if (Number.isNaN(date.getTime())) return value
  return date.toLocaleDateString("es-MX", { day: "2-digit", month: "short", year: "numeric" })
}

// Purchases/interest/fees increase the card balance (shown as +); payments reduce it (shown as -).
function increasesBalance(type: CardMovementType): boolean {
  return type === "PURCHASE" || type === "INSTALLMENT_PURCHASE" || type === "INTEREST" || type === "FEE"
}

export function CardMovementsDialog({ open, onOpenChange, card }: CardMovementsDialogProps) {
  const [page, setPage] = useState(1)
  const [expandedMovementId, setExpandedMovementId] = useState<number | null>(null)
  const [installments, setInstallments] = useState<Installment[]>([])
  const [isLoadingInstallments, setIsLoadingInstallments] = useState(false)

  const { movements, isLoading, error } = useCardMovements(open ? (card?.id ?? null) : null, {
    page: page - 1,
    size: pageSize,
  })

  useEffect(() => {
    if (error) {
      toast.error(error)
    }
  }, [error])

  const changePage = (nextPage: number) => {
    setPage(nextPage)
    setExpandedMovementId(null)
    setInstallments([])
  }

  const toggleInstallments = async (movement: CardMovement) => {
    if (!card) return

    if (expandedMovementId === movement.id) {
      setExpandedMovementId(null)
      setInstallments([])
      return
    }

    setExpandedMovementId(movement.id)
    setIsLoadingInstallments(true)
    try {
      const result = await getInstallments(card.id, movement.id)
      setInstallments(result)
    } catch {
      toast.error("No fue posible cargar las cuotas de la compra")
      setExpandedMovementId(null)
    } finally {
      setIsLoadingInstallments(false)
    }
  }

  return (
    <Dialog
      open={open}
      onOpenChange={(nextOpen) => {
        onOpenChange(nextOpen)
        if (!nextOpen) {
          setPage(1)
          setExpandedMovementId(null)
          setInstallments([])
        }
      }}
    >
      <DialogContent className="sm:max-w-2xl">
        <DialogHeader>
          <DialogTitle>Movimientos de la tarjeta</DialogTitle>
          <DialogDescription>
            {card ? `Historial de compras y pagos de "${card.name}".` : "Historial de movimientos de esta tarjeta."}
          </DialogDescription>
        </DialogHeader>

        {isLoading ? (
          <div className="flex flex-col gap-2">
            <Skeleton className="h-12 w-full" />
            <Skeleton className="h-12 w-full" />
            <Skeleton className="h-12 w-full" />
          </div>
        ) : movements.content.length === 0 ? (
          <p className="text-sm text-muted-foreground">Todavia no hay movimientos registrados.</p>
        ) : (
          <div className="rounded-xl border border-border">
            <Table>
              <TableHeader>
                <TableRow>
                  <TableHead>Fecha</TableHead>
                  <TableHead>Tipo</TableHead>
                  <TableHead>Descripcion</TableHead>
                  <TableHead>Monto</TableHead>
                  <TableHead />
                </TableRow>
              </TableHeader>
              <TableBody>
                {movements.content.map((movement) => (
                  <Fragment key={movement.id}>
                    <TableRow>
                      <TableCell>{formatDate(movement.date)}</TableCell>
                      <TableCell>{movementTypeLabels[movement.type]}</TableCell>
                      <TableCell>{movement.description || "Sin descripcion"}</TableCell>
                      <TableCell
                        className={
                          increasesBalance(movement.type) ? "font-semibold text-destructive" : "font-semibold text-success"
                        }
                      >
                        {increasesBalance(movement.type) ? "+" : "-"}
                        {formatCurrency(movement.amount)}
                      </TableCell>
                      <TableCell>
                        {movement.type === "INSTALLMENT_PURCHASE" && (
                          <Button variant="ghost" size="sm" onClick={() => void toggleInstallments(movement)}>
                            {expandedMovementId === movement.id ? (
                              <ChevronDown className="h-4 w-4" />
                            ) : (
                              <ChevronRight className="h-4 w-4" />
                            )}
                            Cuotas
                          </Button>
                        )}
                      </TableCell>
                    </TableRow>
                    {expandedMovementId === movement.id && (
                      <TableRow>
                        <TableCell colSpan={5} className="bg-secondary/30 whitespace-normal">
                          {isLoadingInstallments ? (
                            <Skeleton className="h-10 w-full" />
                          ) : installments.length === 0 ? (
                            <p className="text-xs text-muted-foreground py-2">Esta compra no tiene cuotas registradas.</p>
                          ) : (
                            <div className="flex flex-col gap-1 py-2">
                              {installments.map((installment) => (
                                <div key={installment.id} className="flex items-center justify-between text-xs">
                                  <span className="text-muted-foreground">
                                    Cuota {installment.number} · vence {formatDate(installment.dueDate)}
                                  </span>
                                  <span className="font-medium text-foreground">
                                    {formatCurrency(installment.capitalAmount + installment.interestAmount)}{" "}
                                    <span className="text-muted-foreground">
                                      ({installmentStatusLabels[installment.status]})
                                    </span>
                                  </span>
                                </div>
                              ))}
                            </div>
                          )}
                        </TableCell>
                      </TableRow>
                    )}
                  </Fragment>
                ))}
              </TableBody>
            </Table>
          </div>
        )}

        {movements.totalPages > 1 && (
          <div className="flex items-center justify-end gap-2">
            <Button variant="outline" size="sm" disabled={page <= 1} onClick={() => changePage(page - 1)}>
              Anterior
            </Button>
            <span className="text-xs text-muted-foreground">
              Pagina {Math.max(movements.number + 1, page)} de {Math.max(movements.totalPages, 1)}
            </span>
            <Button
              variant="outline"
              size="sm"
              disabled={page >= Math.max(movements.totalPages, 1)}
              onClick={() => changePage(page + 1)}
            >
              Siguiente
            </Button>
          </div>
        )}
      </DialogContent>
    </Dialog>
  )
}
