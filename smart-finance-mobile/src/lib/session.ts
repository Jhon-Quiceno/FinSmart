import * as SecureStore from 'expo-secure-store';

// Primitivas de sesion: access token en memoria (nunca en disco) + refresh token persistido en
// SecureStore (Keychain/Keystore cifrado por el OS). Reglas deliberadas:
//   - Nunca cae a AsyncStorage como fallback: AsyncStorage no esta cifrado, y guardar ahi un
//     refresh token de larga vida seria exponerlo en texto plano en el filesystem del telefono.
//     Si SecureStore no esta disponible, el token se degrada a memoria (se pierde al reiniciar
//     la app) en vez de escribirse en un lugar inseguro.
//   - keychainAccessible: WHEN_UNLOCKED_THIS_DEVICE_ONLY (no el default WHEN_UNLOCKED): ata el
//     valor a ESTE dispositivo. Sin el "_THIS_DEVICE_ONLY", un backup de iCloud/Keychain
//     restaurado en otro telefono resucitaria una sesion viva ahi.
//   - requireAuthentication (biometria) deliberadamente NO se usa aca: eso rompe en cualquier
//     dispositivo sin biometria/PIN configurado, y es una decision de UX de Fase 3, no de esta
//     capa base de sesion.
const REFRESH_TOKEN_KEY = 'korofin.refreshToken';
const SECURE_STORE_VALUE_LIMIT = 2048; // Limite de SecureStore en Android

let accessToken: string | null = null;
let volatileRefreshToken: string | null = null; // vive solo en memoria: rememberMe=false, o SecureStore no disponible
let persistSession = true;
let secureStoreAvailability: Promise<boolean> | null = null;

export function getAccessToken(): string | null {
  return accessToken;
}

export function setAccessToken(token: string | null): void {
  accessToken = token;
}

export function clearAccessToken(): void {
  accessToken = null;
}

/** El login la llama con el valor real de "Recordar mi sesión". */
export function setPersistSession(persist: boolean): void {
  persistSession = persist;
}

async function isSecureStoreAvailable(): Promise<boolean> {
  if (!secureStoreAvailability) {
    secureStoreAvailability = SecureStore.isAvailableAsync().catch(() => false);
  }
  return secureStoreAvailability;
}

export async function saveRefreshToken(token: string): Promise<void> {
  if (!persistSession || token.length > SECURE_STORE_VALUE_LIMIT || !(await isSecureStoreAvailable())) {
    volatileRefreshToken = token;
    return;
  }
  try {
    await SecureStore.setItemAsync(REFRESH_TOKEN_KEY, token, {
      keychainAccessible: SecureStore.WHEN_UNLOCKED_THIS_DEVICE_ONLY,
    });
    volatileRefreshToken = null;
  } catch {
    volatileRefreshToken = token;
  }
}

export async function getRefreshToken(): Promise<string | null> {
  if (volatileRefreshToken) return volatileRefreshToken;
  if (!(await isSecureStoreAvailable())) return null;
  try {
    return await SecureStore.getItemAsync(REFRESH_TOKEN_KEY);
  } catch {
    await SecureStore.deleteItemAsync(REFRESH_TOKEN_KEY).catch(() => {});
    return null;
  }
}

export async function clearRefreshToken(): Promise<void> {
  volatileRefreshToken = null;
  if (await isSecureStoreAvailable()) {
    await SecureStore.deleteItemAsync(REFRESH_TOKEN_KEY).catch(() => {});
  }
}

/** Limpia access + refresh. NO toca korofin.deviceId (ver device-id.ts). */
export async function clearSessionStorage(): Promise<void> {
  clearAccessToken();
  await clearRefreshToken();
}
