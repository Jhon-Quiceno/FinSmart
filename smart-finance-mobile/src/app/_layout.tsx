import { Slot } from 'expo-router';

import { KorofinThemeProvider } from '@/components/korofin-theme-provider';
import '@/global.css';

export default function RootLayout() {
  return (
    <KorofinThemeProvider>
      <Slot />
    </KorofinThemeProvider>
  );
}
