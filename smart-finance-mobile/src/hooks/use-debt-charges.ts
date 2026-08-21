import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';

import {
  createDebtCharge,
  getDebtCharges,
  type DebtChargeFilters,
} from '@/lib/services/debt-charge.service';
import type { Debt } from '@/lib/types/debt';
import type { DebtChargeRequest } from '@/lib/types/debt-charge';
import type { PaginatedResponse } from '@/lib/types/pagination';

export const debtChargesQueryKey = (debtId: number, filters: DebtChargeFilters = {}) =>
  ['debt-charges', debtId, filters] as const;

export function useDebtCharges(debtId: number, filters: DebtChargeFilters = {}) {
  return useQuery({
    queryKey: debtChargesQueryKey(debtId, filters),
    queryFn: () => getDebtCharges(debtId, filters),
  });
}

export function useCreateDebtCharge(debtId: number) {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: (payload: DebtChargeRequest) => createDebtCharge(debtId, payload),
    onSuccess: (updatedDebt: Debt) => {
      // El backend devuelve la Debt actualizada (nuevo remainingAmount) en vez del DebtCharge
      // creado — se actualiza el cache de todas las listas de deudas al toque (mejor UX que
      // esperar un refetch) y además se invalida por si hay una página/filtro no cacheado acá.
      queryClient.setQueriesData<PaginatedResponse<Debt>>({ queryKey: ['debts'] }, (previous) => {
        if (!previous) return previous;
        return {
          ...previous,
          content: previous.content.map((debt) => (debt.id === updatedDebt.id ? updatedDebt : debt)),
        };
      });
      void queryClient.invalidateQueries({ queryKey: ['debts'] });
      void queryClient.invalidateQueries({ queryKey: ['debt-charges', debtId] });
    },
  });
}
