import { Sparkles, X } from 'lucide-react-native';
import { useState } from 'react';
import { ActivityIndicator, Modal, Pressable, ScrollView, View } from 'react-native';
import { useSafeAreaInsets } from 'react-native-safe-area-context';

import { AppText as Text, AppTextInput as TextInput } from '@/components/app-text';
import { PressableScale } from '@/components/pressable-scale';
import { useIconColors } from '@/constants/icon-colors';
import { useAiCategorize } from '@/hooks/use-ai-categorize';
import { useCategories } from '@/hooks/use-categories';
import { useCreateExpense } from '@/hooks/use-expenses';
import { getApiErrorMessage } from '@/lib/api-client';
import { PAYMENT_METHODS, type PaymentMethodType } from '@/lib/types/expense';
import { CARD_SHADOW } from '@/lib/shadows';

const PAYMENT_METHOD_LABELS: Record<PaymentMethodType, string> = {
  CASH: 'Efectivo',
  DEBIT_CARD: 'Débito',
  CREDIT_CARD: 'Crédito',
  TRANSFER: 'Transferencia',
  OTHER: 'Otro',
};

interface QuickAddExpenseModalProps {
  visible: boolean;
  onClose: () => void;
}

/**
 * Alta rápida de un gasto desde el FAB de Movimientos. Reutiliza el patrón de Modal transparente
 * de ProfileBubble en vez de inventar uno nuevo. Al perder foco la descripción, sugiere una
 * categoría vía POST /api/ai/categorize (useAiCategorize) — el usuario decide si la aplica.
 */
