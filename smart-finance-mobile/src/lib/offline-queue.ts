import * as SQLite from 'expo-sqlite';

import type { ExpenseRequest } from './types/expense';
import type { IncomeRequest } from './types/income';

/**
 * Modo offline acotado (M5 del track móvil, docs/plan-sprints-movil-nativo.md): cola local de
 * altas de gasto/ingreso creadas sin conexión, para sincronizar cuando vuelva. No cubre edición ni
 * borrado, ni ningún otro dominio — a propósito, ver Decisiones del milestone.
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
  kind: QueuedMovementKind,
  payload: ExpenseRequest | IncomeRequest,
): Promise<void> {
  const db = await getDb();
  await db.runAsync(
    'INSERT INTO pending_movements (kind, payload, created_at) VALUES (?, ?, ?)',
    kind,
    JSON.stringify(payload),
    new Date().toISOString(),
  );
}

export async function getQueuedMovements(): Promise<QueuedMovement[]> {
  const db = await getDb();
  const rows = await db.getAllAsync<PendingMovementRow>(
    'SELECT id, kind, payload, created_at FROM pending_movements ORDER BY id ASC',
  );
  return rows.map((row) => ({
    id: row.id,
    kind: row.kind,
    payload: JSON.parse(row.payload) as ExpenseRequest | IncomeRequest,
    createdAt: row.created_at,
  }));
}

export async function removeQueuedMovement(id: number): Promise<void> {
  const db = await getDb();
  await db.runAsync('DELETE FROM pending_movements WHERE id = ?', id);
}

export async function countQueuedMovements(): Promise<number> {
  const db = await getDb();
  const row = await db.getFirstAsync<{ count: number }>('SELECT COUNT(*) as count FROM pending_movements');
  return row?.count ?? 0;
}
