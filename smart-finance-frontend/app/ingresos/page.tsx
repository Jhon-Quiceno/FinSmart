"use client"

import { useMemo, useState } from "react"
import { Plus, TrendingUp } from "lucide-react"
import { toast } from "sonner"
import { IncomeModal } from "@/components/income/income-modal"
import { IncomeTable } from "@/components/income/income-table"
import { AppLayout } from "@/components/layout/app-layout"
import { Button } from "@/components/ui/button"
import { Empty, EmptyDescription, EmptyHeader, EmptyMedia, EmptyTitle } from "@/components/ui/empty"
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
import { useCreateIncome, useDeleteIncome, useIncomes, useUpdateIncome } from "@/hooks/use-incomes"
import { toastApiError } from "@/lib/api-client"
import type { IncomeFormValues } from "@/lib/schemas/income.schema"
import type { Income, IncomeRequest } from "@/lib/types/income"

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

export default function IngresosPage() {
  const now = new Date()
  const [page, setPage] = useState(1)
  const [selectedMonth, setSelectedMonth] = useState(String(now.getMonth() + 1))
  const [selectedYear, setSelectedYear] = useState(String(now.getFullYear()))
  const [modalOpen, setModalOpen] = useState(false)
  const [editingIncome, setEditingIncome] = useState<Income | null>(null)
  const [deletingIncome, setDeletingIncome] = useState<Income | null>(null)

  const { incomes, isLoading } = useIncomes({
    page: page - 1,
    size: pageSize,
    month: Number(selectedMonth),
    year: Number(selectedYear),
  })

  const { createIncome, isLoading: isCreating } = useCreateIncome()
  const { updateIncome, isLoading: isUpdating } = useUpdateIncome()
  const { deleteIncome, isLoading: isDeleting } = useDeleteIncome()

  const totalIncome = useMemo(
    () => incomes.content.reduce((sum, income) => sum + income.amount, 0),
    [incomes.content],
  )

  const averageIncome = incomes.content.length > 0 ? totalIncome / incomes.content.length : 0

  const handleSubmit = async (values: IncomeFormValues) => {
    const payload: IncomeRequest = {
      amount: values.amount,
      description: values.description,
      date: values.date,
      categoryId: values.categoryId ?? undefined,
    }

    try {
      if (editingIncome) {
        await updateIncome(editingIncome.id, payload)
        toast.success("Ingreso actualizado correctamente")
      } else {
        await createIncome(payload)
        toast.success("Ingreso creado correctamente")
      }

      setModalOpen(false)
      setEditingIncome(null)
    } catch (error) {
      toastApiError(error, "No fue posible guardar el ingreso")
    }
  }

  const handleDelete = async () => {
    if (!deletingIncome) return

    try {
      await deleteIncome(deletingIncome.id)
      toast.success("Ingreso eliminado correctamente")
      setDeletingIncome(null)
    } catch (error) {
      toastApiError(error, "No fue posible eliminar el ingreso")
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
              setEditingIncome(null)
              setModalOpen(true)
            }}
          >
            <Plus data-icon="inline-start" />
            Agregar ingreso
          </Button>
        </div>

        <div className="grid grid-cols-1 gap-4 md:grid-cols-3">
          {[
            { label: "Total ingresos", value: totalIncome },
            { label: "Cantidad de ingresos", value: incomes.content.length },
            { label: "Promedio por ingreso", value: averageIncome },
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
                    <p className="text-xl font-semibold text-success">
                      {item.label === "Cantidad de ingresos"
                        ? item.value.toLocaleString("es-MX")
                        : `+$${item.value.toLocaleString("es-MX", { minimumFractionDigits: 2 })}`}
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

        {!isLoading && incomes.content.length === 0 ? (
          <Empty>
            <EmptyHeader>
              <EmptyMedia variant="icon">
                <TrendingUp />
              </EmptyMedia>
              <EmptyTitle>No hay ingresos registrados en este periodo</EmptyTitle>
              <EmptyDescription>
                Ajusta el mes y ano seleccionados o agrega tu primer ingreso para verlo reflejado aqui.
              </EmptyDescription>
            </EmptyHeader>
          </Empty>
        ) : (
          <IncomeTable
            incomes={incomes.content}
            isLoading={isLoading}
            onEdit={(income) => {
              setEditingIncome(income)
              setModalOpen(true)
            }}
            onDelete={(income) => setDeletingIncome(income)}
          />
        )}
      </div>

      <IncomeModal
        open={modalOpen}
        onOpenChange={(nextOpen) => {
          setModalOpen(nextOpen)
          if (!nextOpen) setEditingIncome(null)
        }}
        initialValue={editingIncome}
        onSubmit={handleSubmit}
        isSubmitting={isCreating || isUpdating}
      />

      <AlertDialog open={!!deletingIncome} onOpenChange={(open) => !open && setDeletingIncome(null)}>
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
