jest.mock('expo-secure-store', () => {
  // Se guarda en globalThis (no en una variable de modulo) para que sobreviva a
  // jest.resetModules(): eso simula el almacenamiento real del dispositivo, que persiste
  // aunque los modulos de JS se vuelvan a cargar (a diferencia de los `let` de session.ts).
  const globalStore = globalThis as unknown as { __secureStoreFake?: Map<string, string> };
  if (!globalStore.__secureStoreFake) {
    globalStore.__secureStoreFake = new Map<string, string>();
  }
  const store = globalStore.__secureStoreFake;
  return {
    isAvailableAsync: jest.fn(async () => true),
    getItemAsync: jest.fn(async (key: string) => store.get(key) ?? null),
    setItemAsync: jest.fn(async (key: string, value: string) => {
      store.set(key, value);
    }),
    deleteItemAsync: jest.fn(async (key: string) => {
      store.delete(key);
    }),
    WHEN_UNLOCKED_THIS_DEVICE_ONLY: 'WHEN_UNLOCKED_THIS_DEVICE_ONLY',
    __store: store,
  };
});

import * as SecureStore from 'expo-secure-store';

type FakeSecureStore = {
  isAvailableAsync: jest.Mock<Promise<boolean>, []>;
  getItemAsync: jest.Mock<Promise<string | null>, [string]>;
  setItemAsync: jest.Mock<Promise<void>, [string, string, unknown?]>;
  deleteItemAsync: jest.Mock<Promise<void>, [string]>;
  WHEN_UNLOCKED_THIS_DEVICE_ONLY: typeof SecureStore.WHEN_UNLOCKED_THIS_DEVICE_ONLY;
  __store: Map<string, string>;
};

function getFake(): FakeSecureStore {
  return require('expo-secure-store') as FakeSecureStore;
}

describe('session', () => {
  beforeEach(() => {
    jest.resetModules();
    getFake().__store.clear();
    jest.clearAllMocks();
  });

  it('con setPersistSession(true), guarda en SecureStore y lo relee tras un reload del modulo', async () => {
    const session = require('./session') as typeof import('./session');
    session.setPersistSession(true);

    await session.saveRefreshToken('refresh-token-1');

    expect(getFake().setItemAsync).toHaveBeenCalledWith(
      'korofin.refreshToken',
      'refresh-token-1',
      expect.objectContaining({ keychainAccessible: 'WHEN_UNLOCKED_THIS_DEVICE_ONLY' }),
    );

    jest.resetModules();
    const reloadedSession = require('./session') as typeof import('./session');
    await expect(reloadedSession.getRefreshToken()).resolves.toBe('refresh-token-1');
  });

  it('con setPersistSession(false), no llama a setItemAsync y el token no sobrevive un reload', async () => {
    const session = require('./session') as typeof import('./session');
    session.setPersistSession(false);

    await session.saveRefreshToken('refresh-token-2');

    expect(getFake().setItemAsync).not.toHaveBeenCalled();

    jest.resetModules();
    const reloadedSession = require('./session') as typeof import('./session');
    await expect(reloadedSession.getRefreshToken()).resolves.toBeNull();
  });

  it('si getItemAsync rechaza, getRefreshToken resuelve null y limpia la entrada envenenada', async () => {
    getFake().getItemAsync.mockRejectedValueOnce(new Error('corrupted'));
    const session = require('./session') as typeof import('./session');

    await expect(session.getRefreshToken()).resolves.toBeNull();
    expect(getFake().deleteItemAsync).toHaveBeenCalledWith('korofin.refreshToken');
  });

  it('clearSessionStorage limpia access y refresh token pero no toca korofin.deviceId', async () => {
    const session = require('./session') as typeof import('./session');
    session.setPersistSession(true);
    session.setAccessToken('access-token');
    await session.saveRefreshToken('refresh-token-3');

    await session.clearSessionStorage();

    expect(session.getAccessToken()).toBeNull();
    await expect(session.getRefreshToken()).resolves.toBeNull();
    expect(getFake().deleteItemAsync).not.toHaveBeenCalledWith('korofin.deviceId');
  });

  it('cuando isAvailableAsync resuelve false, saveRefreshToken igual resuelve (fallback volatil) sin llamar setItemAsync', async () => {
    getFake().isAvailableAsync.mockResolvedValue(false);
    const session = require('./session') as typeof import('./session');
    session.setPersistSession(true);

    await expect(session.saveRefreshToken('refresh-token-4')).resolves.toBeUndefined();

    expect(getFake().setItemAsync).not.toHaveBeenCalled();
    await expect(session.getRefreshToken()).resolves.toBe('refresh-token-4');
  });
});
