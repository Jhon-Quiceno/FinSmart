import { createContext, useContext, useEffect, useState, type ReactNode } from 'react';

import { getApiErrorMessage, setSessionExpiredListener } from '@/lib/api-client';
import { getDeviceId } from '@/lib/device-id';
import { registerPushTokenForCurrentUser } from '@/lib/push';
import { queryClient } from '@/lib/query-client';
import { loginRequest, logoutRequest, refreshRequest, registerRequest } from '@/lib/services/auth.service';
import { unregisterPushToken } from '@/lib/services/notification.service';
import { clearSessionStorage, getRefreshToken } from '@/lib/session';
import type { ApiUserResponse } from '@/lib/types/auth';

export type SessionStatus = 'bootstrapping' | 'authenticated' | 'unauthenticated';

export type AuthUser = ApiUserResponse;

interface AuthActionResult {
  success: boolean;
  error?: string;
}

interface AuthContextValue {
  status: SessionStatus;
  user: AuthUser | null;
  login: (email: string, password: string, rememberMe: boolean) => Promise<AuthActionResult>;
  register: (name: string, email: string, password: string) => Promise<AuthActionResult>;
  logout: () => Promise<void>;
  updateUser: (user: AuthUser) => void;
}

const AuthContext = createContext<AuthContextValue | null>(null);

export function AuthProvider({ children }: { children: ReactNode }) {
  const [status, setStatus] = useState<SessionStatus>('bootstrapping');
  const [user, setUser] = useState<AuthUser | null>(null);

  // Bootstrap: si no hay refresh token guardado, no vale la pena pegarle al backend — evita un
  // round-trip inutil y un colgado de hasta 15s en la primera instalacion si el backend esta caido.
  useEffect(() => {
    let cancelled = false;

    async function bootstrap() {
      const storedRefreshToken = await getRefreshToken();
      if (!storedRefreshToken) {
        if (!cancelled) setStatus('unauthenticated');
        return;
      }

      try {
        const response = await refreshRequest();
        if (cancelled) return;
        setUser(response.user);
        setStatus('authenticated');
      } catch {
        if (cancelled) return;
        await clearSessionStorage();
        setStatus('unauthenticated');
      }
    }

    void bootstrap();

    return () => {
      cancelled = true;
    };
  }, []);

  // El interceptor de api-client.ts no puede navegar (corre fuera del arbol de React): avisa
  // por este listener y aca lo traducimos a estado de React + limpieza de cache de queries.
  useEffect(() => {
    setSessionExpiredListener(() => {
      setUser(null);
      setStatus('unauthenticated');
      queryClient.clear();
    });

    return () => {
      setSessionExpiredListener(null);
    };
  }, []);

  async function login(email: string, password: string, rememberMe: boolean): Promise<AuthActionResult> {
    try {
      const response = await loginRequest(email, password, rememberMe);
      setUser(response.user);
      setStatus('authenticated');
      // Fire-and-forget: nunca debe bloquear ni fallar el login.
      void registerPushTokenForCurrentUser();
      return { success: true };
    } catch (error) {
      return { success: false, error: getApiErrorMessage(error, 'No se pudo iniciar sesión.') };
    }
  }

  async function register(name: string, email: string, password: string): Promise<AuthActionResult> {
    try {
      const response = await registerRequest(name, email, password);
      setUser(response.user);
      setStatus('authenticated');
      void registerPushTokenForCurrentUser();
      return { success: true };
    } catch (error) {
      return { success: false, error: getApiErrorMessage(error, 'No se pudo crear la cuenta.') };
    }
  }

  async function logout(): Promise<void> {
    // El orden importa: (1) y (2) son best-effort y no deben bloquear el logout local ante un
    // backend caido o inalcanzable; (3) cancela queries en vuelo ANTES de limpiar el cache — una
    // respuesta tardia de un request de la sesion anterior no debe repoblar el cache ya limpio.
    try {
      await unregisterPushToken(await getDeviceId());
    } catch {
      // Idempotente en el backend: no pasa nada si falla o si el token ya no existe.
    }

    try {
      await logoutRequest();
    } catch {
      // El logout local no depende de que el servidor sea alcanzable.
    }

    await queryClient.cancelQueries();
    queryClient.clear();

    await clearSessionStorage();
    setUser(null);
    setStatus('unauthenticated');
  }

  // Actualiza el usuario en memoria tras una edición exitosa (ej. PUT /api/users/profile) sin
  // pasar por otro round-trip de refresh — el backend ya devuelve el usuario actualizado.
  function updateUser(updatedUser: AuthUser): void {
    setUser(updatedUser);
  }

  return (
    <AuthContext.Provider value={{ status, user, login, register, logout, updateUser }}>
      {children}
    </AuthContext.Provider>
  );
}

export function useAuth(): AuthContextValue {
  const context = useContext(AuthContext);
  if (!context) {
    throw new Error('useAuth debe usarse dentro de un AuthProvider');
  }
  return context;
}
