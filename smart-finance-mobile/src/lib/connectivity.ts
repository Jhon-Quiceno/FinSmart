import axios from 'axios';

/**
 * Una falla sin `response` nunca llegó a un servidor real (sin conexión, DNS, timeout antes de
 * respuesta) - ver `getApiErrorMessage`. Una falla CON `response` (400/404/409/500, etc.) es un
 * rechazo real del servidor: reintentarla no cambia nada, así que nunca debe tratarse como
 * "reintentar más tarde".
 */
export function isConnectivityFailure(error: unknown): boolean {
  return axios.isAxiosError(error) && !error.response;
}
