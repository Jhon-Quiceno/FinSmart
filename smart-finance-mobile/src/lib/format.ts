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
