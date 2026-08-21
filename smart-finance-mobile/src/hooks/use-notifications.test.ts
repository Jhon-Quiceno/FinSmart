import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import { renderHook, waitFor } from '@testing-library/react-native';
import type { ReactNode } from 'react';
import React from 'react';

import { getNotifications, getUnreadCount, markAsRead } from '@/lib/services/notification.service';
import type { Notification } from '@/lib/types/notification';
import type { PaginatedResponse } from '@/lib/types/pagination';

import { useMarkAsRead, useNotifications, useUnreadCount } from './use-notifications';

jest.mock('@/lib/services/notification.service', () => ({
  getNotifications: jest.fn(),
  getUnreadCount: jest.fn(),
  markAsRead: jest.fn(),
  markAllAsRead: jest.fn(),
  getPreferences: jest.fn(),
  updatePreferences: jest.fn(),
}));

const mockedGetNotifications = getNotifications as jest.MockedFunction<typeof getNotifications>;
const mockedGetUnreadCount = getUnreadCount as jest.MockedFunction<typeof getUnreadCount>;
const mockedMarkAsRead = markAsRead as jest.MockedFunction<typeof markAsRead>;

function emptyPage(): PaginatedResponse<Notification> {
  return { content: [], number: 0, size: 20, totalElements: 0, totalPages: 0 };
}

function createWrapper() {
  const queryClient = new QueryClient({
    defaultOptions: { queries: { retry: false }, mutations: { retry: false } },
  });
  function Wrapper({ children }: { children: ReactNode }) {
    return React.createElement(QueryClientProvider, { client: queryClient }, children);
  }
  return Wrapper;
}

describe('useMarkAsRead', () => {
  beforeEach(() => {
    mockedGetNotifications.mockReset();
    mockedGetUnreadCount.mockReset();
    mockedMarkAsRead.mockReset();
  });

  it('invalida tanto el feed de notificaciones como el contador de no leidas', async () => {
    mockedGetNotifications.mockResolvedValue(emptyPage());
    mockedGetUnreadCount.mockResolvedValue(2);

    const notification: Notification = {
      id: 1,
      type: 'OVERSPEND_ALERT',
      title: 'Presupuesto excedido',
      message: 'Superaste tu límite en Servicios',
      read: true,
      readAt: '2026-08-20T12:00:00.000Z',
      createdAt: '2026-08-20T10:00:00.000Z',
    };
    mockedMarkAsRead.mockResolvedValue(notification);

    const wrapper = createWrapper();
    const { result: feedResult } = await renderHook(() => useNotifications(), { wrapper });
    const { result: countResult } = await renderHook(() => useUnreadCount(), { wrapper });

    await waitFor(() => expect(feedResult.current.isSuccess).toBe(true));
    await waitFor(() => expect(countResult.current.isSuccess).toBe(true));
    expect(mockedGetNotifications).toHaveBeenCalledTimes(1);
    expect(mockedGetUnreadCount).toHaveBeenCalledTimes(1);

    const { result: mutationResult } = await renderHook(() => useMarkAsRead(), { wrapper });
    await mutationResult.current.mutateAsync(1);

    expect(mockedMarkAsRead).toHaveBeenCalledWith(1);
    await waitFor(() => expect(mockedGetNotifications).toHaveBeenCalledTimes(2));
    await waitFor(() => expect(mockedGetUnreadCount).toHaveBeenCalledTimes(2));
  });
});
