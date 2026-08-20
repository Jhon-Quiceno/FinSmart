import { Stack } from 'expo-router';
import { Bell, BellOff } from 'lucide-react-native';
import { useState } from 'react';
import { ScrollView, Switch, View } from 'react-native';

import { AppText as Text } from '@/components/app-text';
import { SafeAreaView } from 'react-native-safe-area-context';

import { useIconColors } from '@/constants/icon-colors';
import { CARD_SHADOW } from '@/lib/shadows';

// TODO(Fase 0 backend): conectar con expo-notifications + ExpoPushAdapter (§Fase 0 del plan)
// para pedir el permiso real del sistema y registrar el push token contra el backend.
export default function NotificacionesScreen() {
  const [pushEnabled, setPushEnabled] = useState(false);
  const { ICON_COLOR_PRIMARY } = useIconColors();

  return (
    <SafeAreaView className="flex-1 bg-background">
      <Stack.Screen options={{ headerShown: true, title: 'Notificaciones push' }} />
      <ScrollView contentContainerClassName="gap-5 px-6 py-8" showsVerticalScrollIndicator={false}>
        <View className="items-center gap-3">
          <View className="h-16 w-16 items-center justify-center rounded-full bg-primary/15">
            {pushEnabled ? (
              <Bell size={28} color={ICON_COLOR_PRIMARY} />
            ) : (
              <BellOff size={28} color={ICON_COLOR_PRIMARY} />
            )}
          </View>
          <Text className="text-center text-sm text-muted-foreground">
            KoroFin es la primera vez que tiene notificaciones push nativas — antes solo tenías
            avisos dentro de la app y por correo.
          </Text>
        </View>

        <View className="flex-row items-center justify-between rounded-xl border border-border bg-card p-4" style={CARD_SHADOW}>
          <View className="flex-1 pr-3">
            <Text className="text-sm font-medium text-foreground">Activar notificaciones push</Text>
            <Text className="text-xs text-muted-foreground">Recibí alertas aunque no tengas la app abierta</Text>
          </View>
          <Switch value={pushEnabled} onValueChange={setPushEnabled} />
        </View>

        <View className="gap-3 rounded-xl border border-border bg-card p-4" style={CARD_SHADOW}>
          <ToggleRow label="Recordatorios de pago" defaultValue disabled={!pushEnabled} />
          <ToggleRow label="Alertas de sobregasto" defaultValue disabled={!pushEnabled} />
          <ToggleRow label="Resumen semanal" disabled={!pushEnabled} />
          <ToggleRow label="Recordatorio de inactividad" defaultValue disabled={!pushEnabled} />
        </View>
      </ScrollView>
    </SafeAreaView>
  );
}

function ToggleRow({
  label,
  defaultValue = false,
  disabled = false,
}: {
  label: string;
  defaultValue?: boolean;
  disabled?: boolean;
}) {
  const [value, setValue] = useState(defaultValue);
  return (
    <View className="flex-row items-center justify-between">
      <Text className={`text-sm ${disabled ? 'text-muted-foreground' : 'text-foreground'}`}>{label}</Text>
      <Switch value={value} onValueChange={setValue} disabled={disabled} />
    </View>
  );
}
