import { z } from 'zod';

// Misma regla de longitud de contraseña que el backend (6-100 caracteres) — ver
// smart-finance-frontend/lib/schemas/user.schema.ts (changePasswordSchema).
export const registerSchema = z
  .object({
    name: z.string().trim().min(1, 'El nombre es obligatorio').max(120, 'Máximo 120 caracteres'),
    email: z.string().trim().min(1, 'El correo es obligatorio').email('Correo electrónico inválido'),
    password: z.string().min(6, 'La contraseña debe tener al menos 6 caracteres').max(100, 'Máximo 100 caracteres'),
    confirmPassword: z.string().min(1, 'Confirmá tu contraseña'),
  })
  .refine((values) => values.password === values.confirmPassword, {
    message: 'Las contraseñas no coinciden',
    path: ['confirmPassword'],
  });

export type RegisterFormValues = z.infer<typeof registerSchema>;
