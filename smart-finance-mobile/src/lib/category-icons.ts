import {
  Banknote,
  Car,
  HeartPulse,
  PiggyBank,
  Repeat,
  ShoppingCart,
  UtensilsCrossed,
  Zap,
  type LucideIcon,
} from 'lucide-react-native';

// Ícono específico por categoría para las filas de movimientos/transacciones — más informativo
// que la flecha genérica de ingreso/gasto que se usaba antes. Nombres coherentes con
// `lib/mock/categorias.ts`/`lib/mock/movimientos.ts`.
const CATEGORY_ICONS: Record<string, LucideIcon> = {
  Mercado: ShoppingCart,
  Transporte: Car,
  Suscripciones: Repeat,
  Servicios: Zap,
  Restaurantes: UtensilsCrossed,
  Salud: HeartPulse,
  Salario: Banknote,
  'Otros ingresos': PiggyBank,
};

/** `null` cuando la categoría no está mapeada (o el movimiento no tiene categoría) — el
 * llamador decide el fallback (ej. la flecha genérica de ingreso/gasto). */
export function getCategoryIcon(categoryName: string | null | undefined): LucideIcon | null {
  if (!categoryName) return null;
  return CATEGORY_ICONS[categoryName] ?? null;
}
