import { z } from "zod"
import { CARD_FRANCHISES } from "../types/credit-card"

const optionalText = (maxLength: number) =>
  z.preprocess(
    (value) => {
      if (typeof value !== "string") return value
      const trimmed = value.trim()
      return trimmed.length > 0 ? trimmed : undefined
    },
    z.string().max(maxLength, `Maximo ${maxLength} caracteres`).optional(),
  )

const dayOfMonth = z.coerce
  .number()
  .int("El dia debe ser un numero entero")
  .min(1, "El dia debe estar entre 1 y 31")
  .max(31, "El dia debe estar entre 1 y 31")

export const creditCardSchema = z.object({
  name: z.string().trim().min(1, "El nombre es obligatorio").max(150, "Maximo 150 caracteres"),
  bank: optionalText(100),
  franchise: z.enum(CARD_FRANCHISES, { message: "La franquicia es obligatoria" }),
  creditLimit: z.coerce.number().gt(0, "El cupo debe ser mayor a 0"),
  monthlyRate: z.coerce.number().min(0, "La tasa mensual no puede ser negativa"),
  cutoffDay: dayOfMonth,
  paymentDueDay: dayOfMonth,
})

// Mirrors CreditCardUpdateRequest on the backend: franchise and creditLimit are fixed at
// creation, so editing excludes both (see lib/types/credit-card.ts).
export const creditCardEditSchema = creditCardSchema.omit({ franchise: true, creditLimit: true })

export type CreditCardFormValues = z.infer<typeof creditCardSchema>
export type CreditCardEditFormValues = z.infer<typeof creditCardEditSchema>
