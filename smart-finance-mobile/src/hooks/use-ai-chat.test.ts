import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import { renderHook, waitFor } from '@testing-library/react-native';
import { AxiosError } from 'axios';
import type { ReactNode } from 'react';
import React from 'react';

import { chat, getChatHistory } from '@/lib/services/ai.service';
import type { ChatMessage, ChatReply } from '@/lib/types/ai';
import type { PaginatedResponse } from '@/lib/types/pagination';

import { chatHistoryQueryKey, getChatSendErrorMessage, useChatHistory, useSendChatMessage } from './use-ai-chat';

jest.mock('@/lib/services/ai.service', () => ({
  chat: jest.fn(),
  getChatHistory: jest.fn(),
}));

const mockedChat = chat as jest.MockedFunction<typeof chat>;
const mockedGetChatHistory = getChatHistory as jest.MockedFunction<typeof getChatHistory>;

function emptyHistory(): PaginatedResponse<ChatMessage> {
  return { content: [], number: 0, size: 20, totalElements: 0, totalPages: 0 };
}

function createWrapper() {
  const queryClient = new QueryClient({
    defaultOptions: { queries: { retry: false }, mutations: { retry: false } },
  });
  function Wrapper({ children }: { children: ReactNode }) {
    return React.createElement(QueryClientProvider, { client: queryClient }, children);
  }
  return { Wrapper, queryClient };
}

describe('useChatHistory + useSendChatMessage', () => {
  beforeEach(() => {
    mockedChat.mockReset();
    mockedGetChatHistory.mockReset();
  });

  it('agrega el mensaje del usuario y la respuesta al cache de historial sin refetch', async () => {
    mockedGetChatHistory.mockResolvedValue(emptyHistory());

    const reply: ChatReply = {
      reply: 'Llevás $820.000 en Mercado este mes.',
      providerName: 'openai',
      model: 'gpt-4o-mini',
      createdAt: '2026-08-20T12:00:00.000Z',
    };
    mockedChat.mockResolvedValue(reply);

    const { Wrapper, queryClient } = createWrapper();
    const { result: historyResult } = await renderHook(() => useChatHistory(), { wrapper: Wrapper });

    await waitFor(() => expect(historyResult.current.isSuccess).toBe(true));
    expect(mockedGetChatHistory).toHaveBeenCalledTimes(1);

    const { result: mutationResult } = await renderHook(() => useSendChatMessage(), { wrapper: Wrapper });

    await mutationResult.current.mutateAsync('¿Cuánto gasté en mercado?');

    expect(mockedChat).toHaveBeenCalledWith('¿Cuánto gasté en mercado?');
    // El historial NO debe volver a pedirse al backend — se actualiza directo en cache.
    expect(mockedGetChatHistory).toHaveBeenCalledTimes(1);

    const cached = queryClient.getQueryData<PaginatedResponse<ChatMessage>>(chatHistoryQueryKey());
    expect(cached?.content).toHaveLength(2);
    expect(cached?.content[0].role).toBe('ASSISTANT');
    expect(cached?.content[0].content).toBe(reply.reply);
    expect(cached?.content[1].role).toBe('USER');
    expect(cached?.content[1].content).toBe('¿Cuánto gasté en mercado?');
  });
});

describe('getChatSendErrorMessage', () => {
  it('devuelve un mensaje especifico cuando el backend responde 429', () => {
    const error = new AxiosError('Too Many Requests', undefined, undefined, undefined, {
      status: 429,
      data: { message: 'Rate limit exceeded' },
      statusText: 'Too Many Requests',
      headers: {},
      config: {} as never,
    });

    expect(getChatSendErrorMessage(error)).toBe('Estás enviando mensajes muy rápido, esperá un momento.');
  });

  it('cae al mensaje generico para otros errores', () => {
    const error = new AxiosError('Network Error');
    expect(getChatSendErrorMessage(error)).toBe('No hay conexión con el servidor. Verificá que el backend esté corriendo y que la app apunte a la IP LAN de la PC (no a localhost).');
  });
});
