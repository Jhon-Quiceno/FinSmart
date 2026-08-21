import { apiClient } from "../api-client"
import type { Notification, NotificationPreference, NotificationPreferenceRequest } from "../types/notification"
import type { PaginatedResponse } from "../types/pagination"

export interface NotificationFilters {
  page?: number
  size?: number
  sort?: string
}

export async function getNotifications(
  filters: NotificationFilters = {},
): Promise<PaginatedResponse<Notification>> {
  const response = await apiClient.get<PaginatedResponse<Notification>>("/api/notifications", {
    params: filters,
  })

  return response.data
}

export async function getUnreadCount(): Promise<number> {
  const response = await apiClient.get<number>("/api/notifications/unread-count")
  return response.data
}

export async function markAsRead(id: number): Promise<Notification> {
  const response = await apiClient.patch<Notification>(`/api/notifications/${id}/read`)
  return response.data
}

export async function markAllAsRead(): Promise<void> {
  await apiClient.patch("/api/notifications/read-all")
}

export async function getPreferences(): Promise<NotificationPreference> {
  const response = await apiClient.get<NotificationPreference>("/api/notifications/preferences")
  return response.data
}

export async function updatePreferences(
  payload: NotificationPreferenceRequest,
): Promise<NotificationPreference> {
  const response = await apiClient.put<NotificationPreference>("/api/notifications/preferences", payload)
  return response.data
}

export interface PushTokenRequest {
  expoPushToken: string
  deviceId: string
}

export async function registerPushToken(payload: PushTokenRequest): Promise<void> {
  await apiClient.post("/api/notifications/push-token", payload)
}

export async function unregisterPushToken(deviceId: string): Promise<void> {
  await apiClient.delete(`/api/notifications/push-token/${deviceId}`)
}
