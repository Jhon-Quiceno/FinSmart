import { apiClient } from "../api-client"
import type {
  AiProviderStatus,
  AiUsage,
  CategorizeRequest,
  CategorizeResponse,
  ChatMessage,
  ChatReply,
  Insight,
} from "../types/ai"
import type { PaginatedResponse } from "../types/pagination"

export interface ChatHistoryFilters {
  page?: number
  size?: number
  sort?: string
}

export async function chat(message: string): Promise<ChatReply> {
  const response = await apiClient.post<ChatReply>("/api/ai/chat", { message })
  return response.data
}

export async function getChatHistory(
  filters: ChatHistoryFilters = {},
): Promise<PaginatedResponse<ChatMessage>> {
  const response = await apiClient.get<PaginatedResponse<ChatMessage>>("/api/ai/chat/history", {
    params: filters,
  })

  return response.data
}

export async function getLatestInsight(): Promise<Insight | null> {
  const response = await apiClient.get<Insight>("/api/ai/insights")
  if (response.status === 204) {
    return null
  }

  return response.data
}

export async function generateInsight(): Promise<Insight> {
  const response = await apiClient.post<Insight>("/api/ai/insights/generate")
  return response.data
}

export async function categorize(payload: CategorizeRequest): Promise<CategorizeResponse> {
  const response = await apiClient.post<CategorizeResponse>("/api/ai/categorize", payload)
  return response.data
}

export async function getUsage(): Promise<AiUsage> {
  const response = await apiClient.get<AiUsage>("/api/ai/chat/usage")
  return response.data
}

export async function getProvidersStatus(): Promise<AiProviderStatus[]> {
  const response = await apiClient.get<AiProviderStatus[]>("/api/ai/providers/status")
  return response.data
}
