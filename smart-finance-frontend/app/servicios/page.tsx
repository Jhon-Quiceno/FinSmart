"use client"

import { useEffect, useMemo, useState } from "react"
import { Plus, Repeat, Wallet } from "lucide-react"
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
import { RecurringPaymentCard } from "@/components/recurring/recurring-payment-card"
import { RecurringPaymentModal } from "@/components/recurring/recurring-payment-modal"
import {
  useCreateRecurringPayment,
  useDeleteRecurringPayment,
  usePayRecurringPayment,
  useRecurringPayments,
  useToggleRecurringPayment,
  useUpdateRecurringPayment,
} from "@/hooks/use-recurring-payments"
import { getApiErrorMessage } from "@/lib/api-client"
import type { RecurringPaymentFormValues } from "@/lib/schemas/recurring-payment.schema"
import type {
  RecurringPayment,
  RecurringPaymentCreateRequest,
  RecurringPaymentUpdateRequest,
} from "@/lib/types/recurring-payment"

const pageSize = 9

// A month averages 52/12 ≈ 4.33 weeks. Used only to estimate the display total
// for weekly recurring payments; the backend's actual nextPaymentDate recalculation
// uses an exact plusWeeks(1), this constant is UI-only.
const WEEKS_PER_MONTH_ESTIMATE = 52 / 12

