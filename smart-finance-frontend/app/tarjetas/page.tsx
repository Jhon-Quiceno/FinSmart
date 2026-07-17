"use client"

import { useEffect, useMemo, useState } from "react"
import { AlertCircle, Plus, Wallet, WalletCards } from "lucide-react"
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
import { Empty, EmptyDescription, EmptyHeader, EmptyMedia, EmptyTitle } from "@/components/ui/empty"
import { Skeleton } from "@/components/ui/skeleton"
import { CreditCardItem } from "@/components/cards/credit-card-item"
import { CreditCardModal } from "@/components/cards/credit-card-modal"
import { RegisterPurchaseDialog } from "@/components/cards/register-purchase-dialog"
import { RegisterPaymentDialog } from "@/components/cards/register-payment-dialog"
import { CardMovementsDialog } from "@/components/cards/card-movements-dialog"
import {
  useCreateCreditCard,
  useCreditCards,
  useDeleteCreditCard,
  useUpdateCreditCard,
} from "@/hooks/use-credit-cards"
import { useRegisterPayment, useRegisterPurchase } from "@/hooks/use-card-movements"
import { toastApiError } from "@/lib/api-client"
import type { CreditCardFormValues } from "@/lib/schemas/credit-card.schema"
import type { CardPaymentFormValues, CardPurchaseFormValues } from "@/lib/schemas/card-movement.schema"
import type { CreditCard, CreditCardCreateRequest, CreditCardUpdateRequest } from "@/lib/types/credit-card"

const pageSize = 9

function formatCurrency(value: number): string {
  return `$${value.toLocaleString("es-MX", { minimumFractionDigits: 2, maximumFractionDigits: 2 })}`
}

