import { Link, Stack, type Href } from 'expo-router';
import {
  Bell,
  Bot,
  ChevronRight,
  LogOut,
  Mail,
  MessageCircle,
  PieChart,
  Settings,
  Shield,
  User,
  type LucideIcon,
} from 'lucide-react-native';
import { Pressable, ScrollView, View } from 'react-native';

import { AppText as Text } from '@/components/app-text';
import { SafeAreaView } from 'react-native-safe-area-context';

import { PressableScale } from '@/components/pressable-scale';
import { useIconColors } from '@/constants/icon-colors';
import { mockAiProviders, mockNotificationPreferences, mockUser } from '@/lib/mock/configuracion';
import { CARD_SHADOW } from '@/lib/shadows';

const NOTIFICATION_LABELS: { key: keyof typeof mockNotificationPreferences; label: string }[] = [
  { key: 'paymentReminders', label: 'Recordatorios de pago' },
  { key: 'overspendAlerts', label: 'Alertas de sobregasto' },
  { key: 'weeklySummary', label: 'Resumen semanal' },
  { key: 'inactivityReminders', label: 'Recordatorio de inactividad' },
];

const CONFIG_MENU_ITEMS: { href: Href; label: string; description: string; icon: LucideIcon }[] = [
  { href: '/reportes', label: 'Reportes', description: 'Comparativas mensuales', icon: PieChart },
  { href: '/telegram', label: 'Vínculo con Telegram', description: 'Registrá gastos desde el bot', icon: MessageCircle },
  { href: '/preferencias', label: 'Preferencias', description: 'Tema, moneda e idioma', icon: Settings },
  { href: '/notificaciones', label: 'Notificaciones', description: 'Activá alertas y elegí qué recibir', icon: Bell },
];

export default function ConfiguracionScreen() {
  const { ICON_COLOR_DESTRUCTIVE, ICON_COLOR_MUTED } = useIconColors();
  return (
    <SafeAreaView className="flex-1 bg-background">
      <Stack.Screen options={{ headerShown: true, title: 'Configuración' }} />
      <ScrollView contentContainerClassName="gap-5 px-5 py-6" showsVerticalScrollIndicator={false}>
        <View className="items-center gap-3 rounded-xl border border-border bg-card p-6" style={CARD_SHADOW}>
          <View className="h-20 w-20 items-center justify-center rounded-full bg-primary/15">
            <User size={32} color={ICON_COLOR_MUTED} />
          </View>
          <View className="items-center">
            <Text className="text-base font-semibold text-foreground">{mockUser.name}</Text>
            <View className="mt-1 flex-row items-center gap-1.5">
              <Mail size={12} color={ICON_COLOR_MUTED} />
              <Text className="text-xs text-muted-foreground">{mockUser.email}</Text>
            </View>
          </View>
          {/* TODO(Fase 0 backend): edición real de perfil contra PUT /api/users/me. */}
          <PressableScale className="mt-1 rounded-lg border border-border px-4 py-2">
            <Text className="text-xs font-medium text-foreground">Editar perfil</Text>
          </PressableScale>
        </View>

        <View className="overflow-hidden rounded-xl border border-border bg-card" style={CARD_SHADOW}>
          {CONFIG_MENU_ITEMS.map((item, index) => {
            const Icon = item.icon;
            return (
              <Link key={item.label} href={item.href} asChild>
                <Pressable
                  className={`flex-row items-center gap-3 px-4 py-3.5 ${
                    index < CONFIG_MENU_ITEMS.length - 1 ? 'border-b border-border' : ''
                  }`}
                >
                  <View className="h-9 w-9 items-center justify-center rounded-full bg-secondary">
                    <Icon size={16} color={ICON_COLOR_MUTED} />
                  </View>
                  <View className="flex-1">
                    <Text className="text-sm font-medium text-foreground">{item.label}</Text>
                    <Text className="text-xs text-muted-foreground">{item.description}</Text>
                  </View>
                  <ChevronRight size={16} color={ICON_COLOR_MUTED} />
                </Pressable>
              </Link>
            );
          })}
        </View>

        <View className="rounded-xl border border-border bg-card p-5" style={CARD_SHADOW}>
          <Text className="mb-3 text-base font-semibold text-card-foreground">Preferencias de notificación</Text>
          <View className="gap-2.5">
            {NOTIFICATION_LABELS.map(({ key, label }) => (
              <View key={key} className="flex-row items-center justify-between">
                <Text className="text-sm text-foreground">{label}</Text>
                <View className={`rounded-full px-2.5 py-0.5 ${mockNotificationPreferences[key] ? 'bg-success/15' : 'bg-muted'}`}>
                  <Text className={`text-[10px] font-medium ${mockNotificationPreferences[key] ? 'text-success' : 'text-muted-foreground'}`}>
                    {mockNotificationPreferences[key] ? 'Activo' : 'Inactivo'}
                  </Text>
                </View>
              </View>
            ))}
          </View>
        </View>

        <View className="rounded-xl border border-border bg-card p-5" style={CARD_SHADOW}>
          <View className="mb-3 flex-row items-center gap-2">
            <Bot size={16} color={ICON_COLOR_MUTED} />
            <Text className="text-base font-semibold text-card-foreground">Proveedores de IA</Text>
          </View>
          <View className="gap-2.5">
            {mockAiProviders.map((provider) => (
              <View key={provider.name} className="flex-row items-center justify-between">
                <Text className="text-sm text-foreground">{provider.name}</Text>
                <View className={`rounded-full px-2.5 py-0.5 ${provider.active ? 'bg-success/15' : 'bg-muted'}`}>
                  <Text className={`text-[10px] font-medium ${provider.active ? 'text-success' : 'text-muted-foreground'}`}>
                    {provider.active ? 'En uso' : 'Disponible'}
                  </Text>
                </View>
              </View>
            ))}
          </View>
          <Text className="mt-3 text-xs text-muted-foreground">Solo lectura — se administra desde el backend</Text>
        </View>

        <PressableScale
          className="flex-row items-center justify-between rounded-xl border border-border bg-card px-5 py-4"
          style={CARD_SHADOW}
        >
          <View className="flex-row items-center gap-2.5">
            <Shield size={16} color={ICON_COLOR_MUTED} />
            <Text className="text-sm text-foreground">Cambiar contraseña</Text>
          </View>
          <ChevronRight size={16} color={ICON_COLOR_MUTED} />
        </PressableScale>

        <PressableScale className="flex-row items-center justify-center gap-2 rounded-xl border border-destructive/30 bg-destructive/10 px-5 py-4">
          <LogOut size={16} color={ICON_COLOR_DESTRUCTIVE} />
          <Text className="text-sm font-medium text-destructive">Cerrar sesión</Text>
        </PressableScale>
      </ScrollView>
    </SafeAreaView>
  );
}
