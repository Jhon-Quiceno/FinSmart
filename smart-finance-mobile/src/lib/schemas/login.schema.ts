import { z } from 'zod';

/**
 * Misma regla que el login web (app/login/page.tsx: `password.length < 6`) y que
 * `changePasswordSchema` del frontend — mínimo 6 caracteres, igual que el backend.
 */
export const loginSchema = z.object({
  email: z.string().trim().min(1, 'El correo es obligatorio').email('Correo electrónico inválido'),
  password: z.string().min(6, 'La contraseña debe tener al menos 6 caracteres'),
  rememberMe: z.boolean(),
});

export type LoginFormValues = z.infer<typeof loginSchema>;
