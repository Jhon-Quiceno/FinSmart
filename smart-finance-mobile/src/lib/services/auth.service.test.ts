import MockAdapter from 'axios-mock-adapter';

jest.mock('@/lib/session', () => ({
  getAccessToken: jest.fn(() => null),
  setAccessToken: jest.fn(),
  saveRefreshToken: jest.fn(),
  clearAccessToken: jest.fn(),
  clearRefreshToken: jest.fn(),
  setPersistSession: jest.fn(),
  getRefreshToken: jest.fn(),
}));

import { apiClient } from '@/lib/api-client';
import * as session from '@/lib/session';

import { loginRequest, logoutRequest, refreshRequest, registerRequest } from './auth.service';

const mockedSession = session as jest.Mocked<typeof session>;

describe('auth.service', () => {
  let mock: MockAdapter;

  beforeEach(() => {
    mock = new MockAdapter(apiClient);
    jest.clearAllMocks();
  });

  afterEach(() => {
    mock.restore();
  });

  describe('loginRequest', () => {
    it('postea {email, password, rememberMe: true} y persiste la sesion con el rememberMe original', async () => {
      mock.onPost('/api/users/login').reply(200, {
        accessToken: 'access-token',
        tokenType: 'Bearer',
        expiresIn: 900,
        user: { id: 1, name: 'Ana', email: 'ana@mail.com' },
        refreshToken: 'refresh-token',
      });

      await loginRequest('ana@mail.com', 'secret123', false);

      expect(JSON.parse(mock.history.post[0].data)).toEqual({
        email: 'ana@mail.com',
        password: 'secret123',
        rememberMe: true,
      });
      expect(mockedSession.setPersistSession).toHaveBeenCalledWith(false);
      expect(mockedSession.setAccessToken).toHaveBeenCalledWith('access-token');
      expect(mockedSession.saveRefreshToken).toHaveBeenCalledWith('refresh-token');
    });
  });

  describe('registerRequest', () => {
    it('persiste la sesion igual que login', async () => {
      mock.onPost('/api/users/register').reply(201, {
        accessToken: 'access-token',
        tokenType: 'Bearer',
        expiresIn: 900,
        user: { id: 1, name: 'Ana', email: 'ana@mail.com' },
        refreshToken: 'refresh-token',
      });

      await registerRequest('Ana', 'ana@mail.com', 'secret123');

      expect(mockedSession.setAccessToken).toHaveBeenCalledWith('access-token');
      expect(mockedSession.saveRefreshToken).toHaveBeenCalledWith('refresh-token');
    });
  });

  describe('logoutRequest', () => {
    it('llama a /logout y limpia ambos tokens incluso si el servidor responde 500', async () => {
      mock.onPost('/api/users/logout').reply(500);

      await expect(logoutRequest()).rejects.toBeTruthy();

      expect(mockedSession.clearAccessToken).toHaveBeenCalled();
      expect(mockedSession.clearRefreshToken).toHaveBeenCalled();
    });

    it('limpia ambos tokens cuando el servidor responde bien', async () => {
      mock.onPost('/api/users/logout').reply(204);

      await expect(logoutRequest()).resolves.toBeUndefined();

      expect(mockedSession.clearAccessToken).toHaveBeenCalled();
      expect(mockedSession.clearRefreshToken).toHaveBeenCalled();
    });
  });

  describe('refreshRequest', () => {
    it('manda el refresh token guardado en el body y persiste la nueva sesion', async () => {
      mockedSession.getRefreshToken.mockResolvedValue('stored-refresh-token');
      mock.onPost('/api/users/refresh').reply(200, {
        accessToken: 'new-access-token',
        tokenType: 'Bearer',
        expiresIn: 900,
        user: { id: 1, name: 'Ana', email: 'ana@mail.com' },
        refreshToken: 'rotated-refresh-token',
      });

      const response = await refreshRequest();

      expect(JSON.parse(mock.history.post[0].data)).toEqual({ refreshToken: 'stored-refresh-token' });
      expect(response.accessToken).toBe('new-access-token');
      expect(mockedSession.setAccessToken).toHaveBeenCalledWith('new-access-token');
      expect(mockedSession.saveRefreshToken).toHaveBeenCalledWith('rotated-refresh-token');
    });

    it('rechaza sin llamar al backend cuando no hay refresh token guardado', async () => {
      mockedSession.getRefreshToken.mockResolvedValue(null);

      await expect(refreshRequest()).rejects.toBeTruthy();

      expect(mock.history.post).toHaveLength(0);
    });
  });
});
