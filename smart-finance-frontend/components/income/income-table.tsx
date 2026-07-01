"use client"

import { Pencil, Trash2 } from "lucide-react"
import { Button } from "@/components/ui/button"
import { Skeleton } from "@/components/ui/skeleton"
import { Table, TableBody, TableCell, TableHead, TableHeader, TableRow } from "@/components/ui/table"
import type { Income } from "@/lib/types/income"

interface IncomeTableProps {
  incomes: Income[]
  isLoading: boolean
  onEdit: (income: Income) => void
  onDelete: (income: Income) => void
}

export function IncomeTable({ incomes, isLoading, onEdit, onDelete }: IncomeTableProps) {
  return (
    <div className="rounded-xl border border-border bg-card">
      <Table>
        <TableHeader>
          <TableRow>
            <TableHead>Descripcion</TableHead>
            <TableHead>Categoria</TableHead>
            <TableHead>Fuente</TableHead>
            <TableHead>Monto</TableHead>
            <TableHead>Fecha</TableHead>
            <TableHead className="text-right">Acciones</TableHead>
          </TableRow>
        </TableHeader>
        <TableBody>
          {isLoading &&
            Array.from({ length: 6 }).map((_, index) => (
              <TableRow key={`income-skeleton-${index}`}>
                <TableCell colSpan={6}>
                  <Skeleton className="h-8 w-full" />
                </TableCell>
              </TableRow>
            ))}

          {!isLoading && incomes.length === 0 && (
            <TableRow>
              <TableCell colSpan={6} className="py-6 text-center text-muted-foreground">
                No hay ingresos para los filtros seleccionados.
              </TableCell>
            </TableRow>
          )}

          {!isLoading &&
            incomes.map((income) => (
              <TableRow key={income.id}>
                <TableCell className="font-medium">{income.description || "Sin descripcion"}</TableCell>
                <TableCell>{income.categoryName || "Sin categoria"}</TableCell>
                <TableCell>{income.source || "Sin fuente"}</TableCell>
                <TableCell className="font-semibold text-success">
                  +${income.amount.toLocaleString("es-MX", { minimumFractionDigits: 2 })}
                </TableCell>
                <TableCell>{income.date}</TableCell>
                <TableCell>
                  <div className="flex justify-end gap-2">
                    <Button variant="outline" size="icon" onClick={() => onEdit(income)}>
                      <Pencil />
                      <span className="sr-only">Editar ingreso</span>
                    </Button>
                    <Button variant="outline" size="icon" onClick={() => onDelete(income)}>
                      <Trash2 />
                      <span className="sr-only">Eliminar ingreso</span>
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
