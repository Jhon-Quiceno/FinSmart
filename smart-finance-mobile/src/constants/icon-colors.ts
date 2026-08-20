import { useColorScheme } from 'react-native';

import { korofinRawColors } from './korofin-colors';

/**
 * Colores de íconos de lucide-react-native (el prop `color` no puede consumir variables CSS
 * de NativeWind) que SÍ cambian con el tema — antes eran constantes fijas del tema claro
 * únicamente (deuda técnica documentada acá mismo hasta este pase).
 */
export interface IconColors {
  ICON_COLOR_MUTED: string;
  ICON_COLOR_FOREGROUND: string;
  ICON_COLOR_PRIMARY: string;
  ICON_COLOR_DESTRUCTIVE: string;
  ICON_COLOR_SUCCESS: string;
  ICON_COLOR_WARNING: string;
  ICON_COLOR_WHITE: string;
}

function toRgb(triplet: string): string {
  return `rgb(${triplet})`;
}

export function useIconColors(): IconColors {
  const scheme = useColorScheme();
  const theme = korofinRawColors[scheme === 'dark' ? 'dark' : 'light'];

  return {
    ICON_COLOR_MUTED: toRgb(theme['--color-muted-foreground']),
    ICON_COLOR_FOREGROUND: toRgb(theme['--color-foreground']),
    ICON_COLOR_PRIMARY: toRgb(theme['--color-primary']),
    ICON_COLOR_DESTRUCTIVE: toRgb(theme['--color-destructive']),
    ICON_COLOR_SUCCESS: toRgb(theme['--color-success']),
    ICON_COLOR_WARNING: toRgb(theme['--color-warning']),
    // El blanco no depende del tema (se usa sobre fondos siempre-verdes/primary, ej. FAB).
    ICON_COLOR_WHITE: 'rgb(255 255 255)',
  };
}
