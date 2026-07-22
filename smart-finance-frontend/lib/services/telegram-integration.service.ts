import { apiClient } from "../api-client"
import type { TelegramLinkCodeResponse, TelegramLinkStatusResponse } from "../types/telegram-integration"

export async function generateTelegramLinkCode(): Promise<TelegramLinkCodeResponse> {
  const response = await apiClient.post<TelegramLinkCodeResponse>("/api/integrations/telegram/link-code")
  return response.data
}

export async function getTelegramLinkStatus(): Promise<TelegramLinkStatusResponse> {
  const response = await apiClient.get<TelegramLinkStatusResponse>("/api/integrations/telegram/status")
  return response.data
}
