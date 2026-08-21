import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';

import {
  getMovements,
  registerPayment,
  registerPurchase,
  type CardMovementFilters,
} from '@/lib/services/card-movement.service';
import type { CardPaymentRequest, CardPurchaseRequest } from '@/lib/types/card-movement';

export const cardMovementsQueryKey = (cardId: number, filters: CardMovementFilters = {}) =>
  ['card-movements', cardId, filters] as const;

export function useCardMovements(cardId: number, filters: CardMovementFilters = {}) {
  return useQuery({
    queryKey: cardMovementsQueryKey(cardId, filters),
    queryFn: () => getMovements(cardId, filters),
  });
}

export function useCreateCardPurchase(cardId: number) {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: (payload: CardPurchaseRequest) => registerPurchase(cardId, payload),
    onSuccess: () => {
      // Una compra cambia currentBalance/availableCredit de la tarjeta: invalidar tanto sus
      // movimientos como la lista de tarjetas.
      void queryClient.invalidateQueries({ queryKey: ['card-movements', cardId] });
      void queryClient.invalidateQueries({ queryKey: ['credit-cards'] });
    },
  });
}

export function useCreateCardPayment(cardId: number) {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: (payload: CardPaymentRequest) => registerPayment(cardId, payload),
    onSuccess: () => {
      void queryClient.invalidateQueries({ queryKey: ['card-movements', cardId] });
      void queryClient.invalidateQueries({ queryKey: ['credit-cards'] });
    },
  });
}
