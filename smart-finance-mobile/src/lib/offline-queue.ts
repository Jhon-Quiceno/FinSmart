import * as SQLite from 'expo-sqlite';

import type { ExpenseRequest } from './types/expense';
import type { IncomeRequest } from './types/income';

/**
 * Modo offline acotado (M5 del track móvil, docs/plan-sprints-movil-nativo.md): cola local de
 * altas de gasto/ingreso creadas sin conexión, para sincronizar cuando vuelva. No cubre edición ni
 * borrado, ni ningún otro dominio — a propósito, ver Decisiones del milestone.
 *
 * Cada fila lleva el `userId` de quien la encoló: sin esto, un logout con movimientos todavía sin
 * sincronizar + login de otro usuario en el mismo dispositivo terminaría sincronizando los
 * movimientos del primero contra la cuenta del segundo. `getQueuedMovements`/`removeQueuedMovement`
 * siempre están acotados a un `userId` — nunca se opera sobre la cola completa entre usuarios.
 */
export type QueuedMovementKind = 'EXPENSE' | 'INCOME';

export interface QueuedMovement {
  id: number;
  kind: QueuedMovementKind;
  payload: ExpenseRequest | IncomeRequest;
  createdAt: string;
}

interface PendingMovementRow {
  id: number;
  kind: QueuedMovementKind;
  payload: string;
  created_at: string;
}

let dbPromise: Promise<SQLite.SQLiteDatabase> | null = null;

function getDb(): Promise<SQLite.SQLiteDatabase> {
  if (!dbPromise) {
    dbPromise = SQLite.openDatabaseAsync('korofin-offline-queue.db').then(async (db) => {
      await db.execAsync(
        `CREATE TABLE IF NOT EXISTS pending_movements (
          id INTEGER PRIMARY KEY AUTOINCREMENT,
          user_id INTEGER NOT NULL,
          kind TEXT NOT NULL,
          payload TEXT NOT NULL,
          created_at TEXT NOT NULL
        );`,
      );
      return db;
    });
  }
  return dbPromise;
}

export async function enqueueMovement(
  userId: number,
  kind: QueuedMovementKind,
  payload: ExpenseRequest | IncomeRequest,
): Promise<void> {
  const db = await getDb();
  await db.runAsync(
    'INSERT INTO pending_movements (user_id, kind, payload, created_at) VALUES (?, ?, ?, ?)',
    userId,
    kind,
    JSON.stringify(payload),
    new Date().toISOString(),
  );
}

/**
 * @param userId solo se devuelven/afectan movimientos de este usuario — ver Javadoc de la clase.
 * Una fila con `payload` corrupto (no debería pasar nunca, pero un JSON.parse roto acá tiraría y
 * dejaría la cola entera indrenable) se descarta silenciosamente en vez de propagar el error.
 */
export async function getQueuedMovements(userId: number): Promise<QueuedMovement[]> {
  const db = await getDb();
  const rows = await db.getAllAsync<PendingMovementRow>(
    'SELECT id, kind, payload, created_at FROM pending_movements WHERE user_id = ? ORDER BY id ASC',
    userId,
  );

  const parsed: QueuedMovement[] = [];
  for (const row of rows) {
    try {
      parsed.push({
        id: row.id,
        kind: row.kind,
        payload: JSON.parse(row.payload) as ExpenseRequest | IncomeRequest,
        createdAt: row.created_at,
      });
    } catch {
      await removeQueuedMovement(row.id);
    }
  }
  return parsed;
}

export async function removeQueuedMovement(id: number): Promise<void> {
  const db = await getDb();
  await db.runAsync('DELETE FROM pending_movements WHERE id = ?', id);
}

export async function countQueuedMovements(userId: number): Promise<number> {
  const db = await getDb();
  const row = await db.getFirstAsync<{ count: number }>(
    'SELECT COUNT(*) as count FROM pending_movements WHERE user_id = ?',
    userId,
  );
  return row?.count ?? 0;
}
