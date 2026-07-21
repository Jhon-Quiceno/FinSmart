"use client"

import { useMemo, useState } from "react"
import { Plus, TrendingDown } from "lucide-react"
import { toast } from "sonner"
import { ExpenseModal } from "@/components/expenses/expense-modal"
import { ExpensesFilters } from "@/components/expenses/expenses-filters"
import { ExpensesTable } from "@/components/expenses/expenses-table"
import { AppLayout } from "@/components/layout/app-layout"
import { Button } from "@/components/ui/button"
import { Empty, EmptyDescription, EmptyHeader, EmptyMedia, EmptyTitle } from "@/components/ui/empty"
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
import { toastApiError } from "@/lib/api-client"
import type { ExpenseFormValues } from "@/lib/schemas/expense.schema"
import type { Expense, ExpenseRequest } from "@/lib/types/expense"
import { ALL_PERIODS_VALUE, getCurrentPeriodValue, periodToDateRange } from "@/lib/utils/period"

const pageSize = 10
// Large enough to cover a personal finance user's history in one page; only used to
// compute the true filtered total/count below, never rendered as a paginated table.
const summaryPageSize = 1000

export default function GastosPage() {
  const [page, setPage] = useState(1)
  const [searchQuery, setSearchQuery] = useState("")
  const [selectedCategory, setSelectedCategory] = useState("none")
  const [period, setPeriod] = useState(getCurrentPeriodValue())
  const [modalOpen, setModalOpen] = useState(false)
  const [editingExpense, setEditingExpense] = useState<Expense | null>(null)
  const [deletingExpense, setDeletingExpense] = useState<Expense | null>(null)

  const { from, to } = useMemo(() => periodToDateRange(period), [period])
  const categoryId = selectedCategory !== "none" ? Number(selectedCategory) : undefined

  const { expenses, isLoading } = useExpenses({
    page: page - 1,
    size: pageSize,
    categoryId,
    from,
    to,
  })

  // Separate fetch (same filters, no pagination) so the "Total gastos filtrados" card
  // reflects every matching expense, not just the 10 shown on the current table page.
  const { expenses: allFilteredExpenses, isLoading: isSummaryLoading } = useExpenses({
    page: 0,
    size: summaryPageSize,
    categoryId,
    from,
    to,
  })

  const { createExpense, isLoading: isCreating } = useCreateExpense()
  const { updateExpense, isLoading: isUpdating } = useUpdateExpense()
  const { deleteExpense, isLoading: isDeleting } = useDeleteExpense()

  const filteredExpenses = useMemo(() => {
    const normalizedQuery = searchQuery.toLowerCase().trim()

    if (!normalizedQuery) return expenses.content

    return expenses.content.filter(
      (expense) =>
        `${expense.description || ""} ${expense.paymentMethod || ""}`.toLowerCase().includes(normalizedQuery),
    )
  }, [expenses.content, searchQuery])

  const totalExpenses = useMemo(
    () => allFilteredExpenses.content.reduce((sum, expense) => sum + expense.amount, 0),
    [allFilteredExpenses.content],
  )

  const hasActiveFilters = searchQuery.trim() !== "" || selectedCategory !== "none" || period !== ALL_PERIODS_VALUE

  const handleClearFilters = () => {
    setSearchQuery("")
    setSelectedCategory("none")
    setPeriod(ALL_PERIODS_VALUE)
    setPage(1)
  }

  const handleSubmit = async (values: ExpenseFormValues) => {
    const payload: ExpenseRequest = {
      amount: values.amount,
      description: values.description,
      date: values.date,
      paymentMethod: values.paymentMethod,
      categoryId: values.categoryId ?? undefined,
    }

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
      toastApiError(error, "No fue posible guardar el gasto")
    }
  }

  const handleDelete = async () => {
    if (!deletingExpense) return

    try {
      await deleteExpense(deletingExpense.id)
      toast.success("Gasto eliminado correctamente")
      setDeletingExpense(null)
    } catch (error) {
      toastApiError(error, "No fue posible eliminar el gasto")
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
          <div className="flex gap-2">
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
        </div>

        <div className="rounded-xl border border-border bg-card p-5">
          {isSummaryLoading ? (
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
              <p className="text-sm text-muted-foreground">
                {allFilteredExpenses.totalElements} transacciones
              </p>
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
          period={period}
          onPeriodChange={(value) => {
            setPeriod(value)
            setPage(1)
          }}
          onClearFilters={handleClearFilters}
          hasActiveFilters={hasActiveFilters}
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

        {!isLoading && filteredExpenses.length === 0 ? (
          <Empty>
            <EmptyHeader>
              <EmptyMedia variant="icon">
                <TrendingDown />
              </EmptyMedia>
              <EmptyTitle>No hay gastos registrados</EmptyTitle>
              <EmptyDescription>
                Ajusta los filtros seleccionados o agrega tu primer gasto para verlo reflejado aqui.
              </EmptyDescription>
            </EmptyHeader>
          </Empty>
        ) : (
          <ExpensesTable
            expenses={filteredExpenses}
            isLoading={isLoading}
            onEdit={(expense) => {
              setEditingExpense(expense)
              setModalOpen(true)
            }}
            onDelete={(expense) => setDeletingExpense(expense)}
          />
        )}
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
