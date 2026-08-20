import { Stack } from 'expo-router';
import { Globe, Palette, Wallet, type LucideIcon } from 'lucide-react-native';
import { View } from 'react-native';

import { AppText as Text } from '@/components/app-text';
import { SafeAreaView } from 'react-native-safe-area-context';

import { useIconColors } from '@/constants/icon-colors';
import { CARD_SHADOW } from '@/lib/shadows';

const ROWS: { label: string; value: string; icon: LucideIcon }[] = [
  { label: 'Tema', value: 'Claro', icon: Palette },
  { label: 'Moneda', value: 'COP - Peso colombiano', icon: Wallet },
  { label: 'Idioma', value: 'Español', icon: Globe },
];

export default function PreferenciasScreen() {
  const { ICON_COLOR_MUTED } = useIconColors();
  return (
    <SafeAreaView className="flex-1 bg-background">
      <Stack.Screen options={{ headerShown: true, title: 'Preferencias' }} />
      {/* TODO(Fase 0 backend): conectar con un endpoint real de preferencias — hoy son valores
          fijos de solo lectura, no existe GET/PUT /api/users/preferences todavía. */}
      <View className="gap-3 px-5 py-6">
        <View className="overflow-hidden rounded-xl border border-border bg-card" style={CARD_SHADOW}>
          {ROWS.map((row, index) => {
            const Icon = row.icon;
            return (
              <View
                key={row.label}
                className={`flex-row items-center gap-3 px-4 py-3.5 ${
                  index < ROWS.length - 1 ? 'border-b border-border' : ''
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
