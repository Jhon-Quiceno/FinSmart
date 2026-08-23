import type { RecurringFrequency } from '@/lib/types/recurring-payment';

export const FREQUENCY_LABELS: Record<RecurringFrequency, string> = {
  MONTHLY: 'Mensual',
  WEEKLY: 'Semanal',
};
