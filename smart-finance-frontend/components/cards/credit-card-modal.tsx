"use client"

import { useEffect } from "react"
import { zodResolver } from "@hookform/resolvers/zod"
import { useForm, type Resolver } from "react-hook-form"
import { Button } from "@/components/ui/button"
import { Dialog, DialogContent, DialogDescription, DialogFooter, DialogHeader, DialogTitle } from "@/components/ui/dialog"
import { Form, FormControl, FormField, FormItem, FormLabel, FormMessage } from "@/components/ui/form"
import { Input } from "@/components/ui/input"
import { Select, SelectContent, SelectGroup, SelectItem, SelectTrigger, SelectValue } from "@/components/ui/select"
import { creditCardEditSchema, creditCardSchema } from "@/lib/schemas/credit-card.schema"
import type { CreditCardFormValues } from "@/lib/schemas/credit-card.schema"
import { CARD_FRANCHISES } from "@/lib/types/credit-card"
import type { CardFranchise, CreditCard } from "@/lib/types/credit-card"

interface CreditCardModalProps {
  open: boolean
  onOpenChange: (open: boolean) => void
  initialValue?: CreditCard | null
  isSubmitting: boolean
  onSubmit: (values: CreditCardFormValues) => Promise<boolean>
}

const franchiseLabels: Record<CardFranchise, string> = {
  VISA: "Visa",
  MASTERCARD: "Mastercard",
  AMEX: "Amex",
  DINERS: "Diners",
}

const getDefaultValues = (card?: CreditCard | null): CreditCardFormValues => ({
  name: card?.name ?? "",
  bank: card?.bank ?? "",
  franchise: card?.franchise ?? "VISA",
  creditLimit: card?.creditLimit ?? 0,
  monthlyRate: card?.monthlyRate ?? 0,
  cutoffDay: card?.cutoffDay ?? 1,
  paymentDueDay: card?.paymentDueDay ?? 1,
})

export function CreditCardModal({ open, onOpenChange, initialValue, isSubmitting, onSubmit }: CreditCardModalProps) {
  const isEditing = !!initialValue

  // Edit mode validates against creditCardEditSchema (franchise/creditLimit omitted, since both
  // are fixed at creation); create mode validates the full creditCardSchema.
  const resolver = (
    isEditing ? zodResolver(creditCardEditSchema) : zodResolver(creditCardSchema)
  ) as unknown as Resolver<CreditCardFormValues>

  const form = useForm<CreditCardFormValues>({
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
          <DialogTitle>{isEditing ? "Editar tarjeta" : "Crear tarjeta"}</DialogTitle>
          <DialogDescription>
            Completa la informacion para registrar la tarjeta de credito.
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
                    <Input placeholder="Ej: Visa Platinum" {...field} />
                  </FormControl>
                  <FormMessage />
                </FormItem>
              )}
            />

            <FormField
              control={form.control}
              name="bank"
              render={({ field }) => (
                <FormItem>
                  <FormLabel>Banco (opcional)</FormLabel>
                  <FormControl>
                    <Input placeholder="Ej: BBVA" {...field} value={field.value ?? ""} />
                  </FormControl>
                  <FormMessage />
                </FormItem>
              )}
            />

            <FormField
              control={form.control}
              name="franchise"
              render={({ field }) => (
                <FormItem>
                  <FormLabel>Franquicia</FormLabel>
                  <Select value={field.value} onValueChange={field.onChange} disabled={isEditing}>
                    <FormControl>
                      <SelectTrigger>
                        <SelectValue placeholder="Selecciona una franquicia" />
                      </SelectTrigger>
                    </FormControl>
                    <SelectContent>
                      <SelectGroup>
                        {CARD_FRANCHISES.map((franchise) => (
                          <SelectItem key={franchise} value={franchise}>
                            {franchiseLabels[franchise]}
                          </SelectItem>
                        ))}
                      </SelectGroup>
                    </SelectContent>
                  </Select>
                  {isEditing && (
                    <p className="text-xs text-muted-foreground">La franquicia no se puede editar despues de creada.</p>
                  )}
                  <FormMessage />
                </FormItem>
              )}
            />

            <FormField
              control={form.control}
              name="creditLimit"
              render={({ field }) => (
                <FormItem>
                  <FormLabel>Cupo</FormLabel>
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
                    <p className="text-xs text-muted-foreground">El cupo no se puede editar despues de creada.</p>
                  )}
                  <FormMessage />
                </FormItem>
              )}
            />

            <FormField
              control={form.control}
              name="monthlyRate"
              render={({ field }) => (
                <FormItem>
                  <FormLabel>Tasa mensual (%)</FormLabel>
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
              name="cutoffDay"
              render={({ field }) => (
                <FormItem>
                  <FormLabel>Dia de corte</FormLabel>
                  <FormControl>
                    <Input
                      type="number"
                      min="1"
                      max="31"
                      step="1"
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
              name="paymentDueDay"
              render={({ field }) => (
                <FormItem>
                  <FormLabel>Dia limite de pago</FormLabel>
                  <FormControl>
                    <Input
                      type="number"
                      min="1"
                      max="31"
                      step="1"
                      {...field}
                      onChange={(event) => field.onChange(event.target.value)}
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
                {isSubmitting ? "Guardando..." : isEditing ? "Guardar cambios" : "Crear tarjeta"}
              </Button>
            </DialogFooter>
          </form>
        </Form>
      </DialogContent>
    </Dialog>
  )
}
