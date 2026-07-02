"use client"

import { useEffect } from "react"
import { zodResolver } from "@hookform/resolvers/zod"
import { useForm, type Resolver } from "react-hook-form"
import { Button } from "@/components/ui/button"
import { Dialog, DialogContent, DialogDescription, DialogFooter, DialogHeader, DialogTitle } from "@/components/ui/dialog"
import { Form, FormControl, FormField, FormItem, FormLabel, FormMessage } from "@/components/ui/form"
import { Input } from "@/components/ui/input"
import { debtEditSchema, debtSchema } from "@/lib/schemas/debt.schema"
import type { DebtFormValues } from "@/lib/schemas/debt.schema"
import type { Debt } from "@/lib/types/debt"

interface DebtModalProps {
  open: boolean
  onOpenChange: (open: boolean) => void
  initialValue?: Debt | null
  isSubmitting: boolean
  onSubmit: (values: DebtFormValues) => Promise<boolean>
}

const getDefaultValues = (debt?: Debt | null): DebtFormValues => ({
  name: debt?.name ?? "",
  totalAmount: debt?.totalAmount ?? 0,
  interestRate: debt?.interestRate ?? undefined,
  dueDate: debt?.dueDate ?? undefined,
})

export function DebtModal({ open, onOpenChange, initialValue, isSubmitting, onSubmit }: DebtModalProps) {
  const isEditing = !!initialValue

  // Edit mode validates against debtEditSchema (totalAmount omitted, since it's
  // locked and not sent to the backend); create mode validates the full debtSchema.
  const resolver = (
    isEditing ? zodResolver(debtEditSchema) : zodResolver(debtSchema)
  ) as unknown as Resolver<DebtFormValues>

  const form = useForm<DebtFormValues>({
    resolver,
    mode: "onSubmit",
    reValidateMode: "onBlur",
    defaultValues: getDefaultValues(initialValue),
  })

  useEffect(() => {
    if (open) {
      form.reset(getDefaultValues(initialValue))
    }
  }, [form, initialValue, open])

  const handleSubmit = form.handleSubmit(async (values) => {
    const success = await onSubmit(values)
    if (success) {
      form.reset(getDefaultValues(null))
    }
  })

  return (
    <Dialog open={open} onOpenChange={onOpenChange}>
      <DialogContent>
        <DialogHeader>
          <DialogTitle>{isEditing ? "Editar deuda" : "Crear deuda"}</DialogTitle>
          <DialogDescription>
            Completa la informacion para registrar la deuda en tu historial.
          </DialogDescription>
        </DialogHeader>

        <Form {...form}>
          <form onSubmit={handleSubmit} className="flex flex-col gap-4">
            <FormField
              control={form.control}
              name="name"
              render={({ field }) => (
                <FormItem>
                  <FormLabel>Nombre</FormLabel>
                  <FormControl>
                    <Input placeholder="Ej: Tarjeta BBVA" {...field} />
                  </FormControl>
                  <FormMessage />
                </FormItem>
              )}
            />

            <FormField
              control={form.control}
              name="totalAmount"
              render={({ field }) => (
                <FormItem>
                  <FormLabel>Monto total</FormLabel>
                  <FormControl>
                    <Input
                      type="number"
                      min="0"
                      step="0.01"
                      disabled={isEditing}
                      {...field}
                      onChange={(event) => field.onChange(event.target.value)}
                    />
                  </FormControl>
                  {isEditing && (
                    <p className="text-xs text-muted-foreground">
                      El monto total no se puede editar; los abonos se registran por separado.
                    </p>
                  )}
                  <FormMessage />
                </FormItem>
              )}
            />

            <FormField
              control={form.control}
              name="interestRate"
              render={({ field }) => (
                <FormItem>
                  <FormLabel>Tasa de interes (opcional)</FormLabel>
                  <FormControl>
                    <Input
                      type="number"
                      min="0"
                      step="0.01"
                      {...field}
                      value={field.value ?? ""}
                      onChange={(event) => field.onChange(event.target.value)}
                    />
                  </FormControl>
                  <FormMessage />
                </FormItem>
              )}
            />

            <FormField
              control={form.control}
              name="dueDate"
              render={({ field }) => (
                <FormItem>
                  <FormLabel>Fecha de vencimiento (opcional)</FormLabel>
                  <FormControl>
                    <Input type="date" {...field} value={field.value ?? ""} />
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
                {isSubmitting ? "Guardando..." : isEditing ? "Guardar cambios" : "Crear deuda"}
              </Button>
            </DialogFooter>
          </form>
        </Form>
      </DialogContent>
    </Dialog>
  )
}
