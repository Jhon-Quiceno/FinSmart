import { useRouter, type Href } from 'expo-router';
import { Settings, User, type LucideIcon } from 'lucide-react-native';
import { useState } from 'react';
import { Modal, Pressable, View } from 'react-native';

import { AppText as Text } from '@/components/app-text';
import { useSafeAreaInsets } from 'react-native-safe-area-context';

import { useIconColors } from '@/constants/icon-colors';

const MENU_ITEMS: { key: string; label: string; icon: LucideIcon; href: Href }[] = [
  { key: 'perfil', label: 'Ver perfil', icon: User, href: '/configuracion' },
  { key: 'config', label: 'Configuraciones', icon: Settings, href: '/configuracion' },
];

export function ProfileBubble() {
  const [open, setOpen] = useState(false);
  const insets = useSafeAreaInsets();
  const router = useRouter();
  const { ICON_COLOR_MUTED } = useIconColors();

  return (
    <>
      <Pressable
        onPress={() => setOpen(true)}
        className="h-9 w-9 items-center justify-center rounded-full border border-primary bg-primary/15"
        hitSlop={6}
      >
        <Text className="text-xs font-bold text-primary">JQ</Text>
      </Pressable>

      <Modal visible={open} transparent animationType="fade" onRequestClose={() => setOpen(false)}>
        <Pressable
          className="flex-1"
          style={{ backgroundColor: 'rgba(20,24,31,0.4)' }}
          onPress={() => setOpen(false)}
        >
          <View
            className="absolute right-5 w-72 overflow-hidden rounded-xl border border-border bg-card"
            style={{ top: insets.top + 54 }}
          >
            {MENU_ITEMS.map((item, index) => {
              const Icon = item.icon;
              return (
                <Pressable
                  key={item.key}
                  onPress={() => {
                    setOpen(false);
                    router.push(item.href);
                  }}
                  className={`flex-row items-center gap-2.5 px-5 py-4 ${
                    index < MENU_ITEMS.length - 1 ? 'border-b border-border' : ''
                  }`}
                >
                  <View className="h-8 w-8 items-center justify-center rounded-full bg-secondary">
                    <Icon size={14} color={ICON_COLOR_MUTED} />
                  </View>
                  <Text className="text-sm font-medium text-foreground">{item.label}</Text>
                </Pressable>
              );
            })}
          </View>
        </Pressable>
      </Modal>
    </>
  );
}
