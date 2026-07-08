import axios, { AxiosError, AxiosRequestConfig, InternalAxiosRequestConfig } from "axios"

interface ApiErrorResponse {
  message?: string
}

interface RetryAxiosRequestConfig extends AxiosRequestConfig {
  _retry?: boolean
  _csrfRetry?: boolean
  _skipAuthRefresh?: boolean
}

let accessToken: string | null = null
let refreshPromise: Promise<string> | null = null
let csrfPromise: Promise<void> | null = null

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

function isMutatingMethod(method?: string): boolean {
  if (!method) return false
  return ["POST", "PUT", "PATCH", "DELETE"].includes(method.toUpperCase())
}

function isCsrfError(error: AxiosError): boolean {
  if (error.response?.status !== 403) return false

  const message = String((error.response.data as ApiErrorResponse | undefined)?.message ?? "").toLowerCase()
  return message.includes("csrf")
}

async function ensureCsrfToken(): Promise<void> {
  if (getXsrfToken()) return

  if (!csrfPromise) {
    csrfPromise = apiClient
      .get("/api/users/csrf", { _skipAuthRefresh: true } as RetryAxiosRequestConfig)
      .then(() => undefined)
      .finally(() => {
        csrfPromise = null
      })
  }

  await csrfPromise
}

async function setAuthorizationAndCsrfHeaders(config: InternalAxiosRequestConfig): Promise<InternalAxiosRequestConfig> {
  if (accessToken) {
    config.headers.Authorization = `Bearer ${accessToken}`
  } else {
    delete config.headers.Authorization
  }

  if (isMutatingMethod(config.method) && !getXsrfToken()) {
    await ensureCsrfToken()
  }

  if (isMutatingMethod(config.method)) {
    const xsrfToken = getXsrfToken()
    if (xsrfToken) {
      config.headers["X-XSRF-TOKEN"] = xsrfToken
    }
  }

  return config
}

/**
 * Clears the client-side session snapshot on a hard auth failure (refresh failed, or a 403 that
 * really means "not authenticated"). This intentionally does NOT call `AuthContext.clearSession()`
 * — an axios interceptor runs outside React's render tree, so there is no `AuthProvider` instance
 * to call into here. Instead it reproduces the same two effects `clearSession()` has (drop the
 * `financeai_user` localStorage key, clear the in-memory access token) directly, and the caller
 * always follows up with `redirectToLogin()`, which does a full `window.location.href` navigation
 * — that reload remounts `AuthProvider` from scratch, so React state staying stale in the old page
 * is a non-issue.
 */
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

function shouldTreatForbiddenAsUnauthenticated(error: AxiosError): boolean {
  if (error.response?.status !== 403) return false

  const message = String((error.response.data as ApiErrorResponse | undefined)?.message ?? "").toLowerCase()
  return message.includes("no autenticado") || message.includes("autenticado invalido") || message.includes("autenticado inválido")
}

function showForbiddenToast() {
  if (typeof window === "undefined") return
  void import("sonner").then(({ toast }) => {
    toast.error("No tenes permiso para realizar esta accion")
  })
}

/**
 * The AI endpoints (chat, insights, categorize) intentionally surface their own specific error
 * messages at the call site (e.g. `ai-insights-card.tsx` distinguishes a 422 "no provider
 * configured" from a generic failure, and both `chat`/`generateInsight` propagate a 503 "assistant
 * unavailable" for the caller to render). Showing the generic server-error toast for those
 * requests too would double up on toasts, so 5xx responses from `/api/ai/*` are excluded here.
 */
function isAiEndpoint(url: string): boolean {
  return url.includes("/api/ai/")
}

function showServerErrorToast() {
  if (typeof window === "undefined") return
  void import("sonner").then(({ toast }) => {
    toast.error("Ocurrió un error en el servidor. Intentá de nuevo en unos minutos.")
  })
}

apiClient.interceptors.request.use(async (config) => setAuthorizationAndCsrfHeaders(config))

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
      requestUrl.includes("/api/users/logout") ||
      requestUrl.includes("/api/users/password")
    const status = error.response?.status

    if (isCsrfError(error) && !originalRequest._csrfRetry && isMutatingMethod(originalRequest.method)) {
      originalRequest._csrfRetry = true
      await ensureCsrfToken()
      return await apiClient(originalRequest)
    }

    const canAttemptAuthRecovery =
      (status === 401 || status === 403) &&
      !originalRequest._retry &&
      !originalRequest._skipAuthRefresh &&
      !isAuthEndpoint

    if (canAttemptAuthRecovery) {
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
    }

    if (shouldTreatForbiddenAsUnauthenticated(error)) {
      clearAccessToken()
      clearClientSession()
      redirectToLogin()
      return Promise.reject(error)
    }

    if (status === 403) {
      showForbiddenToast()
    }

    if (status !== undefined && status >= 500 && !isAiEndpoint(requestUrl)) {
      showServerErrorToast()
    }

    return Promise.reject(error)
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

/**
 * Toasts `error` with `fallbackMessage`, except for 5xx responses: those already get the generic
 * server-error toast from the response interceptor above, so toasting here too would stack two
 * toasts for the same failure.
 */
export function toastApiError(error: unknown, fallbackMessage: string): void {
  const status = axios.isAxiosError(error) ? error.response?.status : undefined
  if (status !== undefined && status >= 500) {
    return
  }

  void import("sonner").then(({ toast }) => {
    toast.error(getApiErrorMessage(error, fallbackMessage))
  })
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
