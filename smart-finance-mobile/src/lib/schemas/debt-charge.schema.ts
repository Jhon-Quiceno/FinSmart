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

export const debtChargeSchema = z.object({
  amount: z.coerce.number().gt(0, "El monto debe ser mayor a 0"),
  chargeDate: z
    .string()
    .min(1, "La fecha es obligatoria")
    .refine((value) => isValidDateInput(value), "Fecha invalida"),
  description: optionalText(255),
})

export type DebtChargeFormValues = z.infer<typeof debtChargeSchema>
