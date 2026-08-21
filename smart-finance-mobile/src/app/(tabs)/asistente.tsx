import { Bot, Send } from 'lucide-react-native';
import { useState } from 'react';
import { ActivityIndicator, FlatList, KeyboardAvoidingView, Platform, View } from 'react-native';

import { AppText as Text, AppTextInput as TextInput } from '@/components/app-text';
import { SafeAreaView } from 'react-native-safe-area-context';

import { NotificationBell } from '@/components/notification-bell';
import { PressableScale } from '@/components/pressable-scale';
import { ProfileBubble } from '@/components/profile-bubble';
import { useIconColors } from '@/constants/icon-colors';
import { getChatSendErrorMessage, useChatHistory, useSendChatMessage } from '@/hooks/use-ai-chat';
import { CARD_SHADOW } from '@/lib/shadows';
import type { ChatMessage } from '@/lib/types/ai';

function Bubble({ message }: { message: ChatMessage }) {
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
  const [sendError, setSendError] = useState<string | null>(null);
  const { ICON_COLOR_MUTED, ICON_COLOR_WHITE } = useIconColors();

  const { data: history } = useChatHistory();
  const sendMessage = useSendChatMessage();
  const messages = history?.content ?? [];

  async function handleSend() {
    const trimmed = draft.trim();
    if (!trimmed || sendMessage.isPending) return;

    setDraft('');
    setSendError(null);
    try {
      await sendMessage.mutateAsync(trimmed);
    } catch (error) {
      setSendError(getChatSendErrorMessage(error));
      setDraft(trimmed);
    }
  }

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
          data={messages}
          keyExtractor={(item) => String(item.id)}
          renderItem={({ item }) => <Bubble message={item} />}
          contentContainerClassName="px-5 py-4"
          // El historial viene DESC (más nuevo primero, ver AiChatController#getHistory) — con
          // `inverted` el índice 0 se dibuja abajo, que es justo el mensaje más reciente.
          inverted
        />

        {sendError && (
          <Text className="px-5 pb-1 text-xs text-destructive">{sendError}</Text>
        )}

        <View className="flex-row items-center gap-2 border-t border-border px-4 py-3">
          <TextInput
            className="flex-1 rounded-full border border-input bg-background px-4 py-2.5 text-sm text-foreground"
            style={CARD_SHADOW}
            placeholder="Preguntale algo a tu asistente..."
            placeholderTextColor={ICON_COLOR_MUTED}
            value={draft}
            onChangeText={setDraft}
            editable={!sendMessage.isPending}
          />
          <PressableScale
            className="h-10 w-10 items-center justify-center rounded-full bg-primary"
            hitSlop={4}
            onPress={() => void handleSend()}
            disabled={sendMessage.isPending || draft.trim().length === 0}
          >
            {sendMessage.isPending ? (
              <ActivityIndicator size="small" color={ICON_COLOR_WHITE} />
            ) : (
              <Send size={16} color={ICON_COLOR_WHITE} />
            )}
          </PressableScale>
        </View>
      </KeyboardAvoidingView>
    </SafeAreaView>
  );
}
