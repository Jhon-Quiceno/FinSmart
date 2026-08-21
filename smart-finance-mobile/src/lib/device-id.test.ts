jest.mock('expo-crypto', () => ({
  randomUUID: jest.fn(() => 'generated-uuid'),
}));

jest.mock('expo-secure-store', () => {
  const store = new Map<string, string>();
  return {
    getItemAsync: jest.fn(async (key: string) => store.get(key) ?? null),
    setItemAsync: jest.fn(async (key: string, value: string) => {
      store.set(key, value);
    }),
    __store: store,
  };
});

import * as SecureStore from 'expo-secure-store';

import { getDeviceId } from './device-id';

describe('getDeviceId', () => {
  afterEach(() => {
    (SecureStore as unknown as { __store: Map<string, string> }).__store.clear();
    jest.clearAllMocks();
  });

  it('genera un UUID y lo persiste la primera vez', async () => {
    const id = await getDeviceId();

    expect(id).toBe('generated-uuid');
    expect(SecureStore.setItemAsync).toHaveBeenCalledWith('korofin.deviceId', 'generated-uuid');
  });

  it('devuelve el UUID ya guardado sin generar uno nuevo', async () => {
    (SecureStore as unknown as { __store: Map<string, string> }).__store.set('korofin.deviceId', 'existing-uuid');

    const id = await getDeviceId();

    expect(id).toBe('existing-uuid');
    expect(SecureStore.setItemAsync).not.toHaveBeenCalled();
  });
});
