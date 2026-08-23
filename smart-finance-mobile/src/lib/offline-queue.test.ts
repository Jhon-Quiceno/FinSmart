jest.mock('expo-sqlite', () => {
  // En globalThis (no en un `let` de módulo) para que sobreviva a jest.resetModules() dentro de
  // un mismo test, igual que el fake de expo-secure-store en session.test.ts.
  const globalStore = globalThis as unknown as {
    __sqliteFake?: {
      rows: { id: number; user_id: number; kind: string; payload: string; created_at: string }[];
      nextId: number;
    };
  };
  if (!globalStore.__sqliteFake) {
    globalStore.__sqliteFake = { rows: [], nextId: 1 };
  }
  const state = globalStore.__sqliteFake;

  const fakeDb = {
    execAsync: jest.fn(async () => undefined),
    runAsync: jest.fn(async (sql: string, ...params: unknown[]) => {
      if (sql.startsWith('INSERT')) {
        const [userId, kind, payload, createdAt] = params as [number, string, string, string];
        state.rows.push({ id: state.nextId, user_id: userId, kind, payload, created_at: createdAt });
        state.nextId += 1;
      } else if (sql.startsWith('DELETE')) {
        const [id] = params as [number];
        state.rows = state.rows.filter((row) => row.id !== id);
      }
      return { lastInsertRowId: state.nextId - 1, changes: 1 };
    }),
    getAllAsync: jest.fn(async (sql: string, ...params: unknown[]) => {
      if (sql.includes('WHERE user_id')) {
        const [userId] = params as [number];
        return state.rows.filter((row) => row.user_id === userId);
      }
      return [...state.rows];
    }),
    getFirstAsync: jest.fn(async (sql: string, ...params: unknown[]) => {
      if (sql.includes('WHERE user_id')) {
        const [userId] = params as [number];
        return { count: state.rows.filter((row) => row.user_id === userId).length };
      }
      return { count: state.rows.length };
    }),
  };

  return {
    openDatabaseAsync: jest.fn(async () => fakeDb),
    __state: state,
  };
});

import {
  countQueuedMovements,
  enqueueMovement,
  getQueuedMovements,
  removeQueuedMovement,
} from './offline-queue';

function resetFakeSqlite() {
  const globalStore = globalThis as unknown as { __sqliteFake?: { rows: unknown[]; nextId: number } };
  if (globalStore.__sqliteFake) {
    globalStore.__sqliteFake.rows = [];
    globalStore.__sqliteFake.nextId = 1;
  }
}

function getFakeRows() {
  return (globalThis as unknown as { __sqliteFake: { rows: { payload: string }[] } }).__sqliteFake.rows;
}

const USER_A = 1;
const USER_B = 2;

describe('offline queue', () => {
  beforeEach(() => {
    resetFakeSqlite();
  });

  it('starts empty', async () => {
    expect(await getQueuedMovements(USER_A)).toEqual([]);
    expect(await countQueuedMovements(USER_A)).toBe(0);
  });

  it('enqueues a movement and lists it back with its payload intact', async () => {
    await enqueueMovement(USER_A, 'EXPENSE', {
      amount: 45000,
      description: 'Supermercado',
      date: '2026-08-20',
      paymentMethod: 'CASH',
      categoryId: 2,
    });

    const queued = await getQueuedMovements(USER_A);
    expect(queued).toHaveLength(1);
    expect(queued[0].kind).toBe('EXPENSE');
    expect(queued[0].payload).toEqual({
      amount: 45000,
      description: 'Supermercado',
      date: '2026-08-20',
      paymentMethod: 'CASH',
      categoryId: 2,
    });
  });

  it('preserves FIFO order across multiple enqueued movements', async () => {
    await enqueueMovement(USER_A, 'EXPENSE', { amount: 100, date: '2026-08-20', paymentMethod: 'CASH' });
    await enqueueMovement(USER_A, 'INCOME', { amount: 200, date: '2026-08-21' });

    const queued = await getQueuedMovements(USER_A);
    expect(queued.map((m) => m.kind)).toEqual(['EXPENSE', 'INCOME']);
  });

  it('removes a queued movement by id', async () => {
    await enqueueMovement(USER_A, 'EXPENSE', { amount: 100, date: '2026-08-20', paymentMethod: 'CASH' });
    const [first] = await getQueuedMovements(USER_A);

    await removeQueuedMovement(first.id);

    expect(await getQueuedMovements(USER_A)).toEqual([]);
    expect(await countQueuedMovements(USER_A)).toBe(0);
  });

  it('never mixes movements queued by different users on the same device', async () => {
    await enqueueMovement(USER_A, 'EXPENSE', { amount: 100, date: '2026-08-20', paymentMethod: 'CASH' });
    await enqueueMovement(USER_B, 'INCOME', { amount: 200, date: '2026-08-21' });

    expect(await getQueuedMovements(USER_A)).toHaveLength(1);
    expect(await getQueuedMovements(USER_B)).toHaveLength(1);
    expect((await getQueuedMovements(USER_A))[0].kind).toBe('EXPENSE');
    expect((await getQueuedMovements(USER_B))[0].kind).toBe('INCOME');
  });

  it('drops a row with a corrupted payload instead of failing the whole read', async () => {
    await enqueueMovement(USER_A, 'EXPENSE', { amount: 100, date: '2026-08-20', paymentMethod: 'CASH' });
    getFakeRows()[0].payload = '{not valid json';

    const queued = await getQueuedMovements(USER_A);

    expect(queued).toEqual([]);
    expect(await countQueuedMovements(USER_A)).toBe(0);
  });
});
