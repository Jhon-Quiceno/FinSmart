import { z } from "zod"
import { CATEGORY_TYPES } from "../types/category"

export const categorySchema = z.object({
  name: z.string().trim().min(1, "El nombre es obligatorio").max(100, "Maximo 100 caracteres"),
  type: z.enum(CATEGORY_TYPES),
})

export type CategoryFormValues = z.infer<typeof categorySchema>
