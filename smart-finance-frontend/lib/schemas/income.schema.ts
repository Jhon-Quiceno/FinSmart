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

const dateSchema = z
  .string()
  .min(1, "La fecha es obligatoria")
  .refine((value) => isValidDateInput(value), "Fecha invalida")
  .refine((value) => !isFutureDateInput(value), "La fecha no puede ser futura")

export const incomeSchema = z.object({
  amount: z.coerce.number().gt(0, "El monto debe ser mayor a 0"),
  description: optionalText(255),
  date: dateSchema,
  categoryId: z.preprocess(
    (value) => {
      if (value === null || value === undefined || value === "" || value === "none") return null
      const parsed = Number(value)
      return Number.isNaN(parsed) ? value : parsed
    },
    z.number().int().positive().nullable(),
  ),
})

export type IncomeFormValues = z.infer<typeof incomeSchema>
