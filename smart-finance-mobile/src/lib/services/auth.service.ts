import { apiClient } from '@/lib/api-client';
import {
  clearAccessToken,
  clearRefreshToken,
  getRefreshToken,
  saveRefreshToken,
  setAccessToken,
  setPersistSession,
} from '@/lib/session';
import type { ApiAuthResponse } from '@/lib/types/auth';

async function persistSession(response: ApiAuthResponse): Promise<void> {
  setAccessToken(response.accessToken);
  if (response.refreshToken) {
    await saveRefreshToken(response.refreshToken);
  }
}

export async function registerRequest(name: string, email: string, password: string): Promise<ApiAuthResponse> {
  const response = await apiClient.post<ApiAuthResponse>('/api/users/register', { name, email, password });
  await persistSession(response.data);
  return response.data;
}

export async function loginRequest(email: string, password: string, rememberMe: boolean): Promise<ApiAuthResponse> {
  // rememberMe siempre se manda como true al backend para que RefreshTokenService.rememberMe
  // se propague en cada rotacion; en el cliente, rememberMe controla si el refresh token se
  // persiste en SecureStore o solo vive en memoria (ver setPersistSession en session.ts).
  setPersistSession(rememberMe);
  const response = await apiClient.post<ApiAuthResponse>('/api/users/login', { email, password, rememberMe: true });
  await persistSession(response.data);
  return response.data;
}

/**
 * A diferencia del interceptor de api-client.ts (que solo se dispara reactivamente ante un 401),
 * esta funcion es de uso explicito — por ejemplo un bootstrap de AuthProvider que intenta
 * renovar la sesion al arrancar la app. Por eso lee el refresh token guardado y lo manda en el
 * body ella misma, igual que hace refreshAccessToken() internamente en api-client.ts.
 */
export async function refreshRequest(): Promise<ApiAuthResponse> {
  const storedRefreshToken = await getRefreshToken();
  if (!storedRefreshToken) throw new Error('NO_REFRESH_TOKEN');

  const response = await apiClient.post<ApiAuthResponse>(
    '/api/users/refresh',
    { refreshToken: storedRefreshToken },
    { _skipAuthRefresh: true } as never,
  );
  await persistSession(response.data);
  return response.data;
}

export async function logoutRequest(): Promise<void> {
  try {
    await apiClient.post('/api/users/logout');
  } finally {
    clearAccessToken();
    await clearRefreshToken();
  }
}
