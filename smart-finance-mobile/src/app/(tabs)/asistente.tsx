import { Bot, Send } from 'lucide-react-native';
import { useState } from 'react';
import { FlatList, KeyboardAvoidingView, Platform, View } from 'react-native';

import { AppText as Text, AppTextInput as TextInput } from '@/components/app-text';
import { SafeAreaView } from 'react-native-safe-area-context';

import { NotificationBell } from '@/components/notification-bell';
import { PressableScale } from '@/components/pressable-scale';
import { ProfileBubble } from '@/components/profile-bubble';
import { useIconColors } from '@/constants/icon-colors';
import { mockChatMessages, type MockChatMessage } from '@/lib/mock/chat';
import { CARD_SHADOW } from '@/lib/shadows';

function Bubble({ message }: { message: MockChatMessage }) {
  const isUser = message.role === 'USER';
  return (
    <View className={`mb-3 max-w-[85%] ${isUser ? 'self-end' : 'self-start'}`}>
      <View
        className={`rounded-2xl px-4 py-2.5 ${isUser ? 'rounded-br-sm bg-primary' : 'rounded-bl-sm bg-secondary'}`}
        style={CARD_SHADOW}
      >
        <Text className={`text-sm ${isUser ? 'text-primary-foreground' : 'text-secondary-foreground'}`}>
          {message.content}
        </Text>
      </View>
    </View>
  );
}

export default function AsistenteScreen() {
  const [draft, setDraft] = useState('');
  const { ICON_COLOR_MUTED, ICON_COLOR_WHITE } = useIconColors();

  // TODO(Fase 0 backend): reemplazar por POST /api/ai/chat y agregar el mensaje a la lista
  // con la respuesta real — hoy el input no envía nada, es solo el diseño de la pantalla.
  return (
    <SafeAreaView className="flex-1 bg-background" edges={['top']}>
      <View className="flex-row items-center justify-between border-b border-border px-5 pb-3 pt-6">
        <View className="flex-row items-center gap-2">
          <Bot size={20} color={ICON_COLOR_MUTED} />
          <Text className="text-lg font-bold text-foreground">Asistente IA</Text>
        </View>
        <View className="flex-row items-center gap-2">
          <NotificationBell />
          <ProfileBubble />
        </View>
      </View>

      <KeyboardAvoidingView
        className="flex-1"
        behavior={Platform.OS === 'ios' ? 'padding' : undefined}
        keyboardVerticalOffset={90}
      >
        <FlatList
          data={mockChatMessages}
          keyExtractor={(item) => String(item.id)}
          renderItem={({ item }) => <Bubble message={item} />}
          contentContainerClassName="px-5 py-4"
          inverted={false}
        />

        <View className="flex-row items-center gap-2 border-t border-border px-4 py-3">
          <TextInput
            className="flex-1 rounded-full border border-input bg-background px-4 py-2.5 text-sm text-foreground"
            style={CARD_SHADOW}
            placeholder="Preguntale algo a tu asistente..."
            placeholderTextColor={ICON_COLOR_MUTED}
            value={draft}
            onChangeText={setDraft}
          />
          <PressableScale className="h-10 w-10 items-center justify-center rounded-full bg-primary" hitSlop={4}>
            <Send size={16} color={ICON_COLOR_WHITE} />
          </PressableScale>
        </View>
      </KeyboardAvoidingView>
    </SafeAreaView>
  );
}
