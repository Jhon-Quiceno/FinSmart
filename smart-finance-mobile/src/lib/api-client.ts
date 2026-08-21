import axios, { type AxiosRequestConfig, type InternalAxiosRequestConfig } from 'axios';

import { API_BASE_URL } from '@/lib/env';
import {
  clearAccessToken,
  clearRefreshToken,
  getAccessToken,
  getRefreshToken,
  saveRefreshToken,
  setAccessToken,
} from '@/lib/session';
import type { ApiAuthResponse, ApiErrorResponse } from '@/lib/types/auth';

interface RetryAxiosRequestConfig extends AxiosRequestConfig {
  _retry?: boolean;
  _skipAuthRefresh?: boolean;
}

export const apiClient = axios.create({
  baseURL: API_BASE_URL,
  // RN no tiene timeout por defecto: sin esto, con el backend caido la app queda colgada.
  timeout: 15_000,
  headers: {
    'Content-Type': 'application/json',
    'X-Client': 'mobile',
  },
});

type SessionExpiredListener = () => void;
let sessionExpiredListener: SessionExpiredListener | null = null;

/**
 * El interceptor corre fuera del arbol de React: no puede navegar de forma segura. En vez de
 * eso avisa por aca; el AuthProvider (trabajo separado, no tuyo) traduce esto a estado de React
 * y la navegacion sale declarativa desde ahi.
 */
export function setSessionExpiredListener(listener: SessionExpiredListener | null): void {
  sessionExpiredListener = listener;
}

apiClient.interceptors.request.use((config: InternalAxiosRequestConfig) => {
  const token = getAccessToken();
  if (token) {
    config.headers.Authorization = `Bearer ${token}`;
  } else {
    delete config.headers.Authorization;
  }
  return config;
});

const AUTH_ENDPOINTS = [
  '/api/users/login',
  '/api/users/register',
  '/api/users/refresh',
  '/api/users/logout',
  '/api/users/password',
];

apiClient.interceptors.response.use(
  (response) => response,
  async (error: unknown) => {
    if (!axios.isAxiosError(error) || !error.config) return Promise.reject(error);

    const originalRequest = error.config as InternalAxiosRequestConfig & RetryAxiosRequestConfig;
    const status = error.response?.status;
    const url = originalRequest.url ?? '';
    const isAuthEndpoint = AUTH_ENDPOINTS.some((path) => url.includes(path));

    const canAttemptAuthRecovery =
      (status === 401 || status === 403) &&
      !originalRequest._retry &&
      !originalRequest._skipAuthRefresh &&
      !isAuthEndpoint;

    if (!canAttemptAuthRecovery) return Promise.reject(error);

    originalRequest._retry = true;
    try {
      const token = await refreshAccessToken();
      originalRequest.headers.Authorization = `Bearer ${token}`;
      return await apiClient(originalRequest);
    } catch (refreshError) {
      await handleHardAuthFailure();
      return Promise.reject(refreshError);
    }
  },
);

let refreshPromise: Promise<string> | null = null;

async function refreshAccessToken(): Promise<string> {
  if (!refreshPromise) {
    refreshPromise = (async () => {
      const storedRefreshToken = await getRefreshToken();
      if (!storedRefreshToken) throw new Error('NO_REFRESH_TOKEN');

      const response = await apiClient.post<ApiAuthResponse>(
        '/api/users/refresh',
        { refreshToken: storedRefreshToken },
        { _skipAuthRefresh: true } as RetryAxiosRequestConfig,
      );

      setAccessToken(response.data.accessToken);
      // El backend ROTA el refresh token en cada llamada (revoca el anterior). Si no se guarda
      // el nuevo, el siguiente refresh falla y el usuario queda deslogueado sin motivo aparente.
      if (response.data.refreshToken) {
        await saveRefreshToken(response.data.refreshToken);
      }
      return response.data.accessToken;
    })().finally(() => {
      refreshPromise = null;
    });
  }
  return refreshPromise;
}

async function handleHardAuthFailure(): Promise<void> {
  clearAccessToken();
  await clearRefreshToken();
  sessionExpiredListener?.();
}

export function getApiErrorMessage(error: unknown, fallbackMessage: string): string {
  if (axios.isAxiosError(error)) {
    if (!error.response) {
      return 'No hay conexión con el servidor. Verificá que el backend esté corriendo y que la app apunte a la IP LAN de la PC (no a localhost).';
    }
    return (error.response.data as ApiErrorResponse | undefined)?.message ?? fallbackMessage;
  }
  return fallbackMessage;
}
