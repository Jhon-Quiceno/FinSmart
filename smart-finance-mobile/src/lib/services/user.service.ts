import { apiClient } from "../api-client"
import type { ApiUserResponse } from "../types/auth"
import type { UserPreferences } from "../types/preferences"

export interface UpdateProfileRequest {
  name: string
  email: string
}

export interface ChangePasswordRequest {
  currentPassword: string
  newPassword: string
}

export async function updateProfile(payload: UpdateProfileRequest): Promise<ApiUserResponse> {
  const response = await apiClient.put<ApiUserResponse>("/api/users/profile", payload)
  return response.data
}

export async function changePassword(payload: ChangePasswordRequest): Promise<void> {
  await apiClient.put("/api/users/password", payload)
}

export async function getUserPreferences(): Promise<UserPreferences> {
  const response = await apiClient.get<UserPreferences>("/api/users/preferences")
  return response.data
}

export async function updateUserPreferences(payload: UserPreferences): Promise<UserPreferences> {
  const response = await apiClient.patch<UserPreferences>("/api/users/preferences", payload)
  return response.data
}
