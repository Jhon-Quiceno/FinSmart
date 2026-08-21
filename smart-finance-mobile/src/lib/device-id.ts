import * as Crypto from 'expo-crypto';
import * as SecureStore from 'expo-secure-store';

const DEVICE_ID_KEY = 'korofin.deviceId';

/**
 * Identificador estable por instalacion. NO lo borra clearSessionStorage(): si se borrara,
 * cada logout dejaria huerfana la fila de push_tokens del usuario anterior en el backend.
 */
export async function getDeviceId(): Promise<string> {
  const existing = await SecureStore.getItemAsync(DEVICE_ID_KEY).catch(() => null);
  if (existing) return existing;
  const generated = Crypto.randomUUID();
  await SecureStore.setItemAsync(DEVICE_ID_KEY, generated).catch(() => {});
  return generated;
}
