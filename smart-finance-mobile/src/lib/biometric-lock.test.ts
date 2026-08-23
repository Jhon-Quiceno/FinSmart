jest.mock('expo-secure-store', () => {
  const globalStore = globalThis as unknown as { __secureStoreFake?: Map<string, string> };
  if (!globalStore.__secureStoreFake) {
    globalStore.__secureStoreFake = new Map<string, string>();
  }
  const store = globalStore.__secureStoreFake;
  return {
    getItemAsync: jest.fn(async (key: string) => store.get(key) ?? null),
    setItemAsync: jest.fn(async (key: string, value: string) => {
      store.set(key, value);
    }),
    __store: store,
  };
});

jest.mock('expo-local-authentication', () => ({
  hasHardwareAsync: jest.fn(),
  isEnrolledAsync: jest.fn(),
  authenticateAsync: jest.fn(),
}));

import * as LocalAuthentication from 'expo-local-authentication';
import {
  authenticate,
  isBiometricAvailable,
  isBiometricLockEnabled,
  setBiometricLockEnabled,
} from './biometric-lock';

function getSecureStoreFake() {
  return require('expo-secure-store') as { __store: Map<string, string> };
}

describe('biometric lock', () => {
  beforeEach(() => {
    getSecureStoreFake().__store.clear();
    jest.clearAllMocks();
  });

  describe('isBiometricLockEnabled / setBiometricLockEnabled', () => {
    it('defaults to disabled when nothing was stored yet', async () => {
      expect(await isBiometricLockEnabled()).toBe(false);
    });

    it('persists the enabled preference', async () => {
      await setBiometricLockEnabled(true);
      expect(await isBiometricLockEnabled()).toBe(true);
    });

    it('persists the disabled preference', async () => {
      await setBiometricLockEnabled(true);
      await setBiometricLockEnabled(false);
      expect(await isBiometricLockEnabled()).toBe(false);
    });
  });

  describe('isBiometricAvailable', () => {
    it('is true only when there is hardware AND an enrolled biometric', async () => {
      (LocalAuthentication.hasHardwareAsync as jest.Mock).mockResolvedValue(true);
      (LocalAuthentication.isEnrolledAsync as jest.Mock).mockResolvedValue(true);

      expect(await isBiometricAvailable()).toBe(true);
    });

    it('is false when there is hardware but nothing enrolled', async () => {
      (LocalAuthentication.hasHardwareAsync as jest.Mock).mockResolvedValue(true);
      (LocalAuthentication.isEnrolledAsync as jest.Mock).mockResolvedValue(false);

      expect(await isBiometricAvailable()).toBe(false);
    });

    it('is false when there is no hardware at all', async () => {
      (LocalAuthentication.hasHardwareAsync as jest.Mock).mockResolvedValue(false);
      (LocalAuthentication.isEnrolledAsync as jest.Mock).mockResolvedValue(false);

      expect(await isBiometricAvailable()).toBe(false);
    });
  });

  describe('authenticate', () => {
    it('resolves true when the native prompt succeeds', async () => {
      (LocalAuthentication.authenticateAsync as jest.Mock).mockResolvedValue({ success: true });

      expect(await authenticate('Desbloqueá KoroFin')).toBe(true);
    });

    it('resolves false when the native prompt fails or is cancelled', async () => {
      (LocalAuthentication.authenticateAsync as jest.Mock).mockResolvedValue({
        success: false,
        error: 'user_cancel',
      });

      expect(await authenticate('Desbloqueá KoroFin')).toBe(false);
    });
  });
});
