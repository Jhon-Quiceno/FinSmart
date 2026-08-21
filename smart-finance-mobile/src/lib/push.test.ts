// isDevice se expone como getter (no como valor plano): el interop de "import * as" de Babel
// copia propiedades de valor a un objeto nuevo por cada importador, asi que reasignar
// Device.isDevice desde el test no se veria reflejado en la copia que usa push.ts. Un getter
// preserva el mismo descriptor (y por lo tanto la misma funcion) en cada copia.
let mockIsDevice = true;
jest.mock('expo-device', () => ({
  get isDevice() {
    return mockIsDevice;
  },
}));

jest.mock('expo-notifications', () => ({
  getPermissionsAsync: jest.fn(),
  requestPermissionsAsync: jest.fn(),
  getExpoPushTokenAsync: jest.fn(),
}));

jest.mock('expo-constants', () => ({
  __esModule: true,
  default: {
    expoConfig: { extra: {} },
  },
}));

jest.mock('@/lib/device-id', () => ({
  getDeviceId: jest.fn(),
}));

jest.mock('@/lib/services/notification.service', () => ({
  registerPushToken: jest.fn(),
}));

import Constants from 'expo-constants';
import * as Notifications from 'expo-notifications';

import { getDeviceId } from '@/lib/device-id';
import { registerPushToken } from '@/lib/services/notification.service';

import { registerPushTokenForCurrentUser } from './push';

const mockedNotifications = Notifications as jest.Mocked<typeof Notifications>;
const mockedGetDeviceId = getDeviceId as jest.Mock;
const mockedRegisterPushToken = registerPushToken as jest.Mock;

describe('registerPushTokenForCurrentUser', () => {
  beforeEach(() => {
    jest.clearAllMocks();
    mockIsDevice = true;
    (Constants as unknown as { expoConfig: { extra: Record<string, unknown> } }).expoConfig = { extra: {} };
  });

  it('no hace nada si no es un dispositivo fisico', async () => {
    mockIsDevice = false;

    await registerPushTokenForCurrentUser();

    expect(mockedNotifications.getPermissionsAsync).not.toHaveBeenCalled();
  });

  it('no hace nada si el permiso fue denegado y no se puede volver a pedir', async () => {
    mockedNotifications.getPermissionsAsync.mockResolvedValue({
      granted: false,
      canAskAgain: false,
      expires: 'never',
      status: 'denied',
    } as Notifications.NotificationPermissionsStatus);

    await registerPushTokenForCurrentUser();

    expect(mockedNotifications.getExpoPushTokenAsync).not.toHaveBeenCalled();
    expect(mockedRegisterPushToken).not.toHaveBeenCalled();
  });

  it('no hace nada si hay permiso pero no hay projectId de EAS configurado', async () => {
    mockedNotifications.getPermissionsAsync.mockResolvedValue({
      granted: true,
      canAskAgain: true,
      expires: 'never',
      status: 'granted',
    } as Notifications.NotificationPermissionsStatus);
    (Constants as unknown as { expoConfig: { extra: Record<string, unknown> } }).expoConfig = { extra: {} };

    await registerPushTokenForCurrentUser();

    expect(mockedNotifications.getExpoPushTokenAsync).not.toHaveBeenCalled();
  });

  it('registra el push token cuando hay permiso y projectId', async () => {
    mockedNotifications.getPermissionsAsync.mockResolvedValue({
      granted: true,
      canAskAgain: true,
      expires: 'never',
      status: 'granted',
    } as Notifications.NotificationPermissionsStatus);
    (Constants as unknown as { expoConfig: { extra: Record<string, unknown> } }).expoConfig = {
      extra: { eas: { projectId: 'project-123' } },
    };
    mockedNotifications.getExpoPushTokenAsync.mockResolvedValue({
      type: 'expo',
      data: 'ExponentPushToken[abc123]',
    });
    mockedGetDeviceId.mockResolvedValue('device-1');

    await registerPushTokenForCurrentUser();

    expect(mockedNotifications.getExpoPushTokenAsync).toHaveBeenCalledWith({ projectId: 'project-123' });
    expect(mockedRegisterPushToken).toHaveBeenCalledWith({
      expoPushToken: 'ExponentPushToken[abc123]',
      deviceId: 'device-1',
    });
  });
});
