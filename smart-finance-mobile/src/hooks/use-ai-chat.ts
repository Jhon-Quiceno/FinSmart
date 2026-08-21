import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import axios from 'axios';

import { getApiErrorMessage } from '@/lib/api-client';
import { chat, getChatHistory, type ChatHistoryFilters } from '@/lib/services/ai.service';
import type { ChatMessage } from '@/lib/types/ai';
import type { PaginatedResponse } from '@/lib/types/pagination';

export const chatHistoryQueryKey = (filters: ChatHistoryFilters = {}) => ['ai-chat-history', filters] as const;

export function useChatHistory(filters: ChatHistoryFilters = {}) {
  return useQuery({
    queryKey: chatHistoryQueryKey(filters),
    queryFn: () => getChatHistory(filters),
  });
}

/**
 * El backend de `/api/ai/chat` puede devolver 429 (RateLimitFilter, por IP+usuario) además de
 * los errores genéricos — este mensaje es específico y legible en vez del fallback genérico de
 * `getApiErrorMessage`.
 */
export function getChatSendErrorMessage(error: unknown): string {
  if (axios.isAxiosError(error) && error.response?.status === 429) {
    return 'Estás enviando mensajes muy rápido, esperá un momento.';
  }
  return getApiErrorMessage(error, 'No se pudo enviar el mensaje.');
}

/**
 * En éxito, agrega el turno (mensaje del usuario + respuesta del asistente) directo al cache de
 * `useChatHistory` con `setQueriesData` en vez de esperar un refetch completo — el chat se siente
 * instantáneo. El historial del backend viene ordenado `createdAt DESC` (más nuevo primero, ver
 * `AiChatController#getHistory`), por eso se hace *unshift* y no *push*.
 */
export function useSendChatMessage() {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: (message: string) => chat(message),
    onSuccess: (reply, message) => {
      const now = new Date().toISOString();
      const userMessage: ChatMessage = {
        id: -Date.now(),
        role: 'USER',
        content: message,
        providerName: null,
        model: null,
        createdAt: now,
      };
      const assistantMessage: ChatMessage = {
        id: -Date.now() - 1,
        role: 'ASSISTANT',
        content: reply.reply,
        providerName: reply.providerName,
        model: reply.model,
        createdAt: reply.createdAt,
      };

      queryClient.setQueriesData<PaginatedResponse<ChatMessage>>(
        { queryKey: ['ai-chat-history'] },
        (old) => {
          if (!old) {
            return {
              content: [assistantMessage, userMessage],
              number: 0,
              size: 20,
              totalElements: 2,
              totalPages: 1,
            };
          }
          return {
            ...old,
            content: [assistantMessage, userMessage, ...old.content],
            totalElements: old.totalElements + 2,
          };
        },
      );
    },
  });
}
