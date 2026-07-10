import type { AiMessageRole, ChatMessage } from "@/lib/types/ai"

export interface DisplayMessage {
  id: string
  role: AiMessageRole
  content: string
  providerName: string | null
  model: string | null
  createdAt: string
}

export function toDisplayMessage(message: ChatMessage): DisplayMessage {
  return {
    id: String(message.id),
    role: message.role,
    content: message.content,
    providerName: message.providerName,
    model: message.model,
    createdAt: message.createdAt,
  }
}