export function QuickAddExpenseModal({ visible, onClose }: QuickAddExpenseModalProps) {
  const insets = useSafeAreaInsets();
  const { ICON_COLOR_MUTED, ICON_COLOR_SUCCESS } = useIconColors();

  const [description, setDescription] = useState('');
  const [amount, setAmount] = useState('');
  const [categoryId, setCategoryId] = useState<number | null>(null);
  const [paymentMethod, setPaymentMethod] = useState<PaymentMethodType | null>(null);
  const [formError, setFormError] = useState<string | null>(null);

  const categoriesQuery = useCategories('EXPENSE');
  const createExpense = useCreateExpense();
  const aiCategorize = useAiCategorize();

  function reset() {
    setDescription('');
    setAmount('');
    setCategoryId(null);
    setPaymentMethod(null);
    setFormError(null);
    aiCategorize.reset();
  }

  function handleClose() {
    reset();
    onClose();
  }

  async function handleDescriptionBlur() {
    const trimmed = description.trim();
    if (!trimmed || aiCategorize.isPending) return;

    const parsedAmount = Number(amount);
    const result = await aiCategorize.mutateAsync({
      description: trimmed,
      amount: Number.isFinite(parsedAmount) && parsedAmount > 0 ? parsedAmount : undefined,
      type: 'EXPENSE',
    });

    if (result.categoryId !== null && categoryId === null) {
      setCategoryId(result.categoryId);
    }
  }

  async function handleSubmit() {
    setFormError(null);

    const trimmedDescription = description.trim();
    const parsedAmount = Number(amount);

    if (!trimmedDescription) {
      setFormError('La descripción es obligatoria.');
      return;
    }
    if (!Number.isFinite(parsedAmount) || parsedAmount <= 0) {
      setFormError('Ingresá un monto válido.');
      return;
    }
    if (!paymentMethod) {
      setFormError('Elegí un método de pago.');
      return;
    }

    try {
      await createExpense.mutateAsync({
        description: trimmedDescription,
        amount: parsedAmount,
        date: new Date().toISOString().slice(0, 10),
        paymentMethod,
        categoryId,
      });
      handleClose();
    } catch (error) {
      setFormError(getApiErrorMessage(error, 'No se pudo guardar el gasto.'));
    }
  }

  const categories = categoriesQuery.data ?? [];
  const suggestedCategory =
    aiCategorize.data?.categoryId !== undefined && aiCategorize.data?.categoryId !== null
      ? categories.find((c) => c.id === aiCategorize.data?.categoryId)
      : null;

  return (
    <Modal visible={visible} transparent animationType="slide" onRequestClose={handleClose}>
      <Pressable className="flex-1 justify-end" style={{ backgroundColor: 'rgba(20,24,31,0.4)' }} onPress={handleClose}>
        <Pressable
          className="rounded-t-2xl border border-border bg-card p-5"
          style={[CARD_SHADOW, { paddingBottom: insets.bottom + 20 }]}
          onPress={(e) => e.stopPropagation()}
        >
          <ScrollView keyboardShouldPersistTaps="handled" showsVerticalScrollIndicator={false}>
            <View className="mb-4 flex-row items-center justify-between">
              <Text className="text-lg font-bold text-foreground">Nuevo gasto</Text>
              <Pressable onPress={handleClose} hitSlop={8}>
                <X size={20} color={ICON_COLOR_MUTED} />
              </Pressable>
            </View>

            <View className="gap-4">
              <View className="gap-1.5">
                <Text className="text-sm font-medium text-foreground">Descripción</Text>
                <TextInput
                  className="rounded-lg border border-input bg-background px-3 py-2.5 text-base text-foreground"
                  placeholder="Ej. Supermercado"
                  placeholderTextColor={ICON_COLOR_MUTED}
                  value={description}
                  onChangeText={setDescription}
                  onBlur={() => void handleDescriptionBlur()}
                />
              </View>

              {aiCategorize.isPending && (
                <View className="flex-row items-center gap-2">
                  <ActivityIndicator size="small" />
                  <Text className="text-xs text-muted-foreground">Buscando categoría sugerida...</Text>
                </View>
              )}

              {suggestedCategory && suggestedCategory.id !== categoryId && (
                <PressableScale
                  onPress={() => setCategoryId(suggestedCategory.id)}
                  className="flex-row items-center gap-2 rounded-lg border border-primary/30 bg-primary/10 p-3"
                >
                  <Sparkles size={16} color={ICON_COLOR_SUCCESS} />
                  <Text className="flex-1 text-xs text-foreground">
                    Sugerencia IA: <Text className="font-semibold">{suggestedCategory.name}</Text>
                  </Text>
                  <Text className="text-xs font-semibold text-primary">Usar</Text>
                </PressableScale>
              )}

              <View className="gap-1.5">
                <Text className="text-sm font-medium text-foreground">Monto</Text>
                <TextInput
                  className="rounded-lg border border-input bg-background px-3 py-2.5 text-base text-foreground"
                  placeholder="0"
                  placeholderTextColor={ICON_COLOR_MUTED}
                  keyboardType="numeric"
                  value={amount}
                  onChangeText={setAmount}
                />
              </View>

              <View className="gap-1.5">
                <Text className="text-sm font-medium text-foreground">Categoría</Text>
                <ScrollView horizontal showsHorizontalScrollIndicator={false} contentContainerClassName="gap-2">
                  {categories.map((category) => {
                    const active = category.id === categoryId;
                    return (
                      <PressableScale
                        key={category.id}
                        scaleTo={0.97}
                        onPress={() => setCategoryId(active ? null : category.id)}
                        className={`rounded-full border px-3 py-1.5 ${
                          active ? 'border-primary bg-primary' : 'border-border bg-background'
                        }`}
                      >
                        <Text className={`text-xs font-medium ${active ? 'text-primary-foreground' : 'text-foreground'}`}>
                          {category.name}
                        </Text>
                      </PressableScale>
                    );
                  })}
                  {categories.length === 0 && !categoriesQuery.isPending && (
                    <Text className="text-xs text-muted-foreground">Sin categorías de gasto</Text>
                  )}
                </ScrollView>
              </View>

              <View className="gap-1.5">
                <Text className="text-sm font-medium text-foreground">Método de pago</Text>
                <ScrollView horizontal showsHorizontalScrollIndicator={false} contentContainerClassName="gap-2">
                  {PAYMENT_METHODS.map((method) => {
                    const active = method === paymentMethod;
                    return (
                      <PressableScale
                        key={method}
                        scaleTo={0.97}
                        onPress={() => setPaymentMethod(method)}
                        className={`rounded-full border px-3 py-1.5 ${
                          active ? 'border-primary bg-primary' : 'border-border bg-background'
                        }`}
                      >
                        <Text className={`text-xs font-medium ${active ? 'text-primary-foreground' : 'text-foreground'}`}>
                          {PAYMENT_METHOD_LABELS[method]}
                        </Text>
                      </PressableScale>
                    );
                  })}
                </ScrollView>
              </View>

              {formError && (
                <View className="rounded-lg border border-destructive/20 bg-destructive/10 p-3">
                  <Text className="text-sm text-destructive">{formError}</Text>
                </View>
              )}

              <PressableScale
                className="h-11 flex-row items-center justify-center rounded-lg bg-primary"
                onPress={() => void handleSubmit()}
                disabled={createExpense.isPending}
              >
                {createExpense.isPending ? (
                  <ActivityIndicator color="white" />
                ) : (
                  <Text className="text-base font-medium text-primary-foreground">Guardar gasto</Text>
                )}
              </PressableScale>
            </View>
          </ScrollView>
        </Pressable>
      </Pressable>
    </Modal>
  );
}
