import { useRouter } from 'expo-router';
import { Bell } from 'lucide-react-native';
import { View } from 'react-native';

import { PressableScale } from '@/components/pressable-scale';
import { useIconColors } from '@/constants/icon-colors';

// Mock: reemplazar por el conteo real de notificaciones sin leer del feed (TODO Fase 0 backend).
const mockUnreadNotificationsCount = 2;

/**
 * Campana de notificaciones del header — distinta de la burbuja de perfil. Lleva al centro de
 * notificaciones (feed histórico de alertas ya generadas), no a las preferencias de push.
 */
export function NotificationBell() {
  const router = useRouter();
  const { ICON_COLOR_MUTED } = useIconColors();

  return (
    <PressableScale
      onPress={() => router.push('/centro-notificaciones')}
      className="h-10 w-10 items-center justify-center rounded-full bg-secondary"
      hitSlop={6}
    >
      <Bell size={18} color={ICON_COLOR_MUTED} />
      {mockUnreadNotificationsCount > 0 && (
        <View className="absolute right-2 top-2 h-2 w-2 rounded-full bg-destructive" />
      )}
    </PressableScale>
  );
}
