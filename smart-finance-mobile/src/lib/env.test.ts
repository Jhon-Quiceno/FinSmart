describe('API_BASE_URL', () => {
  const originalEnv = process.env.EXPO_PUBLIC_API_URL;

  beforeEach(() => {
    jest.resetModules();
  });

  afterEach(() => {
    process.env.EXPO_PUBLIC_API_URL = originalEnv;
  });

  it('usa EXPO_PUBLIC_API_URL cuando esta definida, sin importar hostUri', () => {
    process.env.EXPO_PUBLIC_API_URL = 'http://staging.example.com:9000';
    jest.doMock('expo-constants', () => ({
      __esModule: true,
      default: { expoConfig: { hostUri: '192.168.1.5:8081' } },
    }));

    const { API_BASE_URL } = require('./env');

    expect(API_BASE_URL).toBe('http://staging.example.com:9000');
  });

  it('deriva la IP LAN desde hostUri de Metro (puerto 8081 -> 8080) cuando no hay env var', () => {
    delete process.env.EXPO_PUBLIC_API_URL;
    jest.doMock('expo-constants', () => ({
      __esModule: true,
      default: { expoConfig: { hostUri: '192.168.1.5:8081' } },
    }));

    const { API_BASE_URL } = require('./env');

    expect(API_BASE_URL).toBe('http://192.168.1.5:8080');
  });

  it('cae a localhost:8080 cuando no hay env var ni hostUri util', () => {
    delete process.env.EXPO_PUBLIC_API_URL;
    jest.doMock('expo-constants', () => ({
      __esModule: true,
      default: {},
    }));

    const { API_BASE_URL } = require('./env');

    expect(API_BASE_URL).toBe('http://localhost:8080');
  });
});
