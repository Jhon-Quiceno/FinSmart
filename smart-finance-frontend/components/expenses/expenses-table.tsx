"use client"

import { Pencil, Repeat, Trash2 } from "lucide-react"
import { Button } from "@/components/ui/button"
import { Skeleton } from "@/components/ui/skeleton"
import { Table, TableBody, TableCell, TableHead, TableHeader, TableRow } from "@/components/ui/table"
import { Tooltip, TooltipContent, TooltipTrigger } from "@/components/ui/tooltip"
import type { Expense, PaymentMethodType } from "@/lib/types/expense"

const paymentMethodLabels: Record<PaymentMethodType, string> = {
  CASH: "Efectivo",
  DEBIT_CARD: "Tarjeta de Debito",
  CREDIT_CARD: "Tarjeta de Credito",
  TRANSFER: "Transferencia",
  OTHER: "Otro",
}

interface ExpensesTableProps {
  expenses: Expense[]
  isLoading: boolean
  onEdit: (expense: Expense) => void
  onDelete: (expense: Expense) => void
}

export function ExpensesTable({ expenses, isLoading, onEdit, onDelete }: ExpensesTableProps) {
  return (
    <div className="w-full">
      <div className="hidden md:block w-full overflow-x-auto rounded-xl border border-border bg-card">
        <Table>
          <TableHeader>
            <TableRow>
              <TableHead>Descripcion</TableHead>
              <TableHead>Categoria</TableHead>
              <TableHead>Metodo</TableHead>
              <TableHead>Monto</TableHead>
              <TableHead>Fecha</TableHead>
              <TableHead className="text-right">Acciones</TableHead>
            </TableRow>
          </TableHeader>
          <TableBody>
            {isLoading &&
              Array.from({ length: 6 }).map((_, index) => (
                <TableRow key={`expense-skeleton-${index}`}>
                  <TableCell colSpan={6}>
                    <Skeleton className="h-8 w-full" />
                  </TableCell>
                </TableRow>
              ))}

            {!isLoading && expenses.length === 0 && (
              <TableRow>
                <TableCell colSpan={6} className="py-6 text-center text-muted-foreground">
                  No hay gastos para los filtros seleccionados.
                </TableCell>
              </TableRow>
            )}

            {!isLoading &&
              expenses.map((expense) => (
                <TableRow key={expense.id}>
                  <TableCell className="font-medium">
                    <div className="flex items-center gap-1.5">
                      <span>{expense.description || "Sin descripcion"}</span>
                      {expense.recurringPaymentId !== null && (
                        <Tooltip>
                          <TooltipTrigger asChild>
                            <Repeat className="h-3.5 w-3.5 shrink-0 text-muted-foreground" />
                          </TooltipTrigger>
                          <TooltipContent>Generado automaticamente por un servicio recurrente</TooltipContent>
                        </Tooltip>
                      )}
                    </div>
                  </TableCell>
                  <TableCell>{expense.categoryName || "Sin categoria"}</TableCell>
                  <TableCell>{paymentMethodLabels[expense.paymentMethod] ?? expense.paymentMethod}</TableCell>
                  <TableCell className="font-semibold text-destructive">
                    -${expense.amount.toLocaleString("es-MX", { minimumFractionDigits: 2 })}
                  </TableCell>
                  <TableCell>{expense.date}</TableCell>
                  <TableCell>
                    <div className="flex justify-end gap-2">
                      <Button variant="outline" size="icon" onClick={() => onEdit(expense)}>
                        <Pencil />
                        <span className="sr-only">Editar gasto</span>
                      </Button>
                      <Button variant="outline" size="icon" onClick={() => onDelete(expense)}>
                        <Trash2 />
                        <span className="sr-only">Eliminar gasto</span>
                      </Button>
                    </div>
                  </TableCell>
                </TableRow>
              ))}
          </TableBody>
        </Table>
      </div>

      <div className="md:hidden space-y-3">
        {isLoading &&
          Array.from({ length: 4 }).map((_, index) => (
            <div key={`expense-skeleton-card-${index}`} className="rounded-lg border border-border bg-card p-4">
              <Skeleton className="h-8 w-full" />
            </div>
          ))}

        {!isLoading && expenses.length === 0 && (
          <div className="rounded-lg border border-border bg-card py-6 text-center text-muted-foreground">
            No hay gastos para los filtros seleccionados.
          </div>
        )}

        {!isLoading &&
          expenses.map((expense) => (
            <div key={expense.id} className="rounded-lg border border-border bg-card p-4">
              <div className="flex items-start justify-between gap-2">
                <div className="flex items-center gap-1.5 font-semibold">
                  <span>{expense.description || "Sin descripcion"}</span>
                  {expense.recurringPaymentId !== null && (
                    <Tooltip>
                      <TooltipTrigger asChild>
                        <Repeat className="h-3.5 w-3.5 shrink-0 text-muted-foreground" />
                      </TooltipTrigger>
                      <TooltipContent>Generado automaticamente por un servicio recurrente</TooltipContent>
                    </Tooltip>
                  )}
                </div>
                <span className="shrink-0 text-lg font-semibold text-destructive">
                  -${expense.amount.toLocaleString("es-MX", { minimumFractionDigits: 2 })}
                </span>
              </div>

              <div className="mt-3 grid grid-cols-2 gap-y-2 text-sm">
                <div>
                  <span className="block text-xs uppercase tracking-wide text-muted-foreground">Categoria</span>
                  <span className="text-foreground">{expense.categoryName || "Sin categoria"}</span>
                </div>
                <div>
                  <span className="block text-xs uppercase tracking-wide text-muted-foreground">Metodo</span>
                  <span className="text-foreground">
                    {paymentMethodLabels[expense.paymentMethod] ?? expense.paymentMethod}
                  </span>
                </div>
                <div>
                  <span className="block text-xs uppercase tracking-wide text-muted-foreground">Fecha</span>
                  <span className="text-foreground">{expense.date}</span>
                </div>
              </div>

              <div className="mt-3 flex justify-end gap-2">
                <Button variant="outline" size="icon" onClick={() => onEdit(expense)}>
                  <Pencil />
                  <span className="sr-only">Editar gasto</span>
                </Button>
                <Button variant="outline" size="icon" onClick={() => onDelete(expense)}>
                  <Trash2 />
                  <span className="sr-only">Eliminar gasto</span>
                </Button>
              </div>
            </div>
          ))}
      </div>
    </div>
  )
}
