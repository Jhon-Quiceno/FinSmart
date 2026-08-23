import NetInfo from '@react-native-community/netinfo';
import { useQueryClient } from '@tanstack/react-query';
import { useEffect, useRef } from 'react';

import { getQueuedMovements, removeQueuedMovement } from '@/lib/offline-queue';
import { createExpense } from '@/lib/services/expense.service';
import { createIncome } from '@/lib/services/income.service';
import type { ExpenseRequest } from '@/lib/types/expense';
import type { IncomeRequest } from '@/lib/types/income';

import { EXPENSES_QUERY_KEY } from './use-expenses';
import { INCOMES_QUERY_KEY } from './use-incomes';

/**
 * Drena la cola offline (M5, docs/plan-sprints-movil-nativo.md) al recuperar conexión: reintenta
 * cada gasto/ingreso encolado en orden FIFO contra el backend real. Se detiene en el primer error
 * (probablemente seguimos sin conexión real, o el token expiró) en vez de reordenar la cola
 * saltando movimientos - el siguiente evento de reconexión retoma desde ahí.
 */
export function useOfflineSync(enabled: boolean): void {
  const queryClient = useQueryClient();
  const syncing = useRef(false);

  useEffect(() => {
    if (!enabled) return;

    async function drainQueue() {
      if (syncing.current) return;
      syncing.current = true;

      try {
        const queued = await getQueuedMovements();
        let syncedAny = false;

        for (const movement of queued) {
          try {
            if (movement.kind === 'EXPENSE') {
              await createExpense(movement.payload as ExpenseRequest);
            } else {
              await createIncome(movement.payload as IncomeRequest);
            }
            await removeQueuedMovement(movement.id);
            syncedAny = true;
          } catch {
            break;
          }
        }

        if (syncedAny) {
          void queryClient.invalidateQueries({ queryKey: [EXPENSES_QUERY_KEY] });
          void queryClient.invalidateQueries({ queryKey: [INCOMES_QUERY_KEY] });
        }
      } finally {
        syncing.current = false;
      }
    }

    void drainQueue();
    const unsubscribe = NetInfo.addEventListener((state) => {
      if (state.isConnected) void drainQueue();
    });

    return () => unsubscribe();
  }, [enabled, queryClient]);
}
