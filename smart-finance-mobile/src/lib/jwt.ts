/**
 * Decodificación local (sin verificar firma) del payload de un JWT — suficiente para leer un
 * claim propio (ej. `sub`) del lado del cliente; la validación real de la firma la hace siempre
 * el backend. Base64url manual en vez de `atob`/`Buffer`: ninguno de los dos está garantizado
 * disponible en todos los runtimes de Hermes/Jest de este proyecto.
 */
const BASE64_CHARS = 'ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789+/';

function base64UrlDecode(input: string): string {
  const base64 = input.replace(/-/g, '+').replace(/_/g, '/');

  let output = '';
  let buffer = 0;
  let bits = 0;

  for (const char of base64) {
    const value = BASE64_CHARS.indexOf(char);
    if (value === -1) continue;
    buffer = (buffer << 6) | value;
    bits += 6;
    if (bits >= 8) {
      bits -= 8;
      output += String.fromCharCode((buffer >> bits) & 0xff);
    }
  }
  return output;
}

export function decodeJwtPayload<T = Record<string, unknown>>(token: string): T | null {
  try {
    const [, payloadSegment] = token.split('.');
    if (!payloadSegment) return null;
    return JSON.parse(base64UrlDecode(payloadSegment)) as T;
  } catch {
    return null;
  }
}
