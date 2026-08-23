import { Stack } from 'expo-router';
import { Globe, Palette, Wallet, type LucideIcon } from 'lucide-react-native';
import { Pressable, View } from 'react-native';

import { AppText as Text } from '@/components/app-text';
import { SafeAreaView } from 'react-native-safe-area-context';

import { useThemePreference, type ThemePreference } from '@/components/korofin-theme-provider';
import { useIconColors } from '@/constants/icon-colors';
import { CARD_SHADOW } from '@/lib/shadows';

const THEME_PREFERENCE_ORDER: ThemePreference[] = ['system', 'light', 'dark'];

const THEME_PREFERENCE_LABELS: Record<ThemePreference, string> = {
  system: 'Sistema',
  light: 'Claro',
  dark: 'Oscuro',
};

const READ_ONLY_ROWS: { label: string; value: string; icon: LucideIcon }[] = [
  { label: 'Moneda', value: 'COP - Peso colombiano', icon: Wallet },
  { label: 'Idioma', value: 'Español', icon: Globe },
];

export default function PreferenciasScreen() {
  const { ICON_COLOR_MUTED } = useIconColors();
  const { preference, setPreference } = useThemePreference();

  function cycleTheme() {
    const currentIndex = THEME_PREFERENCE_ORDER.indexOf(preference);
    const next = THEME_PREFERENCE_ORDER[(currentIndex + 1) % THEME_PREFERENCE_ORDER.length];
    setPreference(next);
  }

  return (
    <SafeAreaView className="flex-1 bg-background">
      <Stack.Screen options={{ headerShown: true, title: 'Preferencias' }} />
      {/* Ver docs/plan-sprints-movil-nativo.md, milestone M3: conectar con un endpoint real de
          preferencias — moneda e idioma siguen siendo valores fijos de solo lectura, no existe
          GET/PUT /api/users/preferences todavía (deliberadamente diferido a M3, no es deuda de
          Fase 0). El tema ya no es de solo lectura: se aplica de inmediato vía NativeWind
          (`colorScheme.set`) y se persiste en expo-secure-store, sin depender del backend. */}
      <View className="gap-3 px-5 py-6">
        <View className="overflow-hidden rounded-xl border border-border bg-card" style={CARD_SHADOW}>
          <Pressable
            className="flex-row items-center gap-3 border-b border-border px-4 py-3.5 active:opacity-70"
            onPress={cycleTheme}
          >
            <View className="h-9 w-9 items-center justify-center rounded-full bg-secondary">
              <Palette size={16} color={ICON_COLOR_MUTED} />
            </View>
            <Text className="flex-1 text-sm font-medium text-foreground">Tema</Text>
            <Text className="text-sm text-muted-foreground">{THEME_PREFERENCE_LABELS[preference]}</Text>
          </Pressable>
          {READ_ONLY_ROWS.map((row, index) => {
            const Icon = row.icon;
            return (
              <View
                key={row.label}
                className={`flex-row items-center gap-3 px-4 py-3.5 ${
                  index < READ_ONLY_ROWS.length - 1 ? 'border-b border-border' : ''
                }`}
              >
                <View className="h-9 w-9 items-center justify-center rounded-full bg-secondary">
                  <Icon size={16} color={ICON_COLOR_MUTED} />
                </View>
                <Text className="flex-1 text-sm font-medium text-foreground">{row.label}</Text>
                <Text className="text-sm text-muted-foreground">{row.value}</Text>
              </View>
            );
          })}
        </View>
      </View>
    </SafeAreaView>
  );
}
