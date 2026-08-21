import { Stack } from 'expo-router';
import { AlertTriangle, CheckCircle, Info, type LucideIcon } from 'lucide-react-native';
import { ScrollView, View } from 'react-native';

import { AppText as Text } from '@/components/app-text';
import { SafeAreaView } from 'react-native-safe-area-context';

import { PressableScale } from '@/components/pressable-scale';
import { useIconColors } from '@/constants/icon-colors';
import { useMarkAllAsRead, useMarkAsRead, useNotifications } from '@/hooks/use-notifications';
import { formatRelativeTime } from '@/lib/format';
import { CARD_SHADOW } from '@/lib/shadows';
import type { NotificationType } from '@/lib/types/notification';

// El tipo real `Notification` (ver src/lib/types/notification.ts) no tiene un campo `severity`
// como el mock (`mockNotificationsFeed`) — solo `type`. Se deriva la urgencia visual a partir
// del `type` real en vez de inventar un campo que el backend no manda.
const TYPE_ICON: Record<NotificationType, LucideIcon> = {
  OVERSPEND_ALERT: AlertTriangle,
  PAYMENT_REMINDER: AlertTriangle,
  MONTH_END_PREDICTION: Info,
  WEEKLY_SUMMARY: Info,
  INACTIVITY_REMINDER: Info,
  SYSTEM: CheckCircle,
};

const TYPE_BG: Record<NotificationType, string> = {
  OVERSPEND_ALERT: 'border-destructive/20 bg-destructive/10',
  PAYMENT_REMINDER: 'border-warning/20 bg-warning/10',
  MONTH_END_PREDICTION: 'border-warning/20 bg-warning/10',
  WEEKLY_SUMMARY: 'border-success/20 bg-success/10',
  INACTIVITY_REMINDER: 'border-success/20 bg-success/10',
  SYSTEM: 'border-success/20 bg-success/10',
};

export default function CentroNotificacionesScreen() {
  const { ICON_COLOR_DESTRUCTIVE, ICON_COLOR_WARNING, ICON_COLOR_SUCCESS } = useIconColors();
  const { data } = useNotifications();
  const markAsRead = useMarkAsRead();
  const markAllAsRead = useMarkAllAsRead();

  const TYPE_COLOR: Record<NotificationType, string> = {
    OVERSPEND_ALERT: ICON_COLOR_DESTRUCTIVE,
    PAYMENT_REMINDER: ICON_COLOR_WARNING,
    MONTH_END_PREDICTION: ICON_COLOR_WARNING,
    WEEKLY_SUMMARY: ICON_COLOR_SUCCESS,
    INACTIVITY_REMINDER: ICON_COLOR_SUCCESS,
    SYSTEM: ICON_COLOR_SUCCESS,
  };

  const notifications = data?.content ?? [];
  const hasUnread = notifications.some((item) => !item.read);

  return (
    <SafeAreaView className="flex-1 bg-background">
      <Stack.Screen options={{ headerShown: true, title: 'Notificaciones' }} />
      <ScrollView contentContainerClassName="gap-3 px-5 py-6" showsVerticalScrollIndicator={false}>
        {hasUnread && (
          <PressableScale
            className="self-end"
            onPress={() => markAllAsRead.mutate()}
            disabled={markAllAsRead.isPending}
          >
            <Text className="text-xs font-medium text-primary">Marcar todas como leídas</Text>
          </PressableScale>
        )}
        {notifications.map((item) => {
          const Icon = TYPE_ICON[item.type];
          return (
            <PressableScale
              key={item.id}
              onPress={() => !item.read && markAsRead.mutate(item.id)}
              className={`flex-row items-start gap-3 rounded-xl border p-4 ${
                item.read ? 'border-border bg-card' : 'border-primary/30 bg-primary/5'
              }`}
              style={CARD_SHADOW}
            >
              <View className={`h-10 w-10 items-center justify-center rounded-full border ${TYPE_BG[item.type]}`}>
                <Icon size={18} color={TYPE_COLOR[item.type]} />
              </View>
              <View className="flex-1 gap-0.5">
                <View className="flex-row items-center gap-2">
                  <Text className="flex-1 text-sm font-semibold text-foreground">{item.title}</Text>
                  {!item.read && <View className="h-2 w-2 rounded-full bg-primary" />}
                </View>
                <Text className="text-sm text-muted-foreground">{item.message}</Text>
                <Text className="mt-1 text-xs text-muted-foreground">{formatRelativeTime(item.createdAt)}</Text>
              </View>
            </PressableScale>
          );
        })}
      </ScrollView>
    </SafeAreaView>
  );
}
