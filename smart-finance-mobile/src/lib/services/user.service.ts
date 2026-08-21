import { apiClient } from "../api-client"
import type { ApiUserResponse } from "../types/auth"

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
