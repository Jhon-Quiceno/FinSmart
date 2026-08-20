import { useFonts } from 'expo-font';
import { Stack } from 'expo-router';
import * as SplashScreen from 'expo-splash-screen';
import { useEffect } from 'react';
import { useColorScheme } from 'react-native';

import { KorofinThemeProvider } from '@/components/korofin-theme-provider';
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

export default function RootLayout() {
  const colorScheme = useColorScheme();
  const theme = HEADER_THEME[colorScheme === 'dark' ? 'dark' : 'light'];

  // Inter-Variable.ttf es el variable font oficial de Google Fonts (eje de peso 100-900 en
  // un solo archivo) — la misma tipografía que carga la web vía next/font/google. Con un
  // solo archivo, las clases `font-medium`/`font-semibold`/`font-bold` que ya usa toda la
  // app (fontWeight numérico) siguen funcionando tal cual, sin tener que retocar pantallas.
  const [fontsLoaded] = useFonts({
    Inter: require('../../assets/fonts/Inter-Variable.ttf'),
  });

  useEffect(() => {
    if (fontsLoaded) {
      SplashScreen.hideAsync();
    }
  }, [fontsLoaded]);

  if (!fontsLoaded) {
    return null;
  }

  return (
    <KorofinThemeProvider>
      <Stack
        screenOptions={{
          headerShown: false,
          headerStyle: { backgroundColor: theme.card },
          headerTintColor: theme.tint,
          headerShadowVisible: false,
          contentStyle: { backgroundColor: theme.background },
        }}
      />
    </KorofinThemeProvider>
  );
}
