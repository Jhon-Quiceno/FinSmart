"use client"

import { useEffect } from "react"
import { zodResolver } from "@hookform/resolvers/zod"
import { useForm } from "react-hook-form"
import { Button } from "@/components/ui/button"
import { Dialog, DialogContent, DialogDescription, DialogFooter, DialogHeader, DialogTitle } from "@/components/ui/dialog"
import { Form, FormControl, FormField, FormItem, FormLabel, FormMessage } from "@/components/ui/form"
import { Input } from "@/components/ui/input"
import { Textarea } from "@/components/ui/textarea"
import { getTodayDateInput } from "@/lib/date"
import { debtPaymentSchema } from "@/lib/schemas/debt-payment.schema"
import type { DebtPaymentFormValues } from "@/lib/schemas/debt-payment.schema"
import type { Debt } from "@/lib/types/debt"

interface DebtPaymentModalProps {
  open: boolean
  onOpenChange: (open: boolean) => void
  debt: Debt | null
  isSubmitting: boolean
  onSubmit: (values: DebtPaymentFormValues) => Promise<boolean>
}

const getDefaultValues = (): DebtPaymentFormValues => ({
  amount: 0,
  paymentDate: getTodayDateInput(),
  note: "",
})

export function DebtPaymentModal({ open, onOpenChange, debt, isSubmitting, onSubmit }: DebtPaymentModalProps) {
  const form = useForm<DebtPaymentFormValues>({
    resolver: zodResolver(debtPaymentSchema),
    mode: "onSubmit",
    reValidateMode: "onBlur",
    defaultValues: getDefaultValues(),
  })

  useEffect(() => {
    if (open) {
      form.reset(getDefaultValues())
    }
  }, [form, open])

  const handleSubmit = form.handleSubmit(async (values) => {
    const success = await onSubmit(values)
    if (success) {
      form.reset(getDefaultValues())
    }
  })

  return (
    <Dialog open={open} onOpenChange={onOpenChange}>
      <DialogContent>
        <DialogHeader>
          <DialogTitle>Registrar pago</DialogTitle>
          <DialogDescription>
            {debt
              ? `Registra un abono para "${debt.name}". El monto restante se actualizara automaticamente.`
              : "Registra un abono para esta deuda."}
          </DialogDescription>
        </DialogHeader>

        <Form {...form}>
          <form onSubmit={handleSubmit} className="flex flex-col gap-4">
            <FormField
              control={form.control}
              name="amount"
              render={({ field }) => (
                <FormItem>
                  <FormLabel>Monto</FormLabel>
                  <FormControl>
                    <Input
                      type="number"
                      min="0"
                      step="0.01"
                      {...field}
                      onChange={(event) => field.onChange(event.target.value)}
                    />
                  </FormControl>
                  <FormMessage />
                </FormItem>
              )}
            />

            <FormField
              control={form.control}
              name="paymentDate"
              render={({ field }) => (
                <FormItem>
                  <FormLabel>Fecha de pago</FormLabel>
                  <FormControl>
                    <Input type="date" {...field} />
                  </FormControl>
                  <FormMessage />
                </FormItem>
              )}
            />

            <FormField
              control={form.control}
              name="note"
              render={({ field }) => (
                <FormItem>
                  <FormLabel>Nota (opcional)</FormLabel>
                  <FormControl>
                    <Textarea placeholder="Ej: Pago mensual" {...field} value={field.value ?? ""} />
                  </FormControl>
                  <FormMessage />
                </FormItem>
              )}
            />

            <DialogFooter>
              <Button type="button" variant="outline" onClick={() => onOpenChange(false)}>
                Cancelar
              </Button>
              <Button type="submit" disabled={isSubmitting}>
                {isSubmitting ? "Guardando..." : "Registrar pago"}
              </Button>
            </DialogFooter>
          </form>
        </Form>
      </DialogContent>
    </Dialog>
  )
}
