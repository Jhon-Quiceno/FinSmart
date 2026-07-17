import { z } from "zod"
import { isFutureDateInput, isValidDateInput } from "../date"

const optionalText = (maxLength: number) =>
  z.preprocess(
    (value) => {
      if (typeof value !== "string") return value
      const trimmed = value.trim()
      return trimmed.length > 0 ? trimmed : undefined
    },
    z.string().max(maxLength, `Maximo ${maxLength} caracteres`).optional(),
  )

// Optional: an omitted date defaults to today on the backend (mirrors CardPurchaseRequest /
// CardPaymentRequest doc), but a provided one must still be a valid, non-future date.
const optionalDate = z.preprocess(
  (value) => {
    if (typeof value !== "string") return value
    const trimmed = value.trim()
    return trimmed.length > 0 ? trimmed : undefined
  },
  z
    .string()
    .refine((value) => isValidDateInput(value), "Fecha invalida")
    .refine((value) => !isFutureDateInput(value), "La fecha no puede ser futura")
    .optional(),
)

// Card purchase form: omitted installmentCount means a simple (1-cuota) purchase; when the user
// opts into deferred installments the count must be at least 2 (see CardPurchaseRequest doc —
// null/1 already means "simple purchase", so the installments picker only makes sense from 2 up).
const optionalInstallmentCount = z.preprocess(
  (value) => {
    if (value === null || value === undefined || value === "") return undefined
    return value
  },
  z.coerce
    .number()
    .int("La cantidad de cuotas debe ser un numero entero")
    .min(2, "La cantidad de cuotas debe ser al menos 2")
    .max(48, "La cantidad de cuotas no puede superar 48")
    .optional(),
)

export const cardPurchaseSchema = z.object({
  amount: z.coerce.number().gt(0, "El monto debe ser mayor a 0"),
  date: optionalDate,
  description: optionalText(255),
  installmentCount: optionalInstallmentCount,
})

export const cardPaymentSchema = z.object({
  amount: z.coerce.number().gt(0, "El monto debe ser mayor a 0"),
  date: optionalDate,
  description: optionalText(255),
})

export type CardPurchaseFormValues = z.infer<typeof cardPurchaseSchema>
export type CardPaymentFormValues = z.infer<typeof cardPaymentSchema>
