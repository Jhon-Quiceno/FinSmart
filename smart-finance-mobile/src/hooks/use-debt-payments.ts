import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';

import {
  createDebtPayment,
  getDebtPayments,
  type DebtPaymentFilters,
} from '@/lib/services/debt-payment.service';
import type { DebtPaymentRequest } from '@/lib/types/debt-payment';

export const debtPaymentsQueryKey = (debtId: number, filters: DebtPaymentFilters = {}) =>
  ['debt-payments', debtId, filters] as const;

export function useDebtPayments(debtId: number, filters: DebtPaymentFilters = {}) {
  return useQuery({
    queryKey: debtPaymentsQueryKey(debtId, filters),
    queryFn: () => getDebtPayments(debtId, filters),
  });
}

export function useCreateDebtPayment(debtId: number) {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: (payload: DebtPaymentRequest) => createDebtPayment(debtId, payload),
    onSuccess: () => {
      // Un abono cambia el remainingAmount de la deuda: invalidar tanto la lista de pagos
      // de esta deuda como la lista de deudas (que muestra ese remainingAmount).
      void queryClient.invalidateQueries({ queryKey: ['debt-payments', debtId] });
      void queryClient.invalidateQueries({ queryKey: ['debts'] });
    },
  });
}
