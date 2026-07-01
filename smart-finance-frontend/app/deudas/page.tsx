"use client"

import { useMemo, useState } from "react"
import { AlertCircle, CreditCard, Plus, TrendingDown } from "lucide-react"
import { toast } from "sonner"
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
import { AppLayout } from "@/components/layout/app-layout"
import { Button } from "@/components/ui/button"
import { Skeleton } from "@/components/ui/skeleton"
import { DebtCard } from "@/components/debts/debt-card"
import { DebtModal } from "@/components/debts/debt-modal"
import { DebtPaymentModal } from "@/components/debts/debt-payment-modal"
import { DebtPaymentHistoryDialog } from "@/components/debts/debt-payment-history-dialog"
import { useCreateDebt, useDebts, useDeleteDebt, useUpdateDebt } from "@/hooks/use-debts"
import { useCreateDebtPayment } from "@/hooks/use-debt-payments"
import { getApiErrorMessage } from "@/lib/api-client"
import type { DebtFormValues } from "@/lib/schemas/debt.schema"
import type { DebtPaymentFormValues } from "@/lib/schemas/debt-payment.schema"
import type { Debt, DebtCreateRequest, DebtUpdateRequest } from "@/lib/types/debt"
import type { DebtPaymentRequest } from "@/lib/types/debt-payment"

const pageSize = 9

