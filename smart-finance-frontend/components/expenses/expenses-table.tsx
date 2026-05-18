"use client"

import { Pencil, Trash2 } from "lucide-react"
import { Badge } from "@/components/ui/badge"
import { Button } from "@/components/ui/button"
import { Skeleton } from "@/components/ui/skeleton"
import { Table, TableBody, TableCell, TableHead, TableHeader, TableRow } from "@/components/ui/table"
import type { Expense } from "@/lib/types/expense"

interface ExpensesTableProps {
  expenses: Expense[]
  isLoading: boolean
  onEdit: (expense: Expense) => void
  onDelete: (expense: Expense) => void
}

export function ExpensesTable({ expenses, isLoading, onEdit, onDelete }: ExpensesTableProps) {
  return (
    <div className="rounded-xl border border-border bg-card">
      <Table>
        <TableHeader>
          <TableRow>
            <TableHead>Descripcion</TableHead>
            <TableHead>Categoria</TableHead>
            <TableHead>Metodo</TableHead>
            <TableHead>Tipo</TableHead>
            <TableHead>Monto</TableHead>
            <TableHead>Fecha</TableHead>
            <TableHead className="text-right">Acciones</TableHead>
          </TableRow>
        </TableHeader>
        <TableBody>
          {isLoading &&
            Array.from({ length: 6 }).map((_, index) => (
              <TableRow key={`expense-skeleton-${index}`}>
                <TableCell colSpan={7}>
                  <Skeleton className="h-8 w-full" />
                </TableCell>
              </TableRow>
            ))}

          {!isLoading && expenses.length === 0 && (
            <TableRow>
              <TableCell colSpan={7} className="py-6 text-center text-muted-foreground">
                No hay gastos para los filtros seleccionados.
              </TableCell>
            </TableRow>
          )}

          {!isLoading &&
            expenses.map((expense) => (
              <TableRow key={expense.id}>
                <TableCell className="font-medium">{expense.description || "Sin descripcion"}</TableCell>
                <TableCell>{expense.categoryName || "Sin categoria"}</TableCell>
                <TableCell>{expense.paymentMethod || "Sin metodo"}</TableCell>
                <TableCell>
                  <Badge variant={expense.isRecurring ? "secondary" : "outline"}>
                    {expense.isRecurring ? "Recurrente" : "Unico"}
                  </Badge>
                </TableCell>
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
  )
}
