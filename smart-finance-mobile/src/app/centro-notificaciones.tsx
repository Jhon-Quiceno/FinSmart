import { Stack } from 'expo-router';
import { AlertTriangle, CheckCircle, Info } from 'lucide-react-native';
import { ScrollView, View } from 'react-native';

import { AppText as Text } from '@/components/app-text';
import { SafeAreaView } from 'react-native-safe-area-context';

import { useIconColors } from '@/constants/icon-colors';
import { mockNotificationsFeed, type NotificationSeverity } from '@/lib/mock/notificaciones-feed';
import { CARD_SHADOW } from '@/lib/shadows';

const SEVERITY_ICON = { HIGH: AlertTriangle, MEDIUM: Info, LOW: CheckCircle } as const;
const SEVERITY_BG = {
  HIGH: 'border-destructive/20 bg-destructive/10',
  MEDIUM: 'border-warning/20 bg-warning/10',
  LOW: 'border-success/20 bg-success/10',
} as const;

export default function CentroNotificacionesScreen() {
  const { ICON_COLOR_DESTRUCTIVE, ICON_COLOR_WARNING, ICON_COLOR_SUCCESS } = useIconColors();
  const SEVERITY_COLOR: Record<NotificationSeverity, string> = {
    HIGH: ICON_COLOR_DESTRUCTIVE,
    MEDIUM: ICON_COLOR_WARNING,
    LOW: ICON_COLOR_SUCCESS,
  };

  return (
    <SafeAreaView className="flex-1 bg-background">
      <Stack.Screen options={{ headerShown: true, title: 'Notificaciones' }} />
      <ScrollView contentContainerClassName="gap-3 px-5 py-6" showsVerticalScrollIndicator={false}>
        {mockNotificationsFeed.map((item) => {
          const Icon = SEVERITY_ICON[item.severity];
          return (
            <View
              key={item.id}
              className={`flex-row items-start gap-3 rounded-xl border p-4 ${
                item.read ? 'border-border bg-card' : 'border-primary/30 bg-primary/5'
              }`}
              style={CARD_SHADOW}
            >
              <View className={`h-10 w-10 items-center justify-center rounded-full border ${SEVERITY_BG[item.severity]}`}>
                <Icon size={18} color={SEVERITY_COLOR[item.severity]} />
              </View>
              <View className="flex-1 gap-0.5">
                <View className="flex-row items-center gap-2">
                  <Text className="flex-1 text-sm font-semibold text-foreground">{item.title}</Text>
                  {!item.read && <View className="h-2 w-2 rounded-full bg-primary" />}
                </View>
                <Text className="text-sm text-muted-foreground">{item.description}</Text>
                <Text className="mt-1 text-xs text-muted-foreground">{item.timestamp}</Text>
              </View>
            </View>
          );
        })}
      </ScrollView>
    </SafeAreaView>
  );
}
