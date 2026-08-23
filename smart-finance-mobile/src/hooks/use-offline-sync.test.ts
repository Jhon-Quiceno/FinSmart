jest.mock('@react-native-community/netinfo', () => ({
  __esModule: true,
  default: { addEventListener: jest.fn(() => jest.fn()) },
}));

jest.mock('@/lib/offline-queue', () => ({
  getQueuedMovements: jest.fn(),
  removeQueuedMovement: jest.fn(),
}));

jest.mock('@/lib/services/expense.service', () => ({ createExpense: jest.fn() }));
jest.mock('@/lib/services/income.service', () => ({ createIncome: jest.fn() }));
jest.mock('@/lib/session', () => ({ getCurrentUserId: jest.fn() }));

import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import { renderHook, waitFor } from '@testing-library/react-native';
import type { ReactNode } from 'react';
import React from 'react';

import { getQueuedMovements, removeQueuedMovement } from '@/lib/offline-queue';
import { createExpense } from '@/lib/services/expense.service';
import { createIncome } from '@/lib/services/income.service';
import { getCurrentUserId } from '@/lib/session';
import type { QueuedMovement } from '@/lib/offline-queue';

import { useOfflineSync } from './use-offline-sync';

const mockedGetQueuedMovements = getQueuedMovements as jest.MockedFunction<typeof getQueuedMovements>;
const mockedRemoveQueuedMovement = removeQueuedMovement as jest.MockedFunction<typeof removeQueuedMovement>;
const mockedCreateExpense = createExpense as jest.MockedFunction<typeof createExpense>;
const mockedCreateIncome = createIncome as jest.MockedFunction<typeof createIncome>;
const mockedGetCurrentUserId = getCurrentUserId as jest.MockedFunction<typeof getCurrentUserId>;

function movement(id: number, kind: 'EXPENSE' | 'INCOME' = 'EXPENSE'): QueuedMovement {
  return {
    id,
    kind,
    payload: { amount: 100, date: '2026-08-20', paymentMethod: 'CASH' } as never,
    createdAt: '2026-08-20T10:00:00.000Z',
  };
}

function wrapper() {
  const queryClient = new QueryClient({ defaultOptions: { queries: { retry: false }, mutations: { retry: false } } });
  return function Wrapper({ children }: { children: ReactNode }) {
    return React.createElement(QueryClientProvider, { client: queryClient }, children);
  };
}

describe('useOfflineSync', () => {
  beforeEach(() => {
    jest.clearAllMocks();
    mockedGetCurrentUserId.mockReturnValue(1);
    mockedRemoveQueuedMovement.mockResolvedValue(undefined);
  });

  it('does nothing when disabled', async () => {
    renderHook(() => useOfflineSync(false), { wrapper: wrapper() });
    await waitFor(() => expect(mockedGetQueuedMovements).not.toHaveBeenCalled());
  });

  it('does nothing when there is no identifiable user', async () => {
    mockedGetCurrentUserId.mockReturnValue(null);
    renderHook(() => useOfflineSync(true), { wrapper: wrapper() });
    await waitFor(() => expect(mockedGetQueuedMovements).not.toHaveBeenCalled());
  });

  it('syncs a queued expense and removes it once created', async () => {
    mockedGetQueuedMovements.mockResolvedValue([movement(1)]);
    mockedCreateExpense.mockResolvedValue({} as never);

    renderHook(() => useOfflineSync(true), { wrapper: wrapper() });

    await waitFor(() => expect(mockedRemoveQueuedMovement).toHaveBeenCalledWith(1));
    expect(mockedCreateExpense).toHaveBeenCalledTimes(1);
  });

  it('stops draining without dropping the item on a connectivity failure', async () => {
    mockedGetQueuedMovements.mockResolvedValue([movement(1), movement(2)]);
    const connectivityError = { isAxiosError: true, response: undefined };
    Object.setPrototypeOf(connectivityError, Error.prototype);
    mockedCreateExpense.mockRejectedValue(connectivityError);

    renderHook(() => useOfflineSync(true), { wrapper: wrapper() });

    await waitFor(() => expect(mockedCreateExpense).toHaveBeenCalledTimes(1));
    expect(mockedRemoveQueuedMovement).not.toHaveBeenCalled();
  });

  it('discards a permanently-rejected item and continues with the rest of the queue', async () => {
    mockedGetQueuedMovements.mockResolvedValue([movement(1), movement(2)]);
    const serverRejection = { isAxiosError: true, response: { status: 404 } };
    Object.setPrototypeOf(serverRejection, Error.prototype);
    mockedCreateExpense.mockRejectedValueOnce(serverRejection).mockResolvedValueOnce({} as never);

    renderHook(() => useOfflineSync(true), { wrapper: wrapper() });

    await waitFor(() => expect(mockedRemoveQueuedMovement).toHaveBeenCalledTimes(2));
    expect(mockedRemoveQueuedMovement).toHaveBeenCalledWith(1);
    expect(mockedRemoveQueuedMovement).toHaveBeenCalledWith(2);
    expect(mockedCreateExpense).toHaveBeenCalledTimes(2);
  });

  it('routes an INCOME movement to createIncome, not createExpense', async () => {
    mockedGetQueuedMovements.mockResolvedValue([movement(1, 'INCOME')]);
    mockedCreateIncome.mockResolvedValue({} as never);

    renderHook(() => useOfflineSync(true), { wrapper: wrapper() });

    await waitFor(() => expect(mockedRemoveQueuedMovement).toHaveBeenCalledWith(1));
    expect(mockedCreateIncome).toHaveBeenCalledTimes(1);
    expect(mockedCreateExpense).not.toHaveBeenCalled();
  });
});
