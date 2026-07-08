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
    <div className="w-full">
      <div className="hidden md:block w-full overflow-x-auto rounded-xl border border-border bg-card">
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

      <div className="md:hidden space-y-3">
        {isLoading &&
          Array.from({ length: 4 }).map((_, index) => (
            <div key={`income-skeleton-card-${index}`} className="rounded-lg border border-border bg-card p-4">
              <Skeleton className="h-8 w-full" />
            </div>
          ))}

        {!isLoading && incomes.length === 0 && (
          <div className="rounded-lg border border-border bg-card py-6 text-center text-muted-foreground">
            No hay ingresos para los filtros seleccionados.
          </div>
        )}

        {!isLoading &&
          incomes.map((income) => (
            <div key={income.id} className="rounded-lg border border-border bg-card p-4">
              <div className="flex items-start justify-between gap-2">
                <span className="font-semibold">{income.description || "Sin descripcion"}</span>
                <span className="shrink-0 text-lg font-semibold text-success">
                  +${income.amount.toLocaleString("es-MX", { minimumFractionDigits: 2 })}
                </span>
              </div>

              <div className="mt-3 grid grid-cols-2 gap-y-2 text-sm">
                <div>
                  <span className="block text-xs uppercase tracking-wide text-muted-foreground">Categoria</span>
                  <span className="text-foreground">{income.categoryName || "Sin categoria"}</span>
                </div>
                <div>
                  <span className="block text-xs uppercase tracking-wide text-muted-foreground">Fuente</span>
                  <span className="text-foreground">{income.source || "Sin fuente"}</span>
                </div>
                <div>
                  <span className="block text-xs uppercase tracking-wide text-muted-foreground">Fecha</span>
                  <span className="text-foreground">{income.date}</span>
                </div>
              </div>

              <div className="mt-3 flex justify-end gap-2">
                <Button variant="outline" size="icon" onClick={() => onEdit(income)}>
                  <Pencil />
                  <span className="sr-only">Editar ingreso</span>
                </Button>
                <Button variant="outline" size="icon" onClick={() => onDelete(income)}>
                  <Trash2 />
                  <span className="sr-only">Eliminar ingreso</span>
                </Button>
              </div>
            </div>
          ))}
      </div>
    </div>
  )
}
