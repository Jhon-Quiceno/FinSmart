import NetInfo from '@react-native-community/netinfo';
import { useQueryClient } from '@tanstack/react-query';
import { useEffect, useRef } from 'react';

import { isConnectivityFailure } from '@/lib/connectivity';
import { getQueuedMovements, removeQueuedMovement } from '@/lib/offline-queue';
import { createExpense } from '@/lib/services/expense.service';
import { createIncome } from '@/lib/services/income.service';
import { getCurrentUserId } from '@/lib/session';
import type { ExpenseRequest } from '@/lib/types/expense';
import type { IncomeRequest } from '@/lib/types/income';

import { EXPENSES_QUERY_KEY } from './use-expenses';
import { INCOMES_QUERY_KEY } from './use-incomes';

/**
 * Drena la cola offline (M5, docs/plan-sprints-movil-nativo.md) al recuperar conexión: reintenta
 * cada gasto/ingreso encolado, en orden FIFO, contra el backend real - solo los del usuario
 * actualmente autenticado (ver offline-queue.ts).
 *
 * Dos tipos de falla se tratan distinto adrede:
 * - Falla de conectividad (seguimos sin red real): se detiene TODO el drenado en ese punto, sin
 *   tocar el resto de la cola - el próximo evento de reconexión retoma desde ahí.
 * - Rechazo real del servidor (ej. la categoría elegida offline ya no existe): reintentarlo para
 *   siempre nunca lo va a arreglar, así que ESE movimiento puntual se descarta y se sigue con el
 *   resto - la alternativa (no descartarlo nunca) dejaría toda la cola trabada indefinidamente
 *   detrás de un ítem que no puede sincronizar. No hay, hoy, ninguna notificación al usuario de un
 *   descarte así - limitación conocida de este alcance acotado.
 */
export function useOfflineSync(enabled: boolean): void {
  const queryClient = useQueryClient();
  const syncing = useRef(false);

  useEffect(() => {
    if (!enabled) return;

    async function drainQueue() {
      if (syncing.current) return;
      const userId = getCurrentUserId();
      if (userId === null) return;

      syncing.current = true;
      try {
        const queued = await getQueuedMovements(userId);
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
          } catch (error) {
            if (isConnectivityFailure(error)) break;

            console.warn('offline_sync_discarded_movement', movement.id, error);
            await removeQueuedMovement(movement.id);
          }
        }

        if (syncedAny) {
          void queryClient.invalidateQueries({ queryKey: [EXPENSES_QUERY_KEY] });
          void queryClient.invalidateQueries({ queryKey: [INCOMES_QUERY_KEY] });
        }
      } catch (error) {
        console.warn('offline_sync_failed', error);
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
