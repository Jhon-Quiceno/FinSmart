import axios from "axios"

export interface ApiUser {
  id: number
  name: string
  email: string
}

export interface ApiAuthResponse {
  token: string
  user: ApiUser
}

interface ApiErrorResponse {
  message?: string
}

export const apiClient = axios.create({
  baseURL: process.env.NEXT_PUBLIC_API_URL ?? "http://localhost:8080",
  headers: {
    "Content-Type": "application/json",
  },
})

apiClient.interceptors.request.use((config) => {
  if (typeof window !== "undefined") {
    const token = window.localStorage.getItem("financeai_token")
    if (token) {
      config.headers.Authorization = `Bearer ${token}`
    }
  }

  return config
})

export async function registerRequest(name: string, email: string, password: string): Promise<ApiAuthResponse> {
  const response = await apiClient.post<ApiAuthResponse>("/api/users/register", {
    name,
    email,
    password,
  })
  return response.data
}

export async function loginRequest(email: string, password: string): Promise<ApiAuthResponse> {
  const response = await apiClient.post<ApiAuthResponse>("/api/users/login", {
    email,
    password,
  })
  return response.data
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
