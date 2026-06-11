import { z } from "zod"
import { CATEGORY_TYPES } from "../types/category"

const optionalText = (maxLength: number) =>
  z.preprocess(
    (value) => {
      if (typeof value !== "string") return value
      const trimmed = value.trim()
      return trimmed.length > 0 ? trimmed : undefined
    },
    z.string().max(maxLength).optional(),
  )

export const categorySchema = z.object({
  name: z.string().trim().min(1, "El nombre es obligatorio").max(100, "Maximo 100 caracteres"),
  type: z.enum(CATEGORY_TYPES),
  icon: optionalText(50),
  color: z.preprocess(
    (value) => {
      if (typeof value !== "string") return value
      const trimmed = value.trim()
      return trimmed.length > 0 ? trimmed : undefined
    },
    z.string().regex(/^#[0-9A-Fa-f]{6}$/, "Color invalido").optional(),
  ),
})

export type CategoryFormValues = z.infer<typeof categorySchema>
