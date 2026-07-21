import { apiClient } from "../api-client"
import type { TelegramLinkCodeResponse } from "../types/telegram-integration"

export async function generateTelegramLinkCode(): Promise<TelegramLinkCodeResponse> {
  const response = await apiClient.post<TelegramLinkCodeResponse>("/api/integrations/telegram/link-code")
  return response.data
}
