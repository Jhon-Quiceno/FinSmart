import { Stack } from 'expo-router';
import * as Notifications from 'expo-notifications';
import { Bell, BellOff } from 'lucide-react-native';
import { useEffect, useState } from 'react';
import { ScrollView, Switch, View } from 'react-native';

import { AppText as Text } from '@/components/app-text';
import { SafeAreaView } from 'react-native-safe-area-context';

import { useIconColors } from '@/constants/icon-colors';
import { useNotificationPreferences, useUpdateNotificationPreferences } from '@/hooks/use-notifications';
import { registerPushTokenForCurrentUser } from '@/lib/push';
import { CARD_SHADOW } from '@/lib/shadows';
import type { NotificationPreferenceRequest } from '@/lib/types/notification';

export default function NotificacionesScreen() {
  const { ICON_COLOR_PRIMARY } = useIconColors();
  const { data: preferences } = useNotificationPreferences();
  const updatePreferences = useUpdateNotificationPreferences();

  // Refleja el permiso real del sistema, no un estado local inventado — se pide con
  // requestPermissionsAsync() recién cuando el usuario activa el switch maestro.
  const [pushEnabled, setPushEnabled] = useState(false);

  useEffect(() => {
    let cancelled = false;
    void Notifications.getPermissionsAsync().then((settings) => {
      if (!cancelled) setPushEnabled(settings.granted);
    });
    return () => {
      cancelled = true;
    };
  }, []);

  async function handleMasterToggle(value: boolean) {
    // No existe una API para revocar el permiso del OS desde la app (es responsabilidad del
    // usuario en Ajustes) — solo se puede reaccionar a un intento de ACTIVAR.
    if (!value) return;

    const settings = await Notifications.requestPermissionsAsync();
    setPushEnabled(settings.granted);
    if (settings.granted) {
      void registerPushTokenForCurrentUser();
    }
  }

  function togglePreference(key: keyof NotificationPreferenceRequest) {
    if (!preferences) return;
    updatePreferences.mutate({ ...preferences, [key]: !preferences[key] });
  }

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
          <Switch value={pushEnabled} onValueChange={(value) => void handleMasterToggle(value)} />
        </View>

        <View className="gap-3 rounded-xl border border-border bg-card p-4" style={CARD_SHADOW}>
          <ToggleRow
            label="Recordatorios de pago"
            value={preferences?.paymentReminders ?? false}
            disabled={!pushEnabled}
            onValueChange={() => togglePreference('paymentReminders')}
          />
          <ToggleRow
            label="Alertas de sobregasto"
            value={preferences?.overspendAlerts ?? false}
            disabled={!pushEnabled}
            onValueChange={() => togglePreference('overspendAlerts')}
          />
          <ToggleRow
            label="Resumen semanal"
            value={preferences?.weeklySummary ?? false}
            disabled={!pushEnabled}
            onValueChange={() => togglePreference('weeklySummary')}
          />
          <ToggleRow
            label="Recordatorio de inactividad"
            value={preferences?.inactivityReminders ?? false}
            disabled={!pushEnabled}
            onValueChange={() => togglePreference('inactivityReminders')}
          />
        </View>
      </ScrollView>
    </SafeAreaView>
  );
}

function ToggleRow({
  label,
  value,
  disabled = false,
  onValueChange,
}: {
  label: string;
  value: boolean;
  disabled?: boolean;
  onValueChange: (value: boolean) => void;
}) {
  return (
    <View className="flex-row items-center justify-between">
      <Text className={`text-sm ${disabled ? 'text-muted-foreground' : 'text-foreground'}`}>{label}</Text>
      <Switch value={value} onValueChange={onValueChange} disabled={disabled} />
    </View>
  );
}