export default function TarjetasPage() {
  const [page, setPage] = useState(1)
  const [modalOpen, setModalOpen] = useState(false)
  const [editingCard, setEditingCard] = useState<CreditCard | null>(null)
  const [deletingCard, setDeletingCard] = useState<CreditCard | null>(null)
  const [purchasingCard, setPurchasingCard] = useState<CreditCard | null>(null)
  const [payingCard, setPayingCard] = useState<CreditCard | null>(null)
  const [movementsCard, setMovementsCard] = useState<CreditCard | null>(null)

  const { cards, isLoading, error } = useCreditCards({ page: page - 1, size: pageSize })

  const { createCreditCard, isLoading: isCreating } = useCreateCreditCard()
  const { updateCreditCard, isLoading: isUpdating } = useUpdateCreditCard()
  const { deleteCreditCard, isLoading: isDeleting } = useDeleteCreditCard()
  const { registerPurchase, isLoading: isRegisteringPurchase } = useRegisterPurchase()
  const { registerPayment, isLoading: isRegisteringPayment } = useRegisterPayment()

  useEffect(() => {
    if (error) {
      toast.error(error)
    }
  }, [error])

  const totalCreditLimit = useMemo(() => cards.content.reduce((sum, card) => sum + card.creditLimit, 0), [cards.content])
  const totalBalance = useMemo(() => cards.content.reduce((sum, card) => sum + card.currentBalance, 0), [cards.content])
  const totalAvailable = useMemo(
    () => cards.content.reduce((sum, card) => sum + card.availableCredit, 0),
    [cards.content],
  )

  const handleSubmit = async (values: CreditCardFormValues): Promise<boolean> => {
    try {
      if (editingCard) {
        const payload: CreditCardUpdateRequest = {
          name: values.name,
          bank: values.bank,
          monthlyRate: values.monthlyRate,
          cutoffDay: values.cutoffDay,
          paymentDueDay: values.paymentDueDay,
        }
        await updateCreditCard(editingCard.id, payload)
        toast.success("Tarjeta actualizada correctamente")
      } else {
        const payload: CreditCardCreateRequest = {
          name: values.name,
          bank: values.bank,
          franchise: values.franchise,
          creditLimit: values.creditLimit,
          monthlyRate: values.monthlyRate,
          cutoffDay: values.cutoffDay,
          paymentDueDay: values.paymentDueDay,
        }
        await createCreditCard(payload)
        toast.success("Tarjeta creada correctamente")
      }

      setModalOpen(false)
      setEditingCard(null)
      return true
    } catch (error) {
      toastApiError(error, "No fue posible guardar la tarjeta")
      return false
    }
  }

  const handleDelete = async () => {
    if (!deletingCard) return

    try {
      await deleteCreditCard(deletingCard.id)
      toast.success("Tarjeta eliminada correctamente")
      setDeletingCard(null)
    } catch (error) {
      toastApiError(error, "No fue posible eliminar la tarjeta")
    }
  }

  const handleRegisterPurchase = async (values: CardPurchaseFormValues): Promise<boolean> => {
    if (!purchasingCard) return false

    try {
      const movement = await registerPurchase(purchasingCard.id, {
        amount: values.amount,
        date: values.date,
        description: values.description,
        installmentCount: values.installmentCount,
      })
      toast.success(
        movement.cardBalanceAfter !== null
          ? `Compra registrada correctamente. Nuevo saldo: ${formatCurrency(movement.cardBalanceAfter)}`
          : "Compra registrada correctamente",
      )
      setPurchasingCard(null)
      return true
    } catch (error) {
      toastApiError(error, "No fue posible registrar la compra")
      return false
    }
  }

  const handleRegisterPayment = async (values: CardPaymentFormValues): Promise<boolean> => {
    if (!payingCard) return false

    try {
      const movement = await registerPayment(payingCard.id, {
        amount: values.amount,
        date: values.date,
        description: values.description,
      })
      toast.success(
        movement.cardBalanceAfter !== null
          ? `Pago registrado correctamente. Nuevo saldo: ${formatCurrency(movement.cardBalanceAfter)}`
          : "Pago registrado correctamente",
      )
      setPayingCard(null)
      return true
    } catch (error) {
      toastApiError(error, "No fue posible registrar el pago")
      return false
    }
  }

  return (
    <AppLayout>
      <div className="space-y-6">
        <div className="flex flex-col sm:flex-row sm:items-center sm:justify-between gap-4">
          <div>
            <h1 className="text-2xl font-bold text-foreground">Tarjetas</h1>
            <p className="text-sm text-muted-foreground">Gestiona tus tarjetas de credito y sus movimientos</p>
          </div>
          <Button
            onClick={() => {
              setEditingCard(null)
              setModalOpen(true)
            }}
          >
            <Plus data-icon="inline-start" />
            Agregar tarjeta
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
                  <div className="flex h-10 w-10 items-center justify-center rounded-lg bg-primary/10">
                    <WalletCards className="h-5 w-5 text-primary" />
                  </div>
                  <div>
                    <p className="text-sm text-muted-foreground">Cupo Total</p>
                    <p className="text-xl font-bold text-foreground">{formatCurrency(totalCreditLimit)}</p>
                  </div>
                </div>
              </div>

              <div className="rounded-xl bg-card border border-border p-5">
                <div className="flex items-center gap-3">
                  <div className="flex h-10 w-10 items-center justify-center rounded-lg bg-destructive/10">
                    <AlertCircle className="h-5 w-5 text-destructive" />
                  </div>
                  <div>
                    <p className="text-sm text-muted-foreground">Saldo Total</p>
                    <p className="text-xl font-bold text-destructive">{formatCurrency(totalBalance)}</p>
                  </div>
                </div>
              </div>

              <div className="rounded-xl bg-card border border-border p-5">
                <div className="flex items-center gap-3">
                  <div className="flex h-10 w-10 items-center justify-center rounded-lg bg-success/10">
                    <Wallet className="h-5 w-5 text-success" />
                  </div>
                  <div>
                    <p className="text-sm text-muted-foreground">Disponible Total</p>
                    <p className="text-xl font-bold text-success">{formatCurrency(totalAvailable)}</p>
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
            Pagina {Math.max(cards.number + 1, page)} de {Math.max(cards.totalPages, 1)}
          </span>
          <Button
            variant="outline"
            disabled={page >= Math.max(cards.totalPages, 1)}
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
        ) : cards.content.length === 0 ? (
          <Empty>
            <EmptyHeader>
              <EmptyMedia variant="icon">
                <WalletCards />
              </EmptyMedia>
              <EmptyTitle>No tenes tarjetas registradas</EmptyTitle>
              <EmptyDescription>
                Agrega tu primera tarjeta de credito para comenzar a registrar compras y pagos.
              </EmptyDescription>
            </EmptyHeader>
          </Empty>
        ) : (
          <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-4">
            {cards.content.map((card) => (
              <CreditCardItem
                key={card.id}
                card={card}
                onEdit={(value) => {
                  setEditingCard(value)
                  setModalOpen(true)
                }}
                onDelete={(value) => setDeletingCard(value)}
                onRegisterPurchase={(value) => setPurchasingCard(value)}
                onRegisterPayment={(value) => setPayingCard(value)}
                onViewMovements={(value) => setMovementsCard(value)}
              />
            ))}
          </div>
        )}
      </div>

      <CreditCardModal
        open={modalOpen}
        onOpenChange={(nextOpen) => {
          setModalOpen(nextOpen)
          if (!nextOpen) setEditingCard(null)
        }}
        initialValue={editingCard}
        onSubmit={handleSubmit}
        isSubmitting={isCreating || isUpdating}
      />

      <RegisterPurchaseDialog
        open={!!purchasingCard}
        onOpenChange={(nextOpen) => {
          if (!nextOpen) setPurchasingCard(null)
        }}
        card={purchasingCard}
        onSubmit={handleRegisterPurchase}
        isSubmitting={isRegisteringPurchase}
      />

      <RegisterPaymentDialog
        open={!!payingCard}
        onOpenChange={(nextOpen) => {
          if (!nextOpen) setPayingCard(null)
        }}
        card={payingCard}
        onSubmit={handleRegisterPayment}
        isSubmitting={isRegisteringPayment}
      />

      <CardMovementsDialog
        open={!!movementsCard}
        onOpenChange={(nextOpen) => {
          if (!nextOpen) setMovementsCard(null)
        }}
        card={movementsCard}
      />

      <AlertDialog open={!!deletingCard} onOpenChange={(open) => !open && setDeletingCard(null)}>
        <AlertDialogContent>
          <AlertDialogHeader>
            <AlertDialogTitle>Eliminar tarjeta</AlertDialogTitle>
            <AlertDialogDescription>
              Esta accion no se puede deshacer. La tarjeta y su historial de movimientos se eliminaran de forma
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
