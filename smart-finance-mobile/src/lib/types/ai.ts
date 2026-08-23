export const AI_MESSAGE_ROLES = ["USER", "ASSISTANT"] as const

export type AiMessageRole = (typeof AI_MESSAGE_ROLES)[number]

export interface ChatMessage {
  id: number
  role: AiMessageRole
  content: string
  providerName: string | null
  model: string | null
  createdAt: string
}

export interface ChatRequest {
  message: string
}

export interface ChatReply {
  reply: string
  providerName: string
  model: string
  createdAt: string
}

export interface Insight {
  id: number
  content: string
  providerName: string
  model: string
  createdAt: string
}

export interface CategorizeRequest {
  description: string
  amount?: number
  type?: "INCOME" | "EXPENSE"
}

export interface CategorizeResponse {
  categoryId: number | null
  categoryName: string | null
}

export interface AiUsage {
  used: number
  limit: number
  remaining: number
  resetsAt: string
}

/** Espejo de AiProviderStatusResponse del backend — GET /api/ai/providers/status, solo lectura. */
export interface AiProviderStatus {
  name: string
  configured: boolean
  priority: number
}
