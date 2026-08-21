import Constants from 'expo-constants';

const DEV_BACKEND_PORT = 8080;

function inferDevApiUrl(): string | null {
  const hostUri = Constants.expoConfig?.hostUri ?? Constants.expoGoConfig?.debuggerHost ?? null;
  if (!hostUri) return null;
  const host = hostUri.split(':')[0];
  if (!host || host === 'localhost' || host === '127.0.0.1') return null;
  return `http://${host}:${DEV_BACKEND_PORT}`;
}

// Un celular fisico con Expo Go no puede resolver "localhost": ese hostname se resuelve dentro
// del propio telefono, no en la PC donde corre el backend. Por eso, en dev, se deriva la IP LAN
// desde el hostUri que expone el servidor de Metro (mismo host al que ya se conecta el bundle
// JS) y se asume que Spring Boot escucha ahi en su puerto por defecto (8080).
export const API_BASE_URL: string =
  process.env.EXPO_PUBLIC_API_URL ?? inferDevApiUrl() ?? `http://localhost:${DEV_BACKEND_PORT}`;
