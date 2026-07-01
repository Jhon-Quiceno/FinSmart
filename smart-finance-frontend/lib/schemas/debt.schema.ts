import { z } from "zod"
import { isValidDateInput } from "../date"

const optionalDate = z.preprocess(
  (value) => {
    if (typeof value !== "string") return value
    const trimmed = value.trim()
    return trimmed.length > 0 ? trimmed : undefined
  },
  z
    .string()
    .refine((value) => isValidDateInput(value), "Fecha invalida")
    .optional(),
)

const optionalNonNegativeNumber = z.preprocess(
  (value) => {
    if (value === null || value === undefined || value === "") return undefined
    return value
  },
  z.coerce.number().min(0, "El valor no puede ser negativo").optional(),
)

export const debtSchema = z.object({
  name: z.string().trim().min(1, "El nombre es obligatorio").max(100, "Maximo 100 caracteres"),
  totalAmount: z.coerce.number().gt(0, "El monto total debe ser mayor a 0"),
  interestRate: optionalNonNegativeNumber,
  dueDate: optionalDate,
})

export const debtEditSchema = debtSchema.omit({ totalAmount: true })

export type DebtFormValues = z.infer<typeof debtSchema>
export type DebtEditFormValues = z.infer<typeof debtEditSchema>
