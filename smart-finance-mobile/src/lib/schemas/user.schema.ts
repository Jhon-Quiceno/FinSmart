import { z } from "zod"

export const profileSchema = z.object({
  name: z.string().trim().min(1, "El nombre es obligatorio").max(120, "Maximo 120 caracteres"),
  email: z.string().trim().min(1, "El correo es obligatorio").email("Correo electronico invalido").max(180, "Maximo 180 caracteres"),
})

export type ProfileFormValues = z.infer<typeof profileSchema>

/**
 * Mirrors the backend's `ChangePasswordRequest` length rule (6-100 chars, same as registration —
 * see `UpdateProfileRequest`/`RegisterRequest` javadoc) and the frontend registration page's
 * enforced minimum (`app/registro/page.tsx`, `password.length < 6`).
 */
export const changePasswordSchema = z
  .object({
    currentPassword: z.string().min(1, "La contrasena actual es obligatoria"),
    newPassword: z
      .string()
      .min(6, "La nueva contrasena debe tener al menos 6 caracteres")
      .max(100, "Maximo 100 caracteres"),
    confirmPassword: z.string().min(1, "Confirma tu nueva contrasena"),
  })
  .refine((values) => values.newPassword === values.confirmPassword, {
    message: "Las contrasenas no coinciden",
    path: ["confirmPassword"],
  })

export type ChangePasswordFormValues = z.infer<typeof changePasswordSchema>
