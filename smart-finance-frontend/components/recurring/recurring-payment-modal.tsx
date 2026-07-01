"use client"

import { useEffect } from "react"
import { zodResolver } from "@hookform/resolvers/zod"
import { useForm } from "react-hook-form"
import { Button } from "@/components/ui/button"
import { Dialog, DialogContent, DialogDescription, DialogFooter, DialogHeader, DialogTitle } from "@/components/ui/dialog"
import { Form, FormControl, FormField, FormItem, FormLabel, FormMessage } from "@/components/ui/form"
import { Input } from "@/components/ui/input"
import { Select, SelectContent, SelectGroup, SelectItem, SelectTrigger, SelectValue } from "@/components/ui/select"
import { getTodayDateInput } from "@/lib/date"
import { recurringPaymentSchema } from "@/lib/schemas/recurring-payment.schema"
import type { RecurringPaymentFormValues } from "@/lib/schemas/recurring-payment.schema"
import { RECURRING_FREQUENCIES } from "@/lib/types/recurring-payment"
import type { RecurringFrequency, RecurringPayment } from "@/lib/types/recurring-payment"

interface RecurringPaymentModalProps {
  open: boolean
  onOpenChange: (open: boolean) => void
  initialValue?: RecurringPayment | null
  isSubmitting: boolean
  onSubmit: (values: RecurringPaymentFormValues) => Promise<void>
}

const frequencyOptions: { value: RecurringFrequency; label: string }[] = [
  { value: "MONTHLY", label: "Mensual" },
  { value: "WEEKLY", label: "Semanal" },
]

const getDefaultValues = (item?: RecurringPayment | null): RecurringPaymentFormValues => ({
  name: item?.name ?? "",
  amount: item?.amount ?? 0,
  frequency: item?.frequency ?? RECURRING_FREQUENCIES[0],
  firstPaymentDate: item?.nextPaymentDate ?? getTodayDateInput(),
})

export function RecurringPaymentModal({
  open,
  onOpenChange,
  initialValue,
  isSubmitting,
  onSubmit,
}: RecurringPaymentModalProps) {
  const isEditing = !!initialValue

  const form = useForm<RecurringPaymentFormValues>({
    resolver: zodResolver(recurringPaymentSchema),
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
    await onSubmit(values)
    form.reset(getDefaultValues(null))
  })

  return (
    <Dialog open={open} onOpenChange={onOpenChange}>
      <DialogContent>
        <DialogHeader>
          <DialogTitle>{isEditing ? "Editar servicio" : "Crear servicio recurrente"}</DialogTitle>
          <DialogDescription>
            Completa la informacion para {isEditing ? "actualizar" : "registrar"} el servicio recurrente.
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
                    <Input placeholder="Ej: Netflix" {...field} />
                  </FormControl>
                  <FormMessage />
                </FormItem>
              )}
            />

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
              name="frequency"
              render={({ field }) => (
                <FormItem>
                  <FormLabel>Frecuencia</FormLabel>
                  <Select value={field.value} onValueChange={field.onChange}>
                    <FormControl>
                      <SelectTrigger>
                        <SelectValue placeholder="Selecciona una frecuencia" />
                      </SelectTrigger>
                    </FormControl>
                    <SelectContent>
                      <SelectGroup>
                        {frequencyOptions.map((option) => (
                          <SelectItem key={option.value} value={option.value}>
                            {option.label}
                          </SelectItem>
                        ))}
                      </SelectGroup>
                    </SelectContent>
                  </Select>
                  <FormMessage />
                </FormItem>
              )}
            />

            <FormField
              control={form.control}
              name="firstPaymentDate"
              render={({ field }) => (
                <FormItem>
                  <FormLabel>Fecha del primer pago</FormLabel>
                  <FormControl>
                    <Input type="date" disabled={isEditing} {...field} />
                  </FormControl>
                  {isEditing && (
                    <p className="text-xs text-muted-foreground">
                      La proxima fecha de pago no se modifica al editar; usa &quot;Marcar como pagado&quot; para
                      avanzarla.
                    </p>
                  )}
                  <FormMessage />
                </FormItem>
              )}
            />

            <DialogFooter>
              <Button type="button" variant="outline" onClick={() => onOpenChange(false)}>
                Cancelar
              </Button>
              <Button type="submit" disabled={isSubmitting}>
                {isSubmitting ? "Guardando..." : isEditing ? "Guardar cambios" : "Crear servicio"}
              </Button>
            </DialogFooter>
          </form>
        </Form>
      </DialogContent>
    </Dialog>
  )
}
