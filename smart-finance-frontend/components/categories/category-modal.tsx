"use client"

import { useEffect } from "react"
import { zodResolver } from "@hookform/resolvers/zod"
import { useForm } from "react-hook-form"
import { Button } from "@/components/ui/button"
import { Dialog, DialogContent, DialogDescription, DialogFooter, DialogHeader, DialogTitle } from "@/components/ui/dialog"
import { Form, FormControl, FormField, FormItem, FormLabel, FormMessage } from "@/components/ui/form"
import { Input } from "@/components/ui/input"
import { Select, SelectContent, SelectGroup, SelectItem, SelectTrigger, SelectValue } from "@/components/ui/select"
import { categorySchema } from "@/lib/schemas/category.schema"
import type { CategoryFormValues } from "@/lib/schemas/category.schema"
import type { Category, CategoryType } from "@/lib/types/category"

const categoryTypeOptions: { value: CategoryType; label: string }[] = [
  { value: "INCOME", label: "Ingreso" },
  { value: "EXPENSE", label: "Gasto" },
]

interface CategoryModalProps {
  open: boolean
  onOpenChange: (open: boolean) => void
  initialValue?: Category | null
  defaultType?: CategoryType
  isSubmitting: boolean
  onSubmit: (values: CategoryFormValues) => Promise<void>
}

const getDefaultValues = (category?: Category | null, defaultType?: CategoryType): CategoryFormValues => ({
  name: category?.name ?? "",
  type: category?.type ?? defaultType ?? "EXPENSE",
})

export function CategoryModal({
  open,
  onOpenChange,
  initialValue,
  defaultType,
  isSubmitting,
  onSubmit,
}: CategoryModalProps) {
  const form = useForm<CategoryFormValues>({
    resolver: zodResolver(categorySchema),
    mode: "onSubmit",
    reValidateMode: "onBlur",
    defaultValues: getDefaultValues(initialValue, defaultType),
  })

  useEffect(() => {
    if (open) {
      form.reset(getDefaultValues(initialValue, defaultType))
    }
  }, [form, initialValue, defaultType, open])

  const handleSubmit = form.handleSubmit(async (values) => {
    await onSubmit(values)
    form.reset(getDefaultValues(null, defaultType))
  })

  return (
    <Dialog open={open} onOpenChange={onOpenChange}>
      <DialogContent>
        <DialogHeader>
          <DialogTitle>{initialValue ? "Editar categoria" : "Crear categoria"}</DialogTitle>
          <DialogDescription>
            {initialValue
              ? "Actualiza el nombre o el tipo de la categoria."
              : "Completa la informacion para crear una nueva categoria."}
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
                    <Input placeholder="Ej: Salario" {...field} />
                  </FormControl>
                  <FormMessage />
                </FormItem>
              )}
            />

            <FormField
              control={form.control}
              name="type"
              render={({ field }) => (
                <FormItem>
                  <FormLabel>Tipo</FormLabel>
                  <Select value={field.value} onValueChange={field.onChange}>
                    <FormControl>
                      <SelectTrigger>
                        <SelectValue placeholder="Selecciona un tipo" />
                      </SelectTrigger>
                    </FormControl>
                    <SelectContent>
                      <SelectGroup>
                        {categoryTypeOptions.map((option) => (
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

            <DialogFooter>
              <Button type="button" variant="outline" onClick={() => onOpenChange(false)}>
                Cancelar
              </Button>
              <Button type="submit" disabled={isSubmitting}>
                {isSubmitting ? "Guardando..." : initialValue ? "Guardar cambios" : "Crear categoria"}
              </Button>
            </DialogFooter>
          </form>
        </Form>
      </DialogContent>
    </Dialog>
  )
}
