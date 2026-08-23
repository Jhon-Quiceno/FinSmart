import * as SecureStore from 'expo-secure-store';
import { colorScheme as nativewindColorScheme } from 'nativewind';
import { createContext, useContext, useEffect, useState, type PropsWithChildren } from 'react';
import { useColorScheme, View } from 'react-native';

import { korofinThemes } from '@/constants/korofin-colors';

export type ThemePreference = 'system' | 'light' | 'dark';

const THEME_PREFERENCE_KEY = 'korofin.theme-preference';
const VALID_PREFERENCES: ThemePreference[] = ['system', 'light', 'dark'];

function isThemePreference(value: string | null): value is ThemePreference {
  return value !== null && (VALID_PREFERENCES as string[]).includes(value);
}

interface ThemePreferenceContextValue {
  preference: ThemePreference;
  setPreference: (value: ThemePreference) => void;
}

const ThemePreferenceContext = createContext<ThemePreferenceContextValue | null>(null);

/**
 * Injects the KoroFin CSS-variable palette (see korofin-colors.ts) into the native style
 * tree via nativewind's `vars()`, so `bg-background`/`text-foreground`/etc. resolve to the
 * right light/dark hex at runtime — the RN equivalent of `:root`/`.dark` on the web.
 *
 * Además expone `useThemePreference()`: la preferencia manual (system/light/dark) persistida en
 * expo-secure-store. `nativewind.colorScheme.set(...)` llama a `Appearance.setColorScheme`, que
 * es lo que hace que el `useColorScheme()` de react-native de abajo reaccione de inmediato — no
 * hace falta duplicar el theme resuelto en dos lugares.
 */
export function KorofinThemeProvider({ children }: PropsWithChildren) {
  const colorScheme = useColorScheme();
  const theme = korofinThemes[colorScheme === 'dark' ? 'dark' : 'light'];
  const [preference, setPreferenceState] = useState<ThemePreference>('system');

  useEffect(() => {
    let cancelled = false;

    async function loadStoredPreference() {
      try {
        const stored = await SecureStore.getItemAsync(THEME_PREFERENCE_KEY);
        if (cancelled || !isThemePreference(stored)) return;

        setPreferenceState(stored);
        if (stored !== 'system') {
          nativewindColorScheme.set(stored);
        }
      } catch {
        // Si SecureStore no está disponible, se degrada al comportamiento actual (seguir el SO).
      }
    }

    void loadStoredPreference();

    return () => {
      cancelled = true;
    };
  }, []);

  function setPreference(value: ThemePreference): void {
    setPreferenceState(value);
    nativewindColorScheme.set(value);
    void SecureStore.setItemAsync(THEME_PREFERENCE_KEY, value).catch(() => {
      // Falla silenciosa: el tema igual se aplica en esta sesión, solo no persiste al reiniciar.
    });
  }

  return (
    <ThemePreferenceContext.Provider value={{ preference, setPreference }}>
      <View style={[theme, { flex: 1 }]} className="bg-background">
        {children}
      </View>
    </ThemePreferenceContext.Provider>
  );
}

export function useThemePreference(): ThemePreferenceContextValue {
  const context = useContext(ThemePreferenceContext);
  if (!context) {
    throw new Error('useThemePreference debe usarse dentro de un KorofinThemeProvider');
  }
  return context;
}
