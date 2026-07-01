import { z } from "zod"
import { isValidDateInput } from "../date"
import { RECURRING_FREQUENCIES } from "../types/recurring-payment"

export const recurringPaymentSchema = z.object({
  name: z.string().trim().min(1, "El nombre es obligatorio").max(100, "Maximo 100 caracteres"),
  amount: z.coerce.number().gt(0, "El monto debe ser mayor a 0"),
  frequency: z.enum(RECURRING_FREQUENCIES, { message: "La frecuencia es obligatoria" }),
  firstPaymentDate: z
    .string()
    .min(1, "La fecha es obligatoria")
    .refine((value) => isValidDateInput(value), "Fecha invalida"),
})

export const recurringPaymentEditSchema = recurringPaymentSchema.omit({ firstPaymentDate: true })

export type RecurringPaymentFormValues = z.infer<typeof recurringPaymentSchema>
export type RecurringPaymentEditFormValues = z.infer<typeof recurringPaymentEditSchema>
