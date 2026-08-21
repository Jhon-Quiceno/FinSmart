/** Mismo formato que `formatCurrency` en smart-finance-frontend (es-MX, 2 decimales, prefijo $). */
export function formatCurrency(value: number): string {
  return `$${value.toLocaleString('es-MX', { minimumFractionDigits: 2, maximumFractionDigits: 2 })}`;
}

export function formatDate(iso: string): string {
  return new Date(iso).toLocaleDateString('es-MX', { day: '2-digit', month: 'short', year: 'numeric' });
}

export function formatPercentage(fraction: number): string {
  return `${Math.round(fraction * 100)}%`;
}

/**
 * Tiempo relativo simple para el feed de notificaciones ("Hace 2 horas", "Ayer"). No existe
 * ninguna librería de fechas en el proyecto (no hay date-fns en package.json) — se evita agregar
 * una dependencia nueva solo para esto.
 */
export function formatRelativeTime(iso: string): string {
  const diffMs = Date.now() - new Date(iso).getTime();
  const diffMinutes = Math.round(diffMs / 60_000);

  if (diffMinutes < 1) return 'Ahora mismo';
  if (diffMinutes < 60) return `Hace ${diffMinutes} min`;

  const diffHours = Math.round(diffMinutes / 60);
  if (diffHours < 24) return `Hace ${diffHours} h`;

  const diffDays = Math.round(diffHours / 24);
  if (diffDays === 1) return 'Ayer';
  if (diffDays < 7) return `Hace ${diffDays} días`;

  return formatDate(iso);
}
