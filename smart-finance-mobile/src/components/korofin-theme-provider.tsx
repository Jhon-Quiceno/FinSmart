import type { PropsWithChildren } from 'react';
import { useColorScheme, View } from 'react-native';

import { korofinThemes } from '@/constants/korofin-colors';

/**
 * Injects the KoroFin CSS-variable palette (see korofin-colors.ts) into the native style
 * tree via nativewind's `vars()`, so `bg-background`/`text-foreground`/etc. resolve to the
 * right light/dark hex at runtime — the RN equivalent of `:root`/`.dark` on the web.
 */
export function KorofinThemeProvider({ children }: PropsWithChildren) {
  const colorScheme = useColorScheme();
  const theme = korofinThemes[colorScheme === 'dark' ? 'dark' : 'light'];

  return (
    <View style={[theme, { flex: 1 }]} className="bg-background">
      {children}
    </View>
  );
}
