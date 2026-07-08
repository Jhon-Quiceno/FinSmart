import { apiClient, clearAccessToken, setAccessToken } from "../api-client"
import type { ApiAuthResponse } from "../types/auth"

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
