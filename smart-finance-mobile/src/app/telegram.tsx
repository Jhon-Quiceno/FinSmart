import { Stack } from 'expo-router';
import { MessageCircle } from 'lucide-react-native';
import { useState } from 'react';
import { ScrollView, View } from 'react-native';

import { AppText as Text } from '@/components/app-text';
import { SafeAreaView } from 'react-native-safe-area-context';

import { PressableScale } from '@/components/pressable-scale';
import { useIconColors } from '@/constants/icon-colors';
import { CARD_SHADOW } from '@/lib/shadows';

function generateMockCode(): string {
  return Math.random().toString(36).slice(2, 8).toUpperCase();
}

export default function TelegramScreen() {
  const [code, setCode] = useState<string | null>(null);
  const { ICON_COLOR_PRIMARY } = useIconColors();

  // TODO(Fase 0 backend): reemplazar por POST al mismo endpoint que usa
  // TelegramLinkService.generateLinkCode en la web — hoy genera un código mock local.
  const handleGenerate = () => setCode(generateMockCode());

  return (
    <SafeAreaView className="flex-1 bg-background">
      <Stack.Screen options={{ headerShown: true, title: 'Vínculo con Telegram' }} />
      <ScrollView contentContainerClassName="flex-grow items-center gap-6 px-6 py-10">
        <View className="h-16 w-16 items-center justify-center rounded-full bg-primary/15">
          <MessageCircle size={28} color={ICON_COLOR_PRIMARY} />
        </View>

        <View className="items-center gap-2">
          <Text className="text-center text-lg font-bold text-foreground">
            Registrá gastos desde Telegram
          </Text>
          <Text className="text-center text-sm text-muted-foreground">
            Generá un código de un solo uso y enviáselo al bot de KoroFin para vincular tu
            cuenta. Después vas a poder registrar gastos y enviar fotos de recibos por chat.
          </Text>
        </View>

        {code ? (
          <View className="w-full items-center gap-2 rounded-xl border border-border bg-card p-6" style={CARD_SHADOW}>
            <Text className="text-xs text-muted-foreground">Tu código de vinculación</Text>
            <Text className="text-3xl font-bold tracking-widest text-primary">{code}</Text>
            <Text className="mt-2 text-center text-xs text-muted-foreground">
              Enviaselo al bot @KoroFinBot en Telegram antes de que expire (10 minutos)
            </Text>
          </View>
        ) : (
          <PressableScale onPress={handleGenerate} className="w-full items-center rounded-lg bg-primary py-3.5">
            <Text className="text-base font-medium text-primary-foreground">Generar código</Text>
          </PressableScale>
        )}

        {code && (
          <PressableScale onPress={handleGenerate} className="flex-row items-center gap-2">
            <Text className="text-sm font-medium text-primary">Generar otro código</Text>
          </PressableScale>
        )}
      </ScrollView>
    </SafeAreaView>
  );
}
