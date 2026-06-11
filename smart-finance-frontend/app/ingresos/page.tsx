"use client"

import { useMemo, useState } from "react"
import { Plus, TrendingUp } from "lucide-react"
import { toast } from "sonner"
import { IncomeModal } from "@/components/income/income-modal"
import { IncomeTable } from "@/components/income/income-table"
import { AppLayout } from "@/components/layout/app-layout"
import { Button } from "@/components/ui/button"
import { Select, SelectContent, SelectGroup, SelectItem, SelectTrigger, SelectValue } from "@/components/ui/select"
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
import {
  useCreateTransaction,
  useDeleteTransaction,
  useTransactions,
  useUpdateTransaction,
} from "@/hooks/use-transactions"
import { getApiErrorMessage } from "@/lib/api-client"
import { incomeSchema } from "@/lib/schemas/income.schema"
import type { Transaction } from "@/lib/types/transaction"
import type { z } from "zod"

const pageSize = 10

const monthOptions = [
  { value: "1", label: "Enero" },
  { value: "2", label: "Febrero" },
  { value: "3", label: "Marzo" },
  { value: "4", label: "Abril" },
  { value: "5", label: "Mayo" },
  { value: "6", label: "Junio" },
  { value: "7", label: "Julio" },
  { value: "8", label: "Agosto" },
  { value: "9", label: "Septiembre" },
  { value: "10", label: "Octubre" },
  { value: "11", label: "Noviembre" },
  { value: "12", label: "Diciembre" },
]

type IncomeFormValues = z.infer<typeof incomeSchema>

function transactionToIncome(t: Transaction) {
  return {
    id: t.id,
    amount: t.amount,
    description: t.description,
    date: t.transactionDate,
    isRecurring: false,
    source: t.notes,
    categoryId: t.categoryId,
    categoryName: t.categoryName,
  }
}

