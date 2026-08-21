import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';

import {
  createCard,
  deleteCard,
  getCard,
  getCards,
  updateCard,
  type CreditCardFilters,
} from '@/lib/services/credit-card.service';
import type { CreditCardCreateRequest, CreditCardUpdateRequest } from '@/lib/types/credit-card';

export const creditCardsQueryKey = (filters: CreditCardFilters = {}) => ['credit-cards', filters] as const;
export const creditCardQueryKey = (id: number) => ['credit-cards', 'detail', id] as const;

export function useCreditCards(filters: CreditCardFilters = {}) {
  return useQuery({
    queryKey: creditCardsQueryKey(filters),
    queryFn: () => getCards(filters),
  });
}

export function useCreditCard(id: number) {
  return useQuery({
    queryKey: creditCardQueryKey(id),
    queryFn: () => getCard(id),
  });
}

export function useCreateCreditCard() {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: (payload: CreditCardCreateRequest) => createCard(payload),
    onSuccess: () => {
      void queryClient.invalidateQueries({ queryKey: ['credit-cards'] });
    },
  });
}

export function useUpdateCreditCard() {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: ({ id, payload }: { id: number; payload: CreditCardUpdateRequest }) => updateCard(id, payload),
    onSuccess: () => {
      void queryClient.invalidateQueries({ queryKey: ['credit-cards'] });
    },
  });
}

export function useDeleteCreditCard() {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: (id: number) => deleteCard(id),
    onSuccess: () => {
      void queryClient.invalidateQueries({ queryKey: ['credit-cards'] });
    },
  });
}
