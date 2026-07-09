"use client"

import { useEffect } from "react"
import { zodResolver } from "@hookform/resolvers/zod"
import { useForm } from "react-hook-form"
import { z } from "zod"
import { CategorySelect } from "@/components/shared/category-select"
import { Button } from "@/components/ui/button"
import { Dialog, DialogContent, DialogDescription, DialogFooter, DialogHeader, DialogTitle } from "@/components/ui/dialog"
import { Form, FormControl, FormField, FormItem, FormLabel, FormMessage } from "@/components/ui/form"
import { Input } from "@/components/ui/input"
import { getTodayDateInput } from "@/lib/date"
import { incomeSchema } from "@/lib/schemas/income.schema"
import type { Income } from "@/lib/types/income"

type IncomeFormValues = z.infer<typeof incomeSchema>

interface IncomeModalProps {
  open: boolean
  onOpenChange: (open: boolean) => void
  initialValue?: Income | null
  isSubmitting: boolean
  onSubmit: (values: IncomeFormValues) => Promise<void>
}

const getDefaultValues = (income?: Income | null): IncomeFormValues => ({
  amount: income?.amount ?? 0,
  description: income?.description ?? "",
  date: income?.date ?? getTodayDateInput(),
  categoryId: income?.categoryId ?? null,
})

export function IncomeModal({ open, onOpenChange, initialValue, isSubmitting, onSubmit }: IncomeModalProps) {
  const form = useForm<IncomeFormValues>({
    resolver: zodResolver(incomeSchema),
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
          <DialogTitle>{initialValue ? "Editar ingreso" : "Crear ingreso"}</DialogTitle>
          <DialogDescription>
            Completa la información para registrar el ingreso en tu historial.
          </DialogDescription>
        </DialogHeader>

        <Form {...form}>
          <form onSubmit={handleSubmit} className="flex flex-col gap-4">
            <FormField
              control={form.control}
              name="description"
              render={({ field }) => (
                <FormItem>
                  <FormLabel>Descripción</FormLabel>
                  <FormControl>
                    <Input placeholder="Ej: Salario mensual" {...field} value={field.value ?? ""} />
                  </FormControl>
                  <FormMessage />
                </FormItem>
              )}
            />

            <div className="grid grid-cols-1 gap-4 sm:grid-cols-2">
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
                    <FormLabel>Fecha</FormLabel>
                    <FormControl>
                      <Input type="date" {...field} />
                    </FormControl>
                    <FormMessage />
                  </FormItem>
                )}
              />
            </div>

            <FormField
              control={form.control}
              name="categoryId"
              render={({ field }) => (
                <FormItem>
                  <FormLabel>Categoría</FormLabel>
                  <FormControl>
                    <CategorySelect
                      type="INCOME"
                      value={field.value === null ? "none" : String(field.value)}
                      onValueChange={(value) => field.onChange(value === "none" ? null : Number(value))}
                    />
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
                {isSubmitting ? "Guardando..." : initialValue ? "Guardar cambios" : "Crear ingreso"}
              </Button>
            </DialogFooter>
          </form>
        </Form>
      </DialogContent>
    </Dialog>
  )
}
