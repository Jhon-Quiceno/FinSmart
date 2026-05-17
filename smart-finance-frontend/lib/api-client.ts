import axios, { AxiosRequestConfig, InternalAxiosRequestConfig } from "axios"

export interface ApiUser {
  id: number
  name: string
  email: string
}

export interface ApiAuthResponse {
  accessToken: string
  tokenType: string
  expiresIn: number
  user: ApiUser
}

interface ApiErrorResponse {
  message?: string
}

interface RetryAxiosRequestConfig extends AxiosRequestConfig {
  _retry?: boolean
}

let accessToken: string | null = null
let refreshPromise: Promise<string> | null = null

export const apiClient = axios.create({
  baseURL: process.env.NEXT_PUBLIC_API_URL ?? "http://localhost:8080",
  withCredentials: true,
  headers: {
    "Content-Type": "application/json",
  },
})

/**
 * Reads the XSRF-TOKEN cookie set by Spring Security's CookieCsrfTokenRepository
 * and attaches it as X-XSRF-TOKEN header on every mutating request (POST/PUT/PATCH/DELETE).
 * This protects against CSRF attacks on cookie-based endpoints like /api/users/refresh.
 */
function getXsrfToken(): string | null {
  if (typeof document === "undefined") return null
  const match = document.cookie.match(/(?:^|;\s*)XSRF-TOKEN=([^;]*)/)
  return match ? decodeURIComponent(match[1]) : null
}

function setAuthorizationAndCsrfHeaders(config: InternalAxiosRequestConfig): InternalAxiosRequestConfig {
  if (accessToken) {
    config.headers.Authorization = `Bearer ${accessToken}`
  } else {
    delete config.headers.Authorization
  }

  const method = config.method?.toUpperCase()
  if (method && ["POST", "PUT", "PATCH", "DELETE"].includes(method)) {
    const xsrfToken = getXsrfToken()
    if (xsrfToken) {
      config.headers["X-XSRF-TOKEN"] = xsrfToken
    }
  }

  return config
}

apiClient.interceptors.request.use((config) => setAuthorizationAndCsrfHeaders(config))

apiClient.interceptors.response.use(
  (response) => response,
  async (error) => {
    if (!axios.isAxiosError(error) || !error.config) {
      return Promise.reject(error)
    }

    const originalRequest = error.config as RetryAxiosRequestConfig
    const requestUrl = originalRequest.url ?? ""
    const isAuthEndpoint =
      requestUrl.includes("/api/users/login") ||
      requestUrl.includes("/api/users/register") ||
      requestUrl.includes("/api/users/refresh") ||
      requestUrl.includes("/api/users/logout")

    if (error.response?.status !== 401 || originalRequest._retry || isAuthEndpoint) {
      return Promise.reject(error)
    }

    originalRequest._retry = true

    try {
      const token = await refreshAccessToken()
      if (!originalRequest.headers) {
        originalRequest.headers = {}
      }
      originalRequest.headers.Authorization = `Bearer ${token}`
      return await apiClient(originalRequest)
    } catch (refreshError) {
      clearAccessToken()
      return Promise.reject(refreshError)
    }
  }
)

export async function registerRequest(name: string, email: string, password: string): Promise<ApiAuthResponse> {
  const response = await apiClient.post<ApiAuthResponse>("/api/users/register", {
    name,
    email,
    password,
  })
  setAccessToken(response.data.accessToken)
  return response.data
}

export async function loginRequest(email: string, password: string): Promise<ApiAuthResponse> {
  const response = await apiClient.post<ApiAuthResponse>("/api/users/login", {
    email,
    password,
  })
  setAccessToken(response.data.accessToken)
  return response.data
}

export async function refreshRequest(): Promise<ApiAuthResponse> {
  const response = await apiClient.post<ApiAuthResponse>("/api/users/refresh")
  setAccessToken(response.data.accessToken)
  return response.data
}

export async function logoutRequest(): Promise<void> {
  await apiClient.post("/api/users/logout")
  clearAccessToken()
}

export function setAccessToken(token: string | null): void {
  accessToken = token
}

export function clearAccessToken(): void {
  accessToken = null
}

export function getApiErrorMessage(error: unknown, fallbackMessage: string): string {
  if (axios.isAxiosError(error)) {
    if (!error.response) {
      return "No hay conexión con el backend. Verifica que la API esté corriendo y accesible."
    }
    const data = error.response?.data as ApiErrorResponse | undefined
    return data?.message ?? fallbackMessage
  }

  return fallbackMessage
}

async function refreshAccessToken(): Promise<string> {
  if (!refreshPromise) {
    refreshPromise = refreshRequest()
      .then((response) => response.accessToken)
      .finally(() => {
        refreshPromise = null
      })
  }

  return refreshPromise
}
