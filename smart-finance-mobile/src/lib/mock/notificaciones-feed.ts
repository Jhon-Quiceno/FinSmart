// Mock del centro de notificaciones (feed histórico de alertas ya generadas por la app) —
// reemplazar por GET /api/notifications cuando exista el endpoint real (TODO Fase 0 backend).
// Distinto de las preferencias de push en notificaciones.tsx: esto es el historial, no el opt-in.

export type NotificationSeverity = 'HIGH' | 'MEDIUM' | 'LOW';

export interface MockNotification {
  id: number;
  title: string;
  description: string;
  timestamp: string;
  severity: NotificationSeverity;
  read: boolean;
}

export const mockNotificationsFeed: MockNotification[] = [
  {
    id: 1,
    title: 'Presupuesto excedido',
    description: 'Superaste tu límite en Servicios este mes',
    timestamp: 'Hace 2 horas',
    severity: 'HIGH',
    read: false,
  },
  {
    id: 2,
    title: 'Recordatorio de pago',
    description: 'La tarjeta Mastercard Gold vence en 3 días',
    timestamp: 'Hace 5 horas',
    severity: 'MEDIUM',
    read: false,
  },
  {
    id: 3,
    title: 'Nueva sugerencia de ahorro',
    description: 'Podrías ahorrar $150.000 reduciendo gastos en Transporte',
    timestamp: 'Ayer',
    severity: 'LOW',
    read: true,
  },
  {
    id: 4,
    title: 'Resumen semanal disponible',
    description: 'Ya podés ver tu resumen de la semana pasada',
    timestamp: 'Hace 2 días',
    severity: 'LOW',
    read: true,
  },
  {
    id: 5,
    title: 'Vínculo con Telegram activo',
    description: 'Tu bot ya está listo para registrar gastos',
    timestamp: 'Hace 3 días',
    severity: 'LOW',
    read: true,
  },
];
