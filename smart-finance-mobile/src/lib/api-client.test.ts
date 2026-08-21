import { AxiosError } from 'axios';
import MockAdapter from 'axios-mock-adapter';

jest.mock('@/lib/session', () => ({
  getAccessToken: jest.fn(),
  setAccessToken: jest.fn(),
  clearAccessToken: jest.fn(),
  getRefreshToken: jest.fn(),
  saveRefreshToken: jest.fn(),
  clearRefreshToken: jest.fn(),
}));

import { apiClient, getApiErrorMessage, setSessionExpiredListener } from './api-client';
import * as session from './session';

const mockedSession = session as jest.Mocked<typeof session>;

describe('apiClient', () => {
  let mock: MockAdapter;

  beforeEach(() => {
    mock = new MockAdapter(apiClient);
    jest.clearAllMocks();
    setSessionExpiredListener(null);
  });

  afterEach(() => {
    mock.restore();
  });

  it('siempre manda el header X-Client: mobile', async () => {
    mock.onGet('/api/ping').reply((config) => {
      expect(config.headers?.['X-Client']).toBe('mobile');
      return [200, {}];
    });

    await apiClient.get('/api/ping');
  });

  it('agrega Authorization cuando hay accessToken, y lo omite cuando no hay', async () => {
    mockedSession.getAccessToken.mockReturnValue('token-abc');
    mock.onGet('/api/ping').reply((config) => {
      expect(config.headers?.Authorization).toBe('Bearer token-abc');
      return [200, {}];
    });
    await apiClient.get('/api/ping');

    mockedSession.getAccessToken.mockReturnValue(null);
    mock.onGet('/api/ping').reply((config) => {
      expect(config.headers?.Authorization).toBeUndefined();
      return [200, {}];
    });
    await apiClient.get('/api/ping');
  });

  it('un 401 en un endpoint protegido dispara UN refresh y reintenta con exito', async () => {
    mockedSession.getAccessToken.mockReturnValue('expired-token');
    mockedSession.getRefreshToken.mockResolvedValue('stored-refresh-token');

    let protectedCalls = 0;
    mock.onGet('/api/expenses').reply(() => {
      protectedCalls += 1;
      if (protectedCalls === 1) return [401, { message: 'Unauthorized' }];
      return [200, { data: 'ok' }];
    });
    mock.onPost('/api/users/refresh').reply(200, {
      accessToken: 'new-access-token',
      tokenType: 'Bearer',
      expiresIn: 900,
      user: { id: 1, name: 'Ana', email: 'ana@mail.com' },
    });

    const response = await apiClient.get('/api/expenses');

    expect(response.status).toBe(200);
    const refreshCalls = mock.history.post.filter((r) => r.url === '/api/users/refresh');
    expect(refreshCalls).toHaveLength(1);
    expect(JSON.parse(refreshCalls[0].data)).toEqual({ refreshToken: 'stored-refresh-token' });
  });

  it('persiste el refreshToken rotado devuelto por /refresh', async () => {
    mockedSession.getAccessToken.mockReturnValue('expired-token');
    mockedSession.getRefreshToken.mockResolvedValue('stored-refresh-token');

    let protectedCalls = 0;
    mock.onGet('/api/expenses').reply(() => {
      protectedCalls += 1;
      if (protectedCalls === 1) return [401, {}];
      return [200, {}];
    });
    mock.onPost('/api/users/refresh').reply(200, {
      accessToken: 'new-access-token',
      tokenType: 'Bearer',
      expiresIn: 900,
      user: { id: 1, name: 'Ana', email: 'ana@mail.com' },
      refreshToken: 'rotated-refresh-token',
    });

    await apiClient.get('/api/expenses');

    expect(mockedSession.saveRefreshToken).toHaveBeenCalledWith('rotated-refresh-token');
  });

  it('dos requests que dan 401 en paralelo disparan UN solo refresh (single-flight)', async () => {
    mockedSession.getAccessToken.mockReturnValue('expired-token');
    mockedSession.getRefreshToken.mockResolvedValue('stored-refresh-token');

    const calls: Record<string, number> = {};
    mock.onGet('/api/expenses').reply(() => {
      calls.expenses = (calls.expenses ?? 0) + 1;
      if (calls.expenses === 1) return [401, {}];
      return [200, { ok: 'expenses' }];
    });
    mock.onGet('/api/incomes').reply(() => {
      calls.incomes = (calls.incomes ?? 0) + 1;
      if (calls.incomes === 1) return [401, {}];
      return [200, { ok: 'incomes' }];
    });
    mock.onPost('/api/users/refresh').reply(200, {
      accessToken: 'new-access-token',
      tokenType: 'Bearer',
      expiresIn: 900,
      user: { id: 1, name: 'Ana', email: 'ana@mail.com' },
    });

    const [expensesResponse, incomesResponse] = await Promise.all([
      apiClient.get('/api/expenses'),
      apiClient.get('/api/incomes'),
    ]);

    expect(expensesResponse.status).toBe(200);
    expect(incomesResponse.status).toBe(200);
    const refreshCalls = mock.history.post.filter((r) => r.url === '/api/users/refresh');
    expect(refreshCalls).toHaveLength(1);
  });

  it('una request ya marcada como retry (segundo 401 tras refresh) no dispara un segundo refresh', async () => {
    mockedSession.getAccessToken.mockReturnValue('expired-token');
    mockedSession.getRefreshToken.mockResolvedValue('stored-refresh-token');

    mock.onGet('/api/expenses').reply(401, {});
    mock.onPost('/api/users/refresh').reply(200, {
      accessToken: 'new-access-token',
      tokenType: 'Bearer',
      expiresIn: 900,
      user: { id: 1, name: 'Ana', email: 'ana@mail.com' },
    });

    await expect(apiClient.get('/api/expenses')).rejects.toBeTruthy();

    const refreshCalls = mock.history.post.filter((r) => r.url === '/api/users/refresh');
    expect(refreshCalls).toHaveLength(1);
  });

  it('un 401 en /api/users/login NO dispara un intento de refresh', async () => {
    mockedSession.getRefreshToken.mockResolvedValue('stored-refresh-token');
    mock.onPost('/api/users/login').reply(401, { message: 'Credenciales invalidas' });
    mock.onPost('/api/users/refresh').reply(200, {});

    await expect(apiClient.post('/api/users/login', { email: 'a', password: 'b' })).rejects.toBeTruthy();

    const refreshCalls = mock.history.post.filter((r) => r.url === '/api/users/refresh');
    expect(refreshCalls).toHaveLength(0);
  });

  it('sin refresh token guardado, un 401 rechaza sin llamar a /refresh y dispara sessionExpiredListener', async () => {
    mockedSession.getAccessToken.mockReturnValue('expired-token');
    mockedSession.getRefreshToken.mockResolvedValue(null);
    const listener = jest.fn();
    setSessionExpiredListener(listener);

    mock.onGet('/api/expenses').reply(401, {});

    await expect(apiClient.get('/api/expenses')).rejects.toBeTruthy();

    const refreshCalls = mock.history.post.filter((r) => r.url === '/api/users/refresh');
    expect(refreshCalls).toHaveLength(0);
    expect(listener).toHaveBeenCalledTimes(1);
  });

  it('si el refresh mismo falla, limpia tokens y dispara sessionExpiredListener una sola vez', async () => {
    mockedSession.getAccessToken.mockReturnValue('expired-token');
    mockedSession.getRefreshToken.mockResolvedValue('stored-refresh-token');
    const listener = jest.fn();
    setSessionExpiredListener(listener);

    mock.onGet('/api/expenses').reply(401, {});
    mock.onPost('/api/users/refresh').reply(401, { message: 'Refresh invalido' });

    await expect(apiClient.get('/api/expenses')).rejects.toBeTruthy();

    expect(mockedSession.clearAccessToken).toHaveBeenCalled();
    expect(mockedSession.clearRefreshToken).toHaveBeenCalled();
    expect(listener).toHaveBeenCalledTimes(1);
  });

  describe('getApiErrorMessage', () => {
    it('devuelve el message del backend cuando esta presente', () => {
      const error = new AxiosError('Request failed', '401');
      error.response = {
        data: { message: 'Correo o contrasena invalidos' },
        status: 401,
        statusText: 'Unauthorized',
        headers: {},
        config: {} as never,
      };
      expect(getApiErrorMessage(error, 'fallback')).toBe('Correo o contrasena invalidos');
    });

    it('devuelve el mensaje de "no hay conexion" cuando no hay response', () => {
      const error = new AxiosError('Network Error', 'ERR_NETWORK');
      expect(getApiErrorMessage(error, 'fallback')).toContain('No hay conexión con el servidor');
    });

    it('devuelve el fallback para cualquier otra cosa', () => {
      expect(getApiErrorMessage(new Error('boom'), 'fallback')).toBe('fallback');
    });
  });
});