export default function IngresosPage() {
  const now = new Date()
  const [page, setPage] = useState(1)
  const [selectedMonth, setSelectedMonth] = useState(String(now.getMonth() + 1))
  const [selectedYear, setSelectedYear] = useState(String(now.getFullYear()))
  const [modalOpen, setModalOpen] = useState(false)
  const [editingTransaction, setEditingTransaction] = useState<Transaction | null>(null)
  const [deletingTransaction, setDeletingTransaction] = useState<Transaction | null>(null)

  const { transactions, isLoading } = useTransactions({
    page: page - 1,
    size: pageSize,
    type: "INCOME",
    month: Number(selectedMonth),
    year: Number(selectedYear),
  })

  const incomes = useMemo(
    () => ({
      ...transactions,
      content: transactions.content.map(transactionToIncome),
    }),
    [transactions],
  )

  const { createTransaction, isLoading: isCreating } = useCreateTransaction()
  const { updateTransaction, isLoading: isUpdating } = useUpdateTransaction()
  const { deleteTransaction, isLoading: isDeleting } = useDeleteTransaction()

  const totalIncome = useMemo(
    () => incomes.content.reduce((sum, income) => sum + income.amount, 0),
    [incomes.content],
  )

  const recurringIncome = useMemo(
    () => incomes.content.filter((income) => income.isRecurring).reduce((sum, income) => sum + income.amount, 0),
    [incomes.content],
  )

  const oneOffIncome = totalIncome - recurringIncome

  const handleSubmit = async (values: IncomeFormValues) => {
    const payload = incomeSchema.parse({
      ...values,
      source: values.source?.trim() || "Salario",
    })

    try {
      if (editingTransaction) {
        await updateTransaction(editingTransaction.id, {
          type: "INCOME",
          amount: payload.amount,
          description: payload.description,
          transactionDate: payload.date,
          categoryId: payload.categoryId ?? undefined,
          notes: payload.source,
        })
        toast.success("Ingreso actualizado correctamente")
      } else {
        await createTransaction({
          type: "INCOME",
          amount: payload.amount,
          description: payload.description,
          transactionDate: payload.date,
          categoryId: payload.categoryId ?? undefined,
          notes: payload.source,
        })
        toast.success("Ingreso creado correctamente")
      }

      setModalOpen(false)
      setEditingTransaction(null)
    } catch (error) {
      toast.error(getApiErrorMessage(error, "No fue posible guardar el ingreso"))
    }
  }

  const handleDelete = async () => {
    if (!deletingTransaction) return

    try {
      await deleteTransaction(deletingTransaction.id)
      toast.success("Ingreso eliminado correctamente")
      setDeletingTransaction(null)
    } catch (error) {
      toast.error(getApiErrorMessage(error, "No fue posible eliminar el ingreso"))
    }
  }

  return (
    <AppLayout>
      <div className="flex flex-col gap-6">
        <div className="flex flex-col gap-4 sm:flex-row sm:items-center sm:justify-between">
          <div>
            <h1 className="text-2xl font-bold text-foreground">Ingresos</h1>
            <p className="text-sm text-muted-foreground">Registra y visualiza todos tus ingresos</p>
          </div>
          <Button
            onClick={() => {
              setEditingTransaction(null)
              setModalOpen(true)
            }}
          >
            <Plus data-icon="inline-start" />
            Agregar ingreso
          </Button>
        </div>

        <div className="grid grid-cols-1 gap-4 md:grid-cols-3">
          {[
            { label: "Total ingresos", value: totalIncome, color: "text-success" },
            { label: "Ingresos recurrentes", value: recurringIncome, color: "text-primary" },
            { label: "Ingresos unicos", value: oneOffIncome, color: "text-warning" },
          ].map((item) => (
            <div key={item.label} className="rounded-xl border border-border bg-card p-5">
              {isLoading ? (
                <div className="flex flex-col gap-2">
                  <Skeleton className="h-4 w-32" />
                  <Skeleton className="h-8 w-40" />
                </div>
              ) : (
                <div className="flex items-center gap-3">
                  <div className="flex size-10 items-center justify-center rounded-lg bg-success/10">
                    <TrendingUp className="text-success" />
                  </div>
                  <div>
                    <p className="text-sm text-muted-foreground">{item.label}</p>
                    <p className={`text-xl font-semibold ${item.color}`}>
                      +${item.value.toLocaleString("es-MX", { minimumFractionDigits: 2 })}
                    </p>
                  </div>
                </div>
              )}
            </div>
          ))}
        </div>

        <div className="grid grid-cols-1 gap-3 md:grid-cols-3">
          <Select
            value={selectedMonth}
            onValueChange={(value) => {
              setSelectedMonth(value)
              setPage(1)
            }}
          >
            <SelectTrigger>
              <SelectValue placeholder="Mes" />
            </SelectTrigger>
            <SelectContent>
              <SelectGroup>
                {monthOptions.map((month) => (
                  <SelectItem key={month.value} value={month.value}>
                    {month.label}
                  </SelectItem>
                ))}
              </SelectGroup>
            </SelectContent>
          </Select>

          <Select
            value={selectedYear}
            onValueChange={(value) => {
              setSelectedYear(value)
              setPage(1)
            }}
          >
            <SelectTrigger>
              <SelectValue placeholder="Ano" />
            </SelectTrigger>
            <SelectContent>
              <SelectGroup>
                {Array.from({ length: 5 }).map((_, index) => {
                  const year = String(now.getFullYear() - index)
                  return (
                    <SelectItem key={year} value={year}>
                      {year}
                    </SelectItem>
                  )
                })}
              </SelectGroup>
            </SelectContent>
          </Select>

          <div className="flex items-center justify-end gap-2">
            <Button variant="outline" disabled={page <= 1} onClick={() => setPage((value) => value - 1)}>
              Anterior
            </Button>
            <span className="text-sm text-muted-foreground">
              Pagina {Math.max(incomes.number + 1, page)} de {Math.max(incomes.totalPages, 1)}
            </span>
            <Button
              variant="outline"
              disabled={page >= Math.max(incomes.totalPages, 1)}
              onClick={() => setPage((value) => value + 1)}
            >
              Siguiente
            </Button>
          </div>
        </div>

        <IncomeTable
          incomes={incomes.content}
          isLoading={isLoading}
          onEdit={(income) => {
            const transaction = transactions.content.find((t) => t.id === income.id)
            if (transaction) {
              setEditingTransaction(transaction)
              setModalOpen(true)
            }
          }}
          onDelete={(income) => {
            const transaction = transactions.content.find((t) => t.id === income.id)
            if (transaction) {
              setDeletingTransaction(transaction)
            }
          }}
        />
      </div>

      <IncomeModal
        open={modalOpen}
        onOpenChange={(nextOpen) => {
          setModalOpen(nextOpen)
          if (!nextOpen) setEditingTransaction(null)
        }}
        initialValue={
          editingTransaction
            ? {
                id: editingTransaction.id,
                amount: editingTransaction.amount,
                description: editingTransaction.description,
                date: editingTransaction.transactionDate,
                isRecurring: false,
                source: editingTransaction.notes,
                categoryId: editingTransaction.categoryId,
                categoryName: editingTransaction.categoryName,
              }
            : null
        }
        onSubmit={handleSubmit}
        isSubmitting={isCreating || isUpdating}
      />

      <AlertDialog open={!!deletingTransaction} onOpenChange={(open) => !open && setDeletingTransaction(null)}>
        <AlertDialogContent>
          <AlertDialogHeader>
            <AlertDialogTitle>Eliminar ingreso</AlertDialogTitle>
            <AlertDialogDescription>
              Esta accion no se puede deshacer. El ingreso seleccionado se eliminara de forma permanente.
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
