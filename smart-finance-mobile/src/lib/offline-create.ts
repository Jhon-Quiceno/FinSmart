import axios from 'axios';

import { enqueueMovement, type QueuedMovementKind } from './offline-queue';
import type { ExpenseRequest } from './types/expense';
import type { IncomeRequest } from './types/income';

export interface QueuedResult {
  queued: true;
}

/** Una falla sin `response` es de conectividad (nunca llegó a un servidor) - ver getApiErrorMessage. */
function isConnectivityFailure(error: unknown): boolean {
  return axios.isAxiosError(error) && !error.response;
}

/**
 * Modo offline acotado (M5, docs/plan-sprints-movil-nativo.md): si el servidor respondió (incluso
 * con un error real, ej. 400/409), se comporta exactamente igual que antes - ese error nunca se
 * encola, se propaga tal cual. Solo una falla de conectividad encola el movimiento localmente para
 * sincronizarlo cuando vuelva la conexión (ver use-offline-sync.ts), en vez de perderlo o mostrar
 * un error confuso al usuario.
 */
export async function createWithOfflineFallback<T>(
  kind: QueuedMovementKind,
  payload: ExpenseRequest | IncomeRequest,
  createFn: () => Promise<T>,
): Promise<T | QueuedResult> {
  try {
    return await createFn();
  } catch (error) {
    if (!isConnectivityFailure(error)) throw error;
    await enqueueMovement(kind, payload);
    return { queued: true };
  }
}
