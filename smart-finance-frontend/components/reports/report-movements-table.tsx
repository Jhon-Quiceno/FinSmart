"use client"

import { Skeleton } from "@/components/ui/skeleton"
import { Table, TableBody, TableCell, TableHead, TableHeader, TableRow } from "@/components/ui/table"
import type { PaymentMethodType } from "@/lib/types/expense"
import type { ReportMovementRow } from "@/lib/types/report"

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

interface ReportMovementsTableProps {
  movements: ReportMovementRow[]
  isLoadingMovements: boolean
}

export function ReportMovementsTable({ movements, isLoadingMovements }: ReportMovementsTableProps) {
  return (
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
  )
}
