import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import { renderHook, waitFor } from '@testing-library/react-native';
import type { ReactNode } from 'react';
import React from 'react';

import { useCreateDebt, useDebts } from '@/hooks/use-debts';
import * as debtService from '@/lib/services/debt.service';
import type { Debt, DebtCreateRequest } from '@/lib/types/debt';
import type { PaginatedResponse } from '@/lib/types/pagination';

jest.mock('@/lib/services/debt.service');

const mockedDebtService = debtService as jest.Mocked<typeof debtService>;

function createWrapper() {
  const queryClient = new QueryClient({
    defaultOptions: { queries: { retry: false }, mutations: { retry: false } },
  });

  function Wrapper({ children }: { children: ReactNode }) {
    return React.createElement(QueryClientProvider, { client: queryClient }, children);
  }

  return { Wrapper, queryClient };
}

const debt: Debt = {
  id: 1,
  name: 'Préstamo libre inversión',
  totalAmount: 6_000_000,
  remainingAmount: 2_450_000,
  interestRate: 1.8,
  dueDate: '2027-02-01',
  createdAt: '2026-01-01T00:00:00Z',
  updatedAt: '2026-01-01T00:00:00Z',
};

const page: PaginatedResponse<Debt> = {
  content: [debt],
  number: 0,
  size: 10,
  totalElements: 1,
  totalPages: 1,
};

describe('useDebts / useCreateDebt', () => {
  beforeEach(() => {
    jest.resetAllMocks();
  });

  it('fetches the debts list', async () => {
    mockedDebtService.getDebts.mockResolvedValue(page);
    const { Wrapper } = createWrapper();

    const { result } = await renderHook(() => useDebts(), { wrapper: Wrapper });

    await waitFor(() => expect(result.current.isSuccess).toBe(true));

    expect(result.current.data).toEqual(page);
    expect(mockedDebtService.getDebts).toHaveBeenCalledWith({});
  });

  it('invalidates and refetches the debts list after creating a debt', async () => {
    mockedDebtService.getDebts.mockResolvedValue(page);
    const created: Debt = { ...debt, id: 2, name: 'Nueva deuda' };
    mockedDebtService.createDebt.mockResolvedValue(created);
    const { Wrapper } = createWrapper();

    const { result } = await renderHook(
      () => ({ list: useDebts(), create: useCreateDebt() }),
      { wrapper: Wrapper },
    );

    await waitFor(() => expect(result.current.list.isSuccess).toBe(true));
    expect(mockedDebtService.getDebts).toHaveBeenCalledTimes(1);

    const payload: DebtCreateRequest = { name: 'Nueva deuda', totalAmount: 1_000_000 };
    await result.current.create.mutateAsync(payload);

    await waitFor(() => expect(mockedDebtService.getDebts).toHaveBeenCalledTimes(2));
    expect(mockedDebtService.createDebt).toHaveBeenCalledWith(payload);
  });
});