export default function DeudasPage() {
  const [page, setPage] = useState(1)
  const [modalOpen, setModalOpen] = useState(false)
  const [editingDebt, setEditingDebt] = useState<Debt | null>(null)
  const [deletingDebt, setDeletingDebt] = useState<Debt | null>(null)
  const [payingDebt, setPayingDebt] = useState<Debt | null>(null)
  const [historyDebt, setHistoryDebt] = useState<Debt | null>(null)

  const { debts, isLoading } = useDebts({ page: page - 1, size: pageSize })

  const { createDebt, isLoading: isCreating } = useCreateDebt()
  const { updateDebt, isLoading: isUpdating } = useUpdateDebt()
  const { deleteDebt, isLoading: isDeleting } = useDeleteDebt()
  const { createDebtPayment, isLoading: isPaying } = useCreateDebtPayment()

  const totalDebt = useMemo(() => debts.content.reduce((sum, debt) => sum + debt.totalAmount, 0), [debts.content])
  const totalRemaining = useMemo(
    () => debts.content.reduce((sum, debt) => sum + debt.remainingAmount, 0),
    [debts.content],
  )
  const totalPaid = totalDebt - totalRemaining
  const overallProgress = totalDebt > 0 ? (totalPaid / totalDebt) * 100 : 0

  const handleSubmit = async (values: DebtFormValues) => {
    try {
      if (editingDebt) {
        const payload: DebtUpdateRequest = {
          name: values.name,
          interestRate: values.interestRate ?? null,
          dueDate: values.dueDate ?? null,
        }
        await updateDebt(editingDebt.id, payload)
        toast.success("Deuda actualizada correctamente")
      } else {
        const payload: DebtCreateRequest = {
          name: values.name,
          totalAmount: values.totalAmount,
          interestRate: values.interestRate ?? null,
          dueDate: values.dueDate ?? null,
        }
        await createDebt(payload)
        toast.success("Deuda creada correctamente")
      }

      setModalOpen(false)
      setEditingDebt(null)
    } catch (error) {
      toast.error(getApiErrorMessage(error, "No fue posible guardar la deuda"))
    }
  }

  const handleDelete = async () => {
    if (!deletingDebt) return

    try {
      await deleteDebt(deletingDebt.id)
      toast.success("Deuda eliminada correctamente")
      setDeletingDebt(null)
    } catch (error) {
      toast.error(getApiErrorMessage(error, "No fue posible eliminar la deuda"))
    }
  }

  const handleRegisterPayment = async (values: DebtPaymentFormValues) => {
    if (!payingDebt) return

    const payload: DebtPaymentRequest = {
      amount: values.amount,
      paymentDate: values.paymentDate,
      note: values.note,
    }

    try {
      await createDebtPayment(payingDebt.id, payload)
      toast.success("Pago registrado correctamente")
      setPayingDebt(null)
    } catch (error) {
      toast.error(getApiErrorMessage(error, "No fue posible registrar el pago"))
    }
  }

  return (
    <AppLayout>
      <div className="space-y-6">
        <div className="flex flex-col sm:flex-row sm:items-center sm:justify-between gap-4">
          <div>
            <h1 className="text-2xl font-bold text-foreground">Deudas</h1>
            <p className="text-sm text-muted-foreground">Gestiona y monitorea todas tus deudas</p>
          </div>
          <Button
            className="bg-warning text-warning-foreground hover:bg-warning/90"
            onClick={() => {
              setEditingDebt(null)
              setModalOpen(true)
            }}
          >
            <Plus data-icon="inline-start" />
            Agregar Deuda
          </Button>
        </div>

        <div className="grid grid-cols-1 md:grid-cols-3 gap-4">
          {isLoading ? (
            <>
              <Skeleton className="h-20 w-full" />
              <Skeleton className="h-20 w-full" />
              <Skeleton className="h-20 w-full" />
            </>
          ) : (
            <>
              <div className="rounded-xl bg-card border border-border p-5">
                <div className="flex items-center gap-3">
                  <div className="flex h-10 w-10 items-center justify-center rounded-lg bg-warning/10">
                    <CreditCard className="h-5 w-5 text-warning" />
                  </div>
                  <div>
                    <p className="text-sm text-muted-foreground">Deuda Total</p>
                    <p className="text-xl font-bold text-foreground">${totalDebt.toLocaleString("es-MX")}</p>
                  </div>
                </div>
              </div>

              <div className="rounded-xl bg-card border border-border p-5">
                <div className="flex items-center gap-3">
                  <div className="flex h-10 w-10 items-center justify-center rounded-lg bg-success/10">
                    <TrendingDown className="h-5 w-5 text-success" />
                  </div>
                  <div>
                    <p className="text-sm text-muted-foreground">Total Pagado</p>
                    <p className="text-xl font-bold text-success">${totalPaid.toLocaleString("es-MX")}</p>
                  </div>
                </div>
              </div>

              <div className="rounded-xl bg-card border border-border p-5">
                <div className="flex items-center gap-3">
                  <div className="flex h-10 w-10 items-center justify-center rounded-lg bg-destructive/10">
                    <AlertCircle className="h-5 w-5 text-destructive" />
                  </div>
                  <div>
                    <p className="text-sm text-muted-foreground">Por Pagar</p>
                    <p className="text-xl font-bold text-destructive">${totalRemaining.toLocaleString("es-MX")}</p>
                  </div>
                </div>
              </div>
            </>
          )}
        </div>

        {!isLoading && debts.content.length > 0 && (
          <div className="rounded-xl bg-card border border-border p-5">
            <div className="flex items-center justify-between mb-3">
              <h3 className="text-sm font-medium text-foreground">Progreso General de Pago</h3>
              <span className="text-sm font-semibold text-primary">{overallProgress.toFixed(1)}%</span>
            </div>
            <div className="h-3 rounded-full bg-secondary overflow-hidden">
              <div
                className="h-full rounded-full bg-gradient-to-r from-primary to-success transition-all duration-500"
                style={{ width: `${Math.min(Math.max(overallProgress, 0), 100)}%` }}
              />
            </div>
            <p className="mt-2 text-xs text-muted-foreground">
              Has pagado ${totalPaid.toLocaleString("es-MX")} de ${totalDebt.toLocaleString("es-MX")}
            </p>
          </div>
        )}

        <div className="flex items-center justify-end gap-2">
          <Button variant="outline" disabled={page <= 1} onClick={() => setPage((value) => value - 1)}>
            Anterior
          </Button>
          <span className="text-sm text-muted-foreground">
            Pagina {Math.max(debts.number + 1, page)} de {Math.max(debts.totalPages, 1)}
          </span>
          <Button
            variant="outline"
            disabled={page >= Math.max(debts.totalPages, 1)}
            onClick={() => setPage((value) => value + 1)}
          >
            Siguiente
          </Button>
        </div>

        {isLoading ? (
          <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-4">
            {Array.from({ length: 3 }).map((_, index) => (
              <Skeleton key={index} className="h-64 w-full" />
            ))}
          </div>
        ) : debts.content.length === 0 ? (
          <div className="rounded-xl border border-dashed border-border p-10 text-center text-sm text-muted-foreground">
            Todavia no tienes deudas registradas.
          </div>
        ) : (
          <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-4">
            {debts.content.map((debt) => (
              <DebtCard
                key={debt.id}
                debt={debt}
                onEdit={(value) => {
                  setEditingDebt(value)
                  setModalOpen(true)
                }}
                onDelete={(value) => setDeletingDebt(value)}
                onRegisterPayment={(value) => setPayingDebt(value)}
                onViewHistory={(value) => setHistoryDebt(value)}
              />
            ))}
          </div>
        )}
      </div>

      <DebtModal
        open={modalOpen}
        onOpenChange={(nextOpen) => {
          setModalOpen(nextOpen)
          if (!nextOpen) setEditingDebt(null)
        }}
        initialValue={editingDebt}
        onSubmit={handleSubmit}
        isSubmitting={isCreating || isUpdating}
      />

      <DebtPaymentModal
        open={!!payingDebt}
        onOpenChange={(nextOpen) => {
          if (!nextOpen) setPayingDebt(null)
        }}
        debt={payingDebt}
        onSubmit={handleRegisterPayment}
        isSubmitting={isPaying}
      />

      <DebtPaymentHistoryDialog
        open={!!historyDebt}
        onOpenChange={(nextOpen) => {
          if (!nextOpen) setHistoryDebt(null)
        }}
        debt={historyDebt}
      />

      <AlertDialog open={!!deletingDebt} onOpenChange={(open) => !open && setDeletingDebt(null)}>
        <AlertDialogContent>
          <AlertDialogHeader>
            <AlertDialogTitle>Eliminar deuda</AlertDialogTitle>
            <AlertDialogDescription>
              Esta accion no se puede deshacer. La deuda seleccionada se eliminara de forma permanente.
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
