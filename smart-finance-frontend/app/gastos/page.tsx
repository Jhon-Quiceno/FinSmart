"use client"

import { useMemo, useState } from "react"
import { Plus, TrendingDown } from "lucide-react"
import { toast } from "sonner"
import { ExpenseModal } from "@/components/expenses/expense-modal"
import { ExpensesFilters } from "@/components/expenses/expenses-filters"
import { ExpensesTable } from "@/components/expenses/expenses-table"
import { AppLayout } from "@/components/layout/app-layout"
import { Button } from "@/components/ui/button"
import { Skeleton } from "@/components/ui/skeleton"
import {
  AlertDialog,
  AlertDialogAction,
  AlertDialogCancel,
  AlertDialogContent,
  AlertDialogDescription,
  AlertDialogFooter,
  AlertDialogHeader,
  AlertDialogTitle,
} from "@/components/ui/alert-dialog"
import { useCreateExpense, useDeleteExpense, useExpenses, useUpdateExpense } from "@/hooks/use-expenses"
import { getApiErrorMessage } from "@/lib/api-client"
import { expenseSchema } from "@/lib/schemas/expense.schema"
import type { Expense } from "@/lib/types/expense"
import type { z } from "zod"

const pageSize = 10
type ExpenseFormValues = z.infer<typeof expenseSchema>

export default function GastosPage() {
  const [page, setPage] = useState(1)
  const [searchQuery, setSearchQuery] = useState("")
  const [selectedCategory, setSelectedCategory] = useState("none")
  const [from, setFrom] = useState("")
  const [to, setTo] = useState("")
  const [modalOpen, setModalOpen] = useState(false)
  const [editingExpense, setEditingExpense] = useState<Expense | null>(null)
  const [deletingExpense, setDeletingExpense] = useState<Expense | null>(null)

  const { expenses, isLoading } = useExpenses({
    page: page - 1,
    size: pageSize,
    categoryId: selectedCategory !== "none" ? Number(selectedCategory) : undefined,
    from: from || undefined,
    to: to || undefined,
  })

  const { createExpense, isLoading: isCreating } = useCreateExpense()
  const { updateExpense, isLoading: isUpdating } = useUpdateExpense()
  const { deleteExpense, isLoading: isDeleting } = useDeleteExpense()

  const filteredExpenses = useMemo(() => {
    const normalizedQuery = searchQuery.toLowerCase().trim()

    if (!normalizedQuery) return expenses.content

    return expenses.content.filter((expense) =>
      `${expense.description || ""} ${expense.paymentMethod || ""}`
        .toLowerCase()
        .includes(normalizedQuery),
    )
  }, [expenses.content, searchQuery])

  const totalExpenses = useMemo(
    () => filteredExpenses.reduce((sum, expense) => sum + expense.amount, 0),
    [filteredExpenses],
  )

  const handleSubmit = async (values: ExpenseFormValues) => {
    const payload = expenseSchema.parse(values)

    try {
      if (editingExpense) {
        await updateExpense(editingExpense.id, payload)
        toast.success("Gasto actualizado correctamente")
      } else {
        await createExpense(payload)
        toast.success("Gasto creado correctamente")
      }

      setModalOpen(false)
      setEditingExpense(null)
    } catch (error) {
      toast.error(getApiErrorMessage(error, "No fue posible guardar el gasto"))
    }
  }

  const handleDelete = async () => {
    if (!deletingExpense) return

    try {
      await deleteExpense(deletingExpense.id)
      toast.success("Gasto eliminado correctamente")
      setDeletingExpense(null)
    } catch (error) {
      toast.error(getApiErrorMessage(error, "No fue posible eliminar el gasto"))
    }
  }

  return (
    <AppLayout>
      <div className="flex flex-col gap-6">
        <div className="flex flex-col gap-4 sm:flex-row sm:items-center sm:justify-between">
          <div>
            <h1 className="text-2xl font-bold text-foreground">Gastos</h1>
            <p className="text-sm text-muted-foreground">Administra y registra todos tus gastos</p>
          </div>
          <Button
            onClick={() => {
              setEditingExpense(null)
              setModalOpen(true)
            }}
          >
            <Plus data-icon="inline-start" />
            Agregar gasto
          </Button>
        </div>

        <div className="rounded-xl border border-border bg-card p-5">
          {isLoading ? (
            <div className="flex flex-col gap-2">
              <Skeleton className="h-4 w-40" />
              <Skeleton className="h-8 w-48" />
            </div>
          ) : (
            <div className="flex items-center justify-between">
              <div className="flex items-center gap-3">
                <div className="flex size-10 items-center justify-center rounded-lg bg-destructive/10">
                  <TrendingDown className="text-destructive" />
                </div>
                <div>
                  <p className="text-sm text-muted-foreground">Total gastos filtrados</p>
                  <p className="text-xl font-semibold text-destructive">
                    -${totalExpenses.toLocaleString("es-MX", { minimumFractionDigits: 2 })}
                  </p>
                </div>
              </div>
              <p className="text-sm text-muted-foreground">{filteredExpenses.length} transacciones</p>
            </div>
          )}
        </div>

        <ExpensesFilters
          searchQuery={searchQuery}
          onSearchChange={setSearchQuery}
          selectedCategory={selectedCategory}
          onCategoryChange={(value) => {
            setSelectedCategory(value)
            setPage(1)
          }}
          from={from}
          onFromChange={(value) => {
            setFrom(value)
            setPage(1)
          }}
          to={to}
          onToChange={(value) => {
            setTo(value)
            setPage(1)
          }}
        />

        <div className="flex items-center justify-end gap-2">
          <Button variant="outline" disabled={page <= 1} onClick={() => setPage((value) => value - 1)}>
            Anterior
          </Button>
          <span className="text-sm text-muted-foreground">
            Pagina {Math.max(expenses.number + 1, page)} de {Math.max(expenses.totalPages, 1)}
          </span>
          <Button
            variant="outline"
            disabled={page >= Math.max(expenses.totalPages, 1)}
            onClick={() => setPage((value) => value + 1)}
          >
            Siguiente
          </Button>
        </div>

        <ExpensesTable
          expenses={filteredExpenses}
          isLoading={isLoading}
          onEdit={(expense) => {
            setEditingExpense(expense)
            setModalOpen(true)
          }}
          onDelete={(expense) => setDeletingExpense(expense)}
        />
      </div>

      <ExpenseModal
        open={modalOpen}
        onOpenChange={(nextOpen) => {
          setModalOpen(nextOpen)
          if (!nextOpen) setEditingExpense(null)
        }}
        initialValue={editingExpense}
        onSubmit={handleSubmit}
        isSubmitting={isCreating || isUpdating}
      />

      <AlertDialog open={!!deletingExpense} onOpenChange={(open) => !open && setDeletingExpense(null)}>
        <AlertDialogContent>
          <AlertDialogHeader>
            <AlertDialogTitle>Eliminar gasto</AlertDialogTitle>
            <AlertDialogDescription>
              Esta accion no se puede deshacer. El gasto seleccionado se eliminara de forma permanente.
            </AlertDialogDescription>
          </AlertDialogHeader>
          <AlertDialogFooter>
            <AlertDialogCancel>Cancelar</AlertDialogCancel>
            <AlertDialogAction onClick={handleDelete} disabled={isDeleting}>
              {isDeleting ? "Eliminando..." : "Eliminar"}
            </AlertDialogAction>
          </AlertDialogFooter>
        </AlertDialogContent>
      </AlertDialog>
    </AppLayout>
  )
}
