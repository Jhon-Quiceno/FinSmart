import AsyncStorage from '@react-native-async-storage/async-storage';
import { createAsyncStoragePersister } from '@tanstack/query-async-storage-persister';
import { PersistQueryClientProvider } from '@tanstack/react-query-persist-client';
import { useFonts } from 'expo-font';
import { Stack } from 'expo-router';
import * as SplashScreen from 'expo-splash-screen';
import { useEffect } from 'react';
import { useColorScheme } from 'react-native';

import { BiometricLockGate } from '@/components/biometric-lock-gate';
import { KorofinThemeProvider } from '@/components/korofin-theme-provider';
import { AuthProvider, useAuth } from '@/context/auth-context';
import { useOfflineSync } from '@/hooks/use-offline-sync';
import { queryClient } from '@/lib/query-client';
import { useQueryAppStateFocus } from '@/lib/query-focus';
import '@/global.css';

// Colores de header nativo (React Navigation no puede leer clases de NativeWind) — mismas
// tripletas RGB que korofin-colors.ts, solo para background/tinte/borde del header.
const HEADER_THEME = {
  light: { background: 'rgb(250 252 255)', card: 'rgb(255 255 255)', tint: 'rgb(20 24 31)', border: 'rgb(220 222 226)' },
  dark: { background: 'rgb(5 6 7)', card: 'rgb(11 13 17)', tint: 'rgb(235 239 245)', border: 'rgb(38 41 46)' },
};

// El splash se queda visible hasta que la fuente termine de cargar (ver useEffect abajo) —
// evita el flash de texto en la fuente del sistema antes de que Inter esté lista.
SplashScreen.preventAutoHideAsync();

// Modo offline acotado (M5, docs/plan-sprints-movil-nativo.md): persiste el cache de react-query
// en AsyncStorage para que el último estado conocido (dashboard, movimientos, etc.) siga
// disponible al abrir la app sin conexión, no solo mientras el proceso de JS sigue vivo en
// memoria. La cola de altas de gasto/ingreso creadas offline vive aparte, en SQLite
// (ver lib/offline-queue.ts) — react-query solo cachea lecturas.
const asyncStoragePersister = createAsyncStoragePersister({ storage: AsyncStorage });

export default function RootLayout() {
  // Inter-Variable.ttf es el variable font oficial de Google Fonts (eje de peso 100-900 en
  // un solo archivo) — la misma tipografía que carga la web vía next/font/google. Con un
  // solo archivo, las clases `font-medium`/`font-semibold`/`font-bold` que ya usa toda la
  // app (fontWeight numérico) siguen funcionando tal cual, sin tener que retocar pantallas.
  const [fontsLoaded] = useFonts({
    Inter: require('../../assets/fonts/Inter-Variable.ttf'),
  });

  if (!fontsLoaded) {
    return null;
  }

  return (
    <PersistQueryClientProvider
      client={queryClient}
      persistOptions={{ persister: asyncStoragePersister }}
    >
      <AuthProvider>
        <KorofinThemeProvider>
          <BiometricLockGate>
            <RootNavigator />
          </BiometricLockGate>
        </KorofinThemeProvider>
      </AuthProvider>
    </PersistQueryClientProvider>
  );
}

// El splash ahora depende de DOS gates AND-eados: fuentes cargadas (ver RootLayout de arriba)
// Y sesión resuelta (bootstrapping del AuthProvider) — antes solo dependía de las fuentes, lo
// que dejaba ver un frame de /login o /(tabs) "equivocado" mientras el AuthProvider todavía
// no sabía si había sesión.
function RootNavigator() {
  const colorScheme = useColorScheme();
  const theme = HEADER_THEME[colorScheme === 'dark' ? 'dark' : 'light'];
  const { status } = useAuth();
  useQueryAppStateFocus();
  useOfflineSync(status === 'authenticated');

  useEffect(() => {
    if (status !== 'bootstrapping') {
      SplashScreen.hideAsync();
    }
  }, [status]);

  return (
    <Stack
      screenOptions={{
        headerShown: false,
        headerStyle: { backgroundColor: theme.card },
        headerTintColor: theme.tint,
        headerShadowVisible: false,
        contentStyle: { backgroundColor: theme.background },
      }}
    />
  );
}
