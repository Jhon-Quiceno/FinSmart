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
import { cardPurchaseSchema } from "@/lib/schemas/card-movement.schema"
import type { CardPurchaseFormValues } from "@/lib/schemas/card-movement.schema"
import type { CreditCard } from "@/lib/types/credit-card"

interface RegisterPurchaseDialogProps {
  open: boolean
  onOpenChange: (open: boolean) => void
  card: CreditCard | null
  isSubmitting: boolean
  onSubmit: (values: CardPurchaseFormValues) => Promise<boolean>
}

const getDefaultValues = (): CardPurchaseFormValues => ({
  amount: 0,
  date: getTodayDateInput(),
  description: "",
  installmentCount: undefined,
})

export function RegisterPurchaseDialog({ open, onOpenChange, card, isSubmitting, onSubmit }: RegisterPurchaseDialogProps) {
  const form = useForm<CardPurchaseFormValues>({
    resolver: zodResolver(cardPurchaseSchema),
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
          <DialogTitle>Registrar compra</DialogTitle>
          <DialogDescription>
            {card
              ? `Registra una compra para "${card.name}". El saldo se actualizara automaticamente.`
              : "Registra una compra para esta tarjeta."}
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
              name="date"
              render={({ field }) => (
                <FormItem>
                  <FormLabel>Fecha (opcional)</FormLabel>
                  <FormControl>
                    <Input type="date" {...field} value={field.value ?? ""} />
                  </FormControl>
                  <FormMessage />
                </FormItem>
              )}
            />

            <FormField
              control={form.control}
              name="description"
              render={({ field }) => (
                <FormItem>
                  <FormLabel>Descripcion (opcional)</FormLabel>
                  <FormControl>
                    <Textarea placeholder="Ej: Compra en linea" {...field} value={field.value ?? ""} />
                  </FormControl>
                  <FormMessage />
                </FormItem>
              )}
            />

            <FormField
              control={form.control}
              name="installmentCount"
              render={({ field }) => (
                <FormItem>
                  <FormLabel>Cuotas (opcional)</FormLabel>
                  <FormControl>
                    <Input
                      type="number"
                      min="2"
                      max="48"
                      step="1"
                      placeholder="Dejar vacio para pago en una sola cuota"
                      value={field.value ?? ""}
                      onChange={(event) => field.onChange(event.target.value === "" ? undefined : event.target.value)}
                    />
                  </FormControl>
                  <p className="text-xs text-muted-foreground">
                    Entre 2 y 48 cuotas. Si se deja vacio, la compra se registra en una sola cuota sin intereses.
                  </p>
                  <FormMessage />
                </FormItem>
              )}
            />

            <DialogFooter>
              <Button type="button" variant="outline" onClick={() => onOpenChange(false)}>
                Cancelar
              </Button>
              <Button type="submit" disabled={isSubmitting}>
                {isSubmitting ? "Guardando..." : "Registrar compra"}
              </Button>
            </DialogFooter>
          </form>
        </Form>
      </DialogContent>
    </Dialog>
  )
}
