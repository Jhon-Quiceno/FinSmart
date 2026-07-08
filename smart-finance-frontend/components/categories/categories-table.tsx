"use client"

import { Pencil, Trash2 } from "lucide-react"
import { Button } from "@/components/ui/button"
import { Skeleton } from "@/components/ui/skeleton"
import { Table, TableBody, TableCell, TableHead, TableHeader, TableRow } from "@/components/ui/table"
import type { Category } from "@/lib/types/category"

interface CategoriesTableProps {
  categories: Category[]
  isLoading: boolean
  onEdit: (category: Category) => void
  onDelete: (category: Category) => void
}

export function CategoriesTable({ categories, isLoading, onEdit, onDelete }: CategoriesTableProps) {
  return (
    <div className="w-full">
      <div className="hidden md:block w-full overflow-x-auto rounded-xl border border-border bg-card">
        <Table>
          <TableHeader>
            <TableRow>
              <TableHead>Nombre</TableHead>
              <TableHead className="text-right">Acciones</TableHead>
            </TableRow>
          </TableHeader>
          <TableBody>
            {isLoading &&
              Array.from({ length: 4 }).map((_, index) => (
                <TableRow key={`category-skeleton-${index}`}>
                  <TableCell colSpan={2}>
                    <Skeleton className="h-8 w-full" />
                  </TableCell>
                </TableRow>
              ))}

            {!isLoading && categories.length === 0 && (
              <TableRow>
                <TableCell colSpan={2} className="py-6 text-center text-muted-foreground">
                  No hay categorias registradas.
                </TableCell>
              </TableRow>
            )}

            {!isLoading &&
              categories.map((category) => (
                <TableRow key={category.id}>
                  <TableCell className="font-medium">{category.name}</TableCell>
                  <TableCell>
                    <div className="flex justify-end gap-2">
                      <Button variant="outline" size="icon" onClick={() => onEdit(category)}>
                        <Pencil />
                        <span className="sr-only">Editar categoria</span>
                      </Button>
                      <Button variant="outline" size="icon" onClick={() => onDelete(category)}>
                        <Trash2 />
                        <span className="sr-only">Eliminar categoria</span>
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
            <div key={`category-skeleton-card-${index}`} className="rounded-lg border border-border bg-card p-4">
              <Skeleton className="h-8 w-full" />
            </div>
          ))}

        {!isLoading && categories.length === 0 && (
          <div className="rounded-lg border border-border bg-card py-6 text-center text-muted-foreground">
            No hay categorias registradas.
          </div>
        )}

        {!isLoading &&
          categories.map((category) => (
            <div
              key={category.id}
              className="flex items-center justify-between gap-2 rounded-lg border border-border bg-card p-4"
            >
              <span className="font-semibold">{category.name}</span>
              <div className="flex justify-end gap-2">
                <Button variant="outline" size="icon" onClick={() => onEdit(category)}>
                  <Pencil />
                  <span className="sr-only">Editar categoria</span>
                </Button>
                <Button variant="outline" size="icon" onClick={() => onDelete(category)}>
                  <Trash2 />
                  <span className="sr-only">Eliminar categoria</span>
                </Button>
              </div>
            </div>
          ))}
      </div>
    </div>
  )
}
