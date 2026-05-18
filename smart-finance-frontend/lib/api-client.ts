import axios, { AxiosRequestConfig, InternalAxiosRequestConfig } from "axios"

interface ApiErrorResponse {
  message?: string
}

interface RetryAxiosRequestConfig extends AxiosRequestConfig {
  _retry?: boolean
  _skipAuthRefresh?: boolean
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

function clearClientSession() {
  if (typeof window === "undefined") return
  window.localStorage.removeItem("financeai_user")
}

function redirectToLogin() {
  if (typeof window === "undefined") return
  if (window.location.pathname !== "/login") {
    window.location.href = "/login"
  }
}

function showForbiddenToast() {
  if (typeof window === "undefined") return
  void import("sonner").then(({ toast }) => {
    toast.error("No tenes permiso para realizar esta accion")
  })
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

    if (error.response?.status === 403) {
      showForbiddenToast()
      return Promise.reject(error)
    }

    if (error.response?.status !== 401 || originalRequest._retry || originalRequest._skipAuthRefresh || isAuthEndpoint) {
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
      clearClientSession()
      redirectToLogin()
      return Promise.reject(refreshError)
    }
  },
)

export function setAccessToken(token: string | null): void {
  accessToken = token
}

export function clearAccessToken(): void {
  accessToken = null
}

export function getApiErrorMessage(error: unknown, fallbackMessage: string): string {
  if (axios.isAxiosError(error)) {
    if (!error.response) {
      return "No hay conexion con el backend. Verifica que la API este corriendo y accesible."
    }

    const data = error.response.data as ApiErrorResponse | undefined
    return data?.message ?? fallbackMessage
  }

  return fallbackMessage
}

async function refreshAccessToken(): Promise<string> {
  if (!refreshPromise) {
    refreshPromise = apiClient
      .post<{ accessToken: string }>("/api/users/refresh", undefined, { _skipAuthRefresh: true } as RetryAxiosRequestConfig)
      .then((response) => {
        setAccessToken(response.data.accessToken)
        return response.data.accessToken
      })
      .finally(() => {
        refreshPromise = null
      })
  }

  return refreshPromise
}
