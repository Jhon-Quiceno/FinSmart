import { z } from "zod"
import { isValidDateInput } from "../date"

const optionalText = (maxLength: number) =>
  z.preprocess(
    (value) => {
      if (typeof value !== "string") return value
      const trimmed = value.trim()
      return trimmed.length > 0 ? trimmed : undefined
    },
    z.string().max(maxLength, `Maximo ${maxLength} caracteres`).optional(),
  )

export const debtPaymentSchema = z.object({
  amount: z.coerce.number().gt(0, "El monto debe ser mayor a 0"),
  paymentDate: z
    .string()
    .min(1, "La fecha es obligatoria")
    .refine((value) => isValidDateInput(value), "Fecha invalida"),
  note: optionalText(255),
})

export type DebtPaymentFormValues = z.infer<typeof debtPaymentSchema>
