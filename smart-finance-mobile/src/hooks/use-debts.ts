import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';

import { createDebt, deleteDebt, getDebts, updateDebt, type DebtFilters } from '@/lib/services/debt.service';
import type { DebtCreateRequest, DebtUpdateRequest } from '@/lib/types/debt';

export const debtsQueryKey = (filters: DebtFilters = {}) => ['debts', filters] as const;

export function useDebts(filters: DebtFilters = {}) {
  return useQuery({
    queryKey: debtsQueryKey(filters),
    queryFn: () => getDebts(filters),
  });
}

export function useCreateDebt() {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: (payload: DebtCreateRequest) => createDebt(payload),
    onSuccess: () => {
      void queryClient.invalidateQueries({ queryKey: ['debts'] });
    },
  });
}

export function useUpdateDebt() {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: ({ id, payload }: { id: number; payload: DebtUpdateRequest }) => updateDebt(id, payload),
    onSuccess: () => {
      void queryClient.invalidateQueries({ queryKey: ['debts'] });
    },
  });
}

export function useDeleteDebt() {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: (id: number) => deleteDebt(id),
    onSuccess: () => {
      void queryClient.invalidateQueries({ queryKey: ['debts'] });
    },
  });
}
