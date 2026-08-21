jest.mock('@/lib/services/auth.service', () => ({
  loginRequest: jest.fn(),
  registerRequest: jest.fn(),
  refreshRequest: jest.fn(),
  logoutRequest: jest.fn(),
}));

jest.mock('@/lib/session', () => ({
  getRefreshToken: jest.fn(),
  clearSessionStorage: jest.fn(),
}));

jest.mock('@/lib/push', () => ({
  registerPushTokenForCurrentUser: jest.fn(),
}));

jest.mock('@/lib/services/notification.service', () => ({
  unregisterPushToken: jest.fn(),
}));

jest.mock('@/lib/device-id', () => ({
  getDeviceId: jest.fn(),
}));

jest.mock('@/lib/api-client', () => {
  let listener: (() => void) | null = null;
  return {
    getApiErrorMessage: jest.fn((_error: unknown, fallback: string) => fallback),
    setSessionExpiredListener: jest.fn((l: (() => void) | null) => {
      listener = l;
    }),
    __triggerSessionExpired: () => listener?.(),
  };
});

jest.mock('@/lib/query-client', () => ({
  queryClient: {
    cancelQueries: jest.fn().mockResolvedValue(undefined),
    clear: jest.fn(),
  },
}));

import { act, renderHook, waitFor } from '@testing-library/react-native';

import * as apiClientMock from '@/lib/api-client';
import { registerPushTokenForCurrentUser } from '@/lib/push';
import { loginRequest, logoutRequest, refreshRequest } from '@/lib/services/auth.service';
import { unregisterPushToken } from '@/lib/services/notification.service';
import { clearSessionStorage, getRefreshToken } from '@/lib/session';
import { queryClient } from '@/lib/query-client';

import { AuthProvider, useAuth } from './auth-context';

const mockedGetRefreshToken = getRefreshToken as jest.Mock;
const mockedClearSessionStorage = clearSessionStorage as jest.Mock;
const mockedRefreshRequest = refreshRequest as jest.Mock;
const mockedLoginRequest = loginRequest as jest.Mock;
const mockedLogoutRequest = logoutRequest as jest.Mock;
const mockedUnregisterPushToken = unregisterPushToken as jest.Mock;
const mockedRegisterPushTokenForCurrentUser = registerPushTokenForCurrentUser as jest.Mock;

const mockUser = { id: 1, name: 'Ana', email: 'ana@mail.com' };

function renderAuth() {
  return renderHook(() => useAuth(), { wrapper: AuthProvider });
}

describe('AuthProvider / useAuth', () => {
  beforeEach(() => {
    jest.clearAllMocks();
    mockedGetRefreshToken.mockResolvedValue(null);
    mockedClearSessionStorage.mockResolvedValue(undefined);
    mockedUnregisterPushToken.mockResolvedValue(undefined);
    mockedLogoutRequest.mockResolvedValue(undefined);
  });

  it('bootstrap termina en unauthenticated sin llamar a refreshRequest cuando no hay refresh token', async () => {
    mockedGetRefreshToken.mockResolvedValue(null);

    const { result } = await renderAuth();

    await waitFor(() => expect(result.current.status).toBe('unauthenticated'));
    expect(mockedRefreshRequest).not.toHaveBeenCalled();
  });

  it('bootstrap termina en authenticated con el usuario devuelto cuando el refresh tiene exito', async () => {
    mockedGetRefreshToken.mockResolvedValue('stored-refresh-token');
    mockedRefreshRequest.mockResolvedValue({
      accessToken: 'a',
      tokenType: 'Bearer',
      expiresIn: 900,
      user: mockUser,
    });

    const { result } = await renderAuth();

    await waitFor(() => expect(result.current.status).toBe('authenticated'));
    expect(result.current.user).toEqual(mockUser);
  });

  it('bootstrap termina en unauthenticated y limpia el storage cuando el refresh falla', async () => {
    mockedGetRefreshToken.mockResolvedValue('stored-refresh-token');
    mockedRefreshRequest.mockRejectedValue(new Error('expired'));

    const { result } = await renderAuth();

    await waitFor(() => expect(result.current.status).toBe('unauthenticated'));
    expect(mockedClearSessionStorage).toHaveBeenCalled();
  });

  it('login exitoso deja authenticated y dispara el registro de push exactamente una vez', async () => {
    mockedLoginRequest.mockResolvedValue({
      accessToken: 'a',
      tokenType: 'Bearer',
      expiresIn: 900,
      user: mockUser,
    });

    const { result } = await renderAuth();
    await waitFor(() => expect(result.current.status).toBe('unauthenticated'));

    const actionResult = await act(async () => result.current.login('ana@mail.com', 'secret123', true));

    expect(actionResult).toEqual({ success: true });
    expect(result.current.status).toBe('authenticated');
    expect(result.current.user).toEqual(mockUser);

    // Fire-and-forget: se dispara sin esperarse, flusheamos microtasks para verificar la llamada.
    await Promise.resolve();
    expect(mockedRegisterPushTokenForCurrentUser).toHaveBeenCalledTimes(1);
  });

  it('login fallido devuelve success:false con mensaje y no lanza excepcion', async () => {
    mockedLoginRequest.mockRejectedValue({
      isAxiosError: true,
      response: { data: { message: 'Credenciales inválidas' } },
    });

    const { result } = await renderAuth();
    await waitFor(() => expect(result.current.status).toBe('unauthenticated'));

    const actionResult = await act(async () => result.current.login('ana@mail.com', 'wrong', false));

    expect(actionResult.success).toBe(false);
    expect(actionResult.error).toBeTruthy();
    expect(result.current.status).toBe('unauthenticated');
  });

  it('logout deja unauthenticated y user null incluso si logoutRequest falla', async () => {
    mockedLogoutRequest.mockRejectedValue(new Error('network down'));
    mockedGetRefreshToken.mockResolvedValue('stored-refresh-token');
    mockedRefreshRequest.mockResolvedValue({
      accessToken: 'a',
      tokenType: 'Bearer',
      expiresIn: 900,
      user: mockUser,
    });

    const { result } = await renderAuth();
    await waitFor(() => expect(result.current.status).toBe('authenticated'));

    await act(async () => {
      await result.current.logout();
    });

    expect(result.current.status).toBe('unauthenticated');
    expect(result.current.user).toBeNull();
    expect(queryClient.cancelQueries).toHaveBeenCalled();
    expect(queryClient.clear).toHaveBeenCalled();
  });

  it('el listener de sesion expirada mueve el status a unauthenticated', async () => {
    mockedGetRefreshToken.mockResolvedValue('stored-refresh-token');
    mockedRefreshRequest.mockResolvedValue({
      accessToken: 'a',
      tokenType: 'Bearer',
      expiresIn: 900,
      user: mockUser,
    });

    const { result } = await renderAuth();
    await waitFor(() => expect(result.current.status).toBe('authenticated'));

    (apiClientMock as unknown as { __triggerSessionExpired: () => void }).__triggerSessionExpired();

    await waitFor(() => expect(result.current.status).toBe('unauthenticated'));
    expect(result.current.user).toBeNull();
  });
});
