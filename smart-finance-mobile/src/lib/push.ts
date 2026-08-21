import Constants from 'expo-constants';
import * as Device from 'expo-device';
import * as Notifications from 'expo-notifications';

import { getDeviceId } from '@/lib/device-id';
import { registerPushToken } from '@/lib/services/notification.service';

/**
 * Silencioso por diseño en cada punto de salida: sin dispositivo fisico, sin permiso, o sin
 * projectId de EAS configurado (este proyecto todavia no corrio `eas init`), simplemente no
 * hace nada. Nunca debe bloquear ni fallar el flujo de login.
 */
export async function registerPushTokenForCurrentUser(): Promise<void> {
  if (!Device.isDevice) return;

  const settings = await Notifications.getPermissionsAsync();
  const granted =
    settings.granted || (settings.canAskAgain && (await Notifications.requestPermissionsAsync()).granted);
  if (!granted) return;

  const projectId = Constants.expoConfig?.extra?.eas?.projectId;
  if (!projectId) return;

  const { data: expoPushToken } = await Notifications.getExpoPushTokenAsync({ projectId });
  await registerPushToken({ expoPushToken, deviceId: await getDeviceId() });
}