export default function ServiciosPage() {
  const [page, setPage] = useState(1)
  const [modalOpen, setModalOpen] = useState(false)
  const [editingItem, setEditingItem] = useState<RecurringPayment | null>(null)
  const [deletingItem, setDeletingItem] = useState<RecurringPayment | null>(null)

  const { recurringPayments, isLoading, error } = useRecurringPayments({ page: page - 1, size: pageSize })

  const { createRecurringPayment, isLoading: isCreating } = useCreateRecurringPayment()
  const { updateRecurringPayment, isLoading: isUpdating } = useUpdateRecurringPayment()
  const { deleteRecurringPayment, isLoading: isDeleting } = useDeleteRecurringPayment()
  const { toggleRecurringPayment, isLoading: isToggling } = useToggleRecurringPayment()
  const { payRecurringPayment, isLoading: isMarkingAsPaid } = usePayRecurringPayment()

  useEffect(() => {
    if (error) {
      toast.error(error)
    }
  }, [error])

  const activeCount = useMemo(
    () => recurringPayments.content.filter((item) => item.isActive).length,
    [recurringPayments.content],
  )
  const monthlyTotal = useMemo(
    () =>
      recurringPayments.content
        .filter((item) => item.isActive)
        .reduce(
          (sum, item) =>
            sum + (item.frequency === "MONTHLY" ? item.amount : item.amount * WEEKS_PER_MONTH_ESTIMATE),
          0,
        ),
    [recurringPayments.content],
  )

  const handleSubmit = async (values: RecurringPaymentFormValues): Promise<boolean> => {
    try {
      if (editingItem) {
        const payload: RecurringPaymentUpdateRequest = {
          name: values.name,
          amount: values.amount,
          frequency: values.frequency,
        }
        await updateRecurringPayment(editingItem.id, payload)
        toast.success("Servicio actualizado correctamente")
      } else {
        const payload: RecurringPaymentCreateRequest = {
          name: values.name,
          amount: values.amount,
          frequency: values.frequency,
          firstPaymentDate: values.firstPaymentDate,
        }
        await createRecurringPayment(payload)
        toast.success("Servicio creado correctamente")
      }

      setModalOpen(false)
      setEditingItem(null)
      return true
    } catch (error) {
      toast.error(getApiErrorMessage(error, "No fue posible guardar el servicio"))
      return false
    }
  }

  const handleDelete = async () => {
    if (!deletingItem) return

    try {
      await deleteRecurringPayment(deletingItem.id)
      toast.success("Servicio eliminado correctamente")
      setDeletingItem(null)
    } catch (error) {
      toast.error(getApiErrorMessage(error, "No fue posible eliminar el servicio"))
    }
  }

  const handleToggle = async (item: RecurringPayment) => {
    try {
      await toggleRecurringPayment(item.id)
      toast.success(item.isActive ? "Servicio desactivado" : "Servicio activado")
    } catch (error) {
      toast.error(getApiErrorMessage(error, "No fue posible cambiar el estado del servicio"))
    }
  }

  const handleMarkAsPaid = async (item: RecurringPayment) => {
    try {
      await payRecurringPayment(item.id)
      toast.success(`Pago de "${item.name}" registrado correctamente`)
    } catch (error) {
      toast.error(getApiErrorMessage(error, "No fue posible registrar el pago"))
    }
  }

  return (
    <AppLayout>
      <div className="space-y-6">
        <div className="flex flex-col sm:flex-row sm:items-center sm:justify-between gap-4">
          <div>
            <h1 className="text-2xl font-bold text-foreground">Servicios y Suscripciones</h1>
            <p className="text-sm text-muted-foreground">Administra tus pagos recurrentes</p>
          </div>
          <Button
            onClick={() => {
              setEditingItem(null)
              setModalOpen(true)
            }}
          >
            <Plus data-icon="inline-start" />
            Agregar servicio
          </Button>
        </div>

        <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
          {isLoading ? (
            <>
              <Skeleton className="h-20 w-full" />
              <Skeleton className="h-20 w-full" />
            </>
          ) : (
            <>
              <div className="rounded-xl bg-card border border-border p-5">
                <div className="flex items-center gap-3">
                  <div className="flex h-10 w-10 items-center justify-center rounded-lg bg-primary/10">
                    <Repeat className="h-5 w-5 text-primary" />
                  </div>
                  <div>
                    <p className="text-sm text-muted-foreground">Servicios activos</p>
                    <p className="text-xl font-bold text-foreground">{activeCount}</p>
                  </div>
                </div>
              </div>

              <div className="rounded-xl bg-card border border-border p-5">
                <div className="flex items-center gap-3">
                  <div className="flex h-10 w-10 items-center justify-center rounded-lg bg-warning/10">
                    <Wallet className="h-5 w-5 text-warning" />
                  </div>
                  <div>
                    <p className="text-sm text-muted-foreground">Estimado mensual</p>
                    <p className="text-xl font-bold text-foreground">
                      ${monthlyTotal.toLocaleString("es-MX", { minimumFractionDigits: 2, maximumFractionDigits: 2 })}
                    </p>
                  </div>
                </div>
              </div>
            </>
          )}
        </div>

        <div className="flex items-center justify-end gap-2">
          <Button variant="outline" disabled={page <= 1} onClick={() => setPage((value) => value - 1)}>
            Anterior
          </Button>
          <span className="text-sm text-muted-foreground">
            Pagina {Math.max(recurringPayments.number + 1, page)} de {Math.max(recurringPayments.totalPages, 1)}
          </span>
          <Button
            variant="outline"
            disabled={page >= Math.max(recurringPayments.totalPages, 1)}
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
        ) : recurringPayments.content.length === 0 ? (
          <div className="rounded-xl border border-dashed border-border p-10 text-center text-sm text-muted-foreground">
            Todavia no tienes servicios recurrentes registrados.
          </div>
        ) : (
          <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-4">
            {recurringPayments.content.map((item) => (
              <RecurringPaymentCard
                key={item.id}
                recurringPayment={item}
                onEdit={(value) => {
                  setEditingItem(value)
                  setModalOpen(true)
                }}
                onDelete={(value) => setDeletingItem(value)}
                onToggle={handleToggle}
                onMarkAsPaid={handleMarkAsPaid}
                isToggling={isToggling}
                isMarkingAsPaid={isMarkingAsPaid}
              />
            ))}
          </div>
        )}
      </div>

      <RecurringPaymentModal
        open={modalOpen}
        onOpenChange={(nextOpen) => {
          setModalOpen(nextOpen)
          if (!nextOpen) setEditingItem(null)
        }}
        initialValue={editingItem}
        onSubmit={handleSubmit}
        isSubmitting={isCreating || isUpdating}
      />

      <AlertDialog open={!!deletingItem} onOpenChange={(open) => !open && setDeletingItem(null)}>
        <AlertDialogContent>
          <AlertDialogHeader>
            <AlertDialogTitle>Eliminar servicio</AlertDialogTitle>
            <AlertDialogDescription>
              Esta accion no se puede deshacer. El servicio recurrente seleccionado se eliminara de forma
              permanente.
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
