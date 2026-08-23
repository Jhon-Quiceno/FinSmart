import * as LocalAuthentication from 'expo-local-authentication';
import * as SecureStore from 'expo-secure-store';

/**
 * Bloqueo biométrico de acceso local (M5 del track móvil, docs/plan-sprints-movil-nativo.md).
 * Solo gatea el acceso a la UI ya montada — el refreshToken sigue viviendo en expo-secure-store
 * sin cambios, la biometría no reemplaza ese mecanismo (ver Decisiones del milestone). Preferencia
 * puramente local: no hay backend involucrado, por eso vive en expo-secure-store y no en
 * /api/users/preferences.
 */
const BIOMETRIC_LOCK_KEY = 'korofin.biometric-lock-enabled';

export async function isBiometricLockEnabled(): Promise<boolean> {
  try {
    return (await SecureStore.getItemAsync(BIOMETRIC_LOCK_KEY)) === 'true';
  } catch {
    return false;
  }
}

export async function setBiometricLockEnabled(enabled: boolean): Promise<void> {
  await SecureStore.setItemAsync(BIOMETRIC_LOCK_KEY, enabled ? 'true' : 'false');
}

/** @return true si el dispositivo tiene sensor Y ya tiene huella/rostro enrolado. */
export async function isBiometricAvailable(): Promise<boolean> {
  const [hasHardware, isEnrolled] = await Promise.all([
    LocalAuthentication.hasHardwareAsync(),
    LocalAuthentication.isEnrolledAsync(),
  ]);
  return hasHardware && isEnrolled;
}

export async function authenticate(promptMessage: string): Promise<boolean> {
  const result = await LocalAuthentication.authenticateAsync({ promptMessage });
  return result.success;
}
