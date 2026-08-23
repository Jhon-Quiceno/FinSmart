jest.mock('./offline-queue', () => ({
  enqueueMovement: jest.fn(),
}));

import MockAdapter from 'axios-mock-adapter';
import { apiClient } from './api-client';
import { createWithOfflineFallback } from './offline-create';
import { enqueueMovement } from './offline-queue';
import { clearAccessToken, setAccessToken } from './session';
import { createExpense } from './services/expense.service';
import type { ExpenseRequest } from './types/expense';

const mockedEnqueueMovement = enqueueMovement as jest.MockedFunction<typeof enqueueMovement>;

// header.{"sub":"42","email":"x@y.com"}.signature - ver jwt.test.ts para el mismo token de prueba.
const FAKE_TOKEN_FOR_USER_42 = 'header.eyJzdWIiOiI0MiIsImVtYWlsIjoieEB5LmNvbSJ9.signature';

describe('createWithOfflineFallback', () => {
  let mock: MockAdapter;

  const payload: ExpenseRequest = {
    amount: 100,
    description: 'Cafe',
    date: '2026-08-20',
    paymentMethod: 'CASH',
  };

  beforeEach(() => {
    mock = new MockAdapter(apiClient);
    mock.onGet('/api/users/csrf').reply(200, { token: 'csrf-token' });
    mockedEnqueueMovement.mockReset();
    setAccessToken(FAKE_TOKEN_FOR_USER_42);
  });

  afterEach(() => {
    mock.restore();
    clearAccessToken();
  });

  it('returns the real result when the server responds successfully', async () => {
    const created = { id: 1, ...payload, categoryId: null, categoryName: null, recurringPaymentId: null };
    mock.onPost('/api/expenses').reply(201, created);

    const result = await createWithOfflineFallback('EXPENSE', payload, () => createExpense(payload));

    expect(result).toEqual(created);
    expect(mockedEnqueueMovement).not.toHaveBeenCalled();
  });

  it('propagates a real server error instead of queueing it', async () => {
    mock.onPost('/api/expenses').reply(400, { message: 'Monto invalido' });

    await expect(createWithOfflineFallback('EXPENSE', payload, () => createExpense(payload))).rejects.toBeTruthy();
    expect(mockedEnqueueMovement).not.toHaveBeenCalled();
  });

  it('queues the movement when the failure is a connectivity error', async () => {
    mock.onPost('/api/expenses').networkError();

    const result = await createWithOfflineFallback('EXPENSE', payload, () => createExpense(payload));

    expect(result).toEqual({ queued: true });
    expect(mockedEnqueueMovement).toHaveBeenCalledWith(42, 'EXPENSE', payload);
  });

  it('propagates the connectivity error instead of queueing when there is no identifiable user', async () => {
    clearAccessToken();
    mock.onPost('/api/expenses').networkError();

    await expect(createWithOfflineFallback('EXPENSE', payload, () => createExpense(payload))).rejects.toBeTruthy();
    expect(mockedEnqueueMovement).not.toHaveBeenCalled();
  });
});
