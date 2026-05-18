"use client"

import { useEffect } from "react"
import { zodResolver } from "@hookform/resolvers/zod"
import { useForm } from "react-hook-form"
import { z } from "zod"
import { CategorySelect } from "@/components/shared/category-select"
import { Button } from "@/components/ui/button"
import { Checkbox } from "@/components/ui/checkbox"
import { Dialog, DialogContent, DialogDescription, DialogFooter, DialogHeader, DialogTitle } from "@/components/ui/dialog"
import { Form, FormControl, FormField, FormItem, FormLabel, FormMessage } from "@/components/ui/form"
import { Input } from "@/components/ui/input"
import { Select, SelectContent, SelectGroup, SelectItem, SelectTrigger, SelectValue } from "@/components/ui/select"
import { expenseSchema } from "@/lib/schemas/expense.schema"
import type { Expense } from "@/lib/types/expense"

const paymentMethods = ["Efectivo", "Tarjeta de Debito", "Tarjeta de Credito", "Transferencia"]

type ExpenseFormValues = z.infer<typeof expenseSchema>

interface ExpenseModalProps {
  open: boolean
  onOpenChange: (open: boolean) => void
  initialValue?: Expense | null
  isSubmitting: boolean
  onSubmit: (values: ExpenseFormValues) => Promise<void>
}

const getDefaultValues = (expense?: Expense | null): ExpenseFormValues => ({
  amount: expense?.amount ?? 0,
  description: expense?.description ?? "",
  date: expense?.date ?? new Date().toISOString().slice(0, 10),
  isRecurring: expense?.isRecurring ?? false,
  paymentMethod: expense?.paymentMethod ?? "Efectivo",
  categoryId: expense?.categoryId ?? null,
})

export function ExpenseModal({ open, onOpenChange, initialValue, isSubmitting, onSubmit }: ExpenseModalProps) {
  const form = useForm<ExpenseFormValues>({
    resolver: zodResolver(expenseSchema),
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
          <DialogTitle>{initialValue ? "Editar gasto" : "Crear gasto"}</DialogTitle>
          <DialogDescription>
            Completa la informacion para registrar el gasto en tu historial.
          </DialogDescription>
        </DialogHeader>

        <Form {...form}>
          <form onSubmit={handleSubmit} className="flex flex-col gap-4">
            <FormField
              control={form.control}
              name="description"
              render={({ field }) => (
                <FormItem>
                  <FormLabel>Descripcion</FormLabel>
                  <FormControl>
                    <Input placeholder="Ej: Mercado semanal" {...field} value={field.value ?? ""} />
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
              name="categoryId"
              render={({ field }) => (
                <FormItem>
                  <FormLabel>Categoria</FormLabel>
                  <FormControl>
                    <CategorySelect
                      type="EXPENSE"
                      value={field.value === null ? "none" : String(field.value)}
                      onValueChange={(value) => field.onChange(value === "none" ? null : Number(value))}
                    />
                  </FormControl>
                  <FormMessage />
                </FormItem>
              )}
            />

            <FormField
              control={form.control}
              name="paymentMethod"
              render={({ field }) => (
                <FormItem>
                  <FormLabel>Metodo de pago</FormLabel>
                  <Select value={field.value} onValueChange={field.onChange}>
                    <FormControl>
                      <SelectTrigger>
                        <SelectValue placeholder="Selecciona un metodo" />
                      </SelectTrigger>
                    </FormControl>
                    <SelectContent>
                      <SelectGroup>
                        {paymentMethods.map((method) => (
                          <SelectItem key={method} value={method}>
                            {method}
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
              name="date"
              render={({ field }) => (
                <FormItem>
                  <FormLabel>Fecha</FormLabel>
                  <FormControl>
                    <Input type="date" {...field} />
                  </FormControl>
                  <FormMessage />
                </FormItem>
              )}
            />

            <FormField
              control={form.control}
              name="isRecurring"
              render={({ field }) => (
                <FormItem className="flex flex-row items-center gap-2">
                  <FormControl>
                    <Checkbox checked={field.value} onCheckedChange={(checked) => field.onChange(Boolean(checked))} />
                  </FormControl>
                  <FormLabel>Es un gasto recurrente</FormLabel>
                </FormItem>
              )}
            />

            <DialogFooter>
              <Button type="button" variant="outline" onClick={() => onOpenChange(false)}>
                Cancelar
              </Button>
              <Button type="submit" disabled={isSubmitting}>
                {isSubmitting ? "Guardando..." : initialValue ? "Guardar cambios" : "Crear gasto"}
              </Button>
            </DialogFooter>
          </form>
        </Form>
      </DialogContent>
    </Dialog>
  )
}
