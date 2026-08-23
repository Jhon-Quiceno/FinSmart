import { CameraView, useCameraPermissions } from 'expo-camera';
import { Stack, useRouter } from 'expo-router';
import { Camera, Check, RotateCcw, X } from 'lucide-react-native';
import { useRef, useState } from 'react';
import { ActivityIndicator, Pressable, ScrollView, View } from 'react-native';
import { SafeAreaView } from 'react-native-safe-area-context';

import { AppText as Text, AppTextInput as TextInput } from '@/components/app-text';
import { PressableScale } from '@/components/pressable-scale';
import { useIconColors } from '@/constants/icon-colors';
import { useCategories } from '@/hooks/use-categories';
import { useCreateExpense } from '@/hooks/use-expenses';
import { useCreateIncome } from '@/hooks/use-incomes';
import { useScanReceipt } from '@/hooks/use-scan-receipt';
import { getApiErrorMessage } from '@/lib/api-client';
import { getTodayDateInput } from '@/lib/date';
import { CARD_SHADOW } from '@/lib/shadows';
import type { CategoryType } from '@/lib/types/category';

/**
 * M1 del track móvil (docs/plan-sprints-movil-nativo.md): captura nativa de recibos.
 * Flujo: cámara → POST /api/receipts/scan → revisión editable → confirmar crea el
 * Expense/Income real vía los endpoints ya existentes (nunca se auto-crea sin que el usuario
 * confirme, mismo criterio que ya aplica el bot de Telegram).
 */
export default function EscanearReciboScreen() {
  const router = useRouter();
  const { ICON_COLOR_MUTED, ICON_COLOR_WHITE, ICON_COLOR_FOREGROUND } = useIconColors();

  const [permission, requestPermission] = useCameraPermissions();
  const cameraRef = useRef<CameraView>(null);
  const [capturedImage, setCapturedImage] = useState<{ uri: string; dataUri: string } | null>(null);
  const [captureError, setCaptureError] = useState<string | null>(null);

  const scanReceipt = useScanReceipt();

  const [movementType, setMovementType] = useState<CategoryType>('EXPENSE');
  const [description, setDescription] = useState('');
  const [amount, setAmount] = useState('');
  const [categoryId, setCategoryId] = useState<number | null>(null);
  const [formError, setFormError] = useState<string | null>(null);

  const categoriesQuery = useCategories(movementType);
  const createExpense = useCreateExpense();
  const createIncome = useCreateIncome();
  const isSaving = createExpense.isPending || createIncome.isPending;

  async function handleCapture() {
    const camera = cameraRef.current;
    if (!camera) return;

    let photo;
    try {
      photo = await camera.takePictureAsync({ base64: true, quality: 0.6 });
    } catch {
      setCaptureError('No se pudo tomar la foto. Probá de nuevo.');
      return;
    }
    if (!photo?.base64) return;

    setCaptureError(null);
    const dataUri = `data:image/jpeg;base64,${photo.base64}`;
    setCapturedImage({ uri: photo.uri, dataUri });

    try {
      const extraction = await scanReceipt.mutateAsync(dataUri);
      if (extraction.isReceipt) {
        setMovementType(extraction.movementType ?? 'EXPENSE');
        setDescription(extraction.description ?? '');
        setAmount(extraction.amount != null ? String(extraction.amount) : '');
        setCategoryId(extraction.categoryId);
      }
    } catch {
      // El error se muestra abajo leyendo scanReceipt.isError / scanReceipt.error.
    }
  }

  function handleRetake() {
    setCapturedImage(null);
    setDescription('');
    setAmount('');
    setCategoryId(null);
    setFormError(null);
    scanReceipt.reset();
  }

  async function handleConfirm() {
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

    const payload = {
      description: trimmedDescription,
      amount: parsedAmount,
      date: getTodayDateInput(),
      categoryId,
    };

    try {
      if (movementType === 'INCOME') {
        await createIncome.mutateAsync(payload);
      } else {
        await createExpense.mutateAsync({ ...payload, paymentMethod: 'OTHER' });
      }
      router.back();
    } catch (error) {
      setFormError(getApiErrorMessage(error, 'No se pudo guardar el movimiento.'));
    }
  }

  const categories = categoriesQuery.data ?? [];
  const extraction = scanReceipt.data;
  const showReviewForm = capturedImage && extraction?.isReceipt;
  const showNotAReceipt = capturedImage && extraction && !extraction.isReceipt;

  if (!capturedImage) {
    if (!permission) {
      return <View className="flex-1 bg-black" />;
    }

    if (!permission.granted) {
      return (
        <SafeAreaView className="flex-1 items-center justify-center gap-4 bg-background px-8">
          <Stack.Screen options={{ headerShown: true, title: 'Escanear recibo' }} />
          <Camera size={40} color={ICON_COLOR_MUTED} />
          <Text className="text-center text-sm text-muted-foreground">
            KoroFin necesita acceso a la cámara para leer tus recibos y cargar el gasto automáticamente.
          </Text>
          <PressableScale
            className="h-11 items-center justify-center rounded-lg bg-primary px-6"
            onPress={() => void requestPermission()}
          >
            <Text className="text-base font-medium text-primary-foreground">Permitir cámara</Text>
          </PressableScale>
        </SafeAreaView>
      );
    }

    return (
      <View className="flex-1 bg-black">
        <Stack.Screen options={{ headerShown: false }} />
        <CameraView ref={cameraRef} style={{ flex: 1 }} facing="back" />
        <SafeAreaView className="absolute inset-x-0 bottom-0">
          {captureError && (
            <View className="mx-8 mb-3 rounded-lg bg-black/60 px-3 py-2">
              <Text className="text-center text-sm text-white">{captureError}</Text>
            </View>
          )}
          <View className="flex-row items-center justify-between px-8 pb-6">
            <Pressable
              className="h-11 w-11 items-center justify-center rounded-full bg-black/40"
              onPress={() => router.back()}
              hitSlop={8}
            >
              <X size={22} color={ICON_COLOR_WHITE} />
            </Pressable>
            <Pressable
              className="h-16 w-16 items-center justify-center rounded-full border-4 border-white bg-white/20"
              onPress={() => void handleCapture()}
            >
              <View className="h-12 w-12 rounded-full bg-white" />
            </Pressable>
            <View className="h-11 w-11" />
          </View>
        </SafeAreaView>
      </View>
    );
  }

  return (
    <SafeAreaView className="flex-1 bg-background">
      <Stack.Screen options={{ headerShown: true, title: 'Revisar recibo' }} />
      <ScrollView keyboardShouldPersistTaps="handled" contentContainerClassName="gap-4 px-5 py-5">
        {scanReceipt.isPending && (
          <View className="items-center gap-3 py-10">
            <ActivityIndicator size="large" />
            <Text className="text-sm text-muted-foreground">Analizando el recibo...</Text>
          </View>
        )}

        {scanReceipt.isError && (
          <View className="gap-3">
            <View className="rounded-lg border border-destructive/20 bg-destructive/10 p-3">
              <Text className="text-sm text-destructive">
                {getApiErrorMessage(scanReceipt.error, 'No se pudo analizar la imagen.')}
              </Text>
            </View>
            <PressableScale
              className="h-11 flex-row items-center justify-center gap-2 rounded-lg border border-border bg-card"
              onPress={handleRetake}
            >
              <RotateCcw size={16} color={ICON_COLOR_FOREGROUND} />
              <Text className="text-base font-medium text-foreground">Volver a intentar</Text>
            </PressableScale>
          </View>
        )}

        {showNotAReceipt && (
          <View className="gap-3">
            <View className="rounded-lg border border-warning/20 bg-warning/10 p-3">
              <Text className="text-sm text-foreground">
                No pude leer un recibo en esa imagen. Probá con más luz o cargá el movimiento a mano.
              </Text>
            </View>
            <PressableScale
              className="h-11 flex-row items-center justify-center gap-2 rounded-lg border border-border bg-card"
              onPress={handleRetake}
            >
              <RotateCcw size={16} color={ICON_COLOR_FOREGROUND} />
              <Text className="text-base font-medium text-foreground">Volver a intentar</Text>
            </PressableScale>
          </View>
        )}

        {showReviewForm && (
          <View className="gap-4">
            <View className="flex-row gap-2">
              {(['EXPENSE', 'INCOME'] as CategoryType[]).map((type) => {
                const active = type === movementType;
                return (
                  <PressableScale
                    key={type}
                    scaleTo={0.97}
                    onPress={() => {
                      setMovementType(type);
                      setCategoryId(null);
                    }}
                    className={`flex-1 items-center rounded-lg border py-2 ${
                      active ? 'border-primary bg-primary' : 'border-border bg-background'
                    }`}
                  >
                    <Text className={`text-sm font-medium ${active ? 'text-primary-foreground' : 'text-foreground'}`}>
                      {type === 'EXPENSE' ? 'Gasto' : 'Ingreso'}
                    </Text>
                  </PressableScale>
                );
              })}
            </View>

            <View className="gap-1.5">
              <Text className="text-sm font-medium text-foreground">Descripción</Text>
              <TextInput
                className="rounded-lg border border-input bg-background px-3 py-2.5 text-base text-foreground"
                placeholder="Ej. Supermercado"
                placeholderTextColor={ICON_COLOR_MUTED}
                value={description}
                onChangeText={setDescription}
              />
            </View>

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
                  <Text className="text-xs text-muted-foreground">Sin categorías</Text>
                )}
              </ScrollView>
            </View>

            {formError && (
              <View className="rounded-lg border border-destructive/20 bg-destructive/10 p-3">
                <Text className="text-sm text-destructive">{formError}</Text>
              </View>
            )}

            <View className="flex-row gap-3">
              <PressableScale
                className="h-11 flex-1 flex-row items-center justify-center gap-2 rounded-lg border border-border bg-card"
                onPress={handleRetake}
                disabled={isSaving}
              >
                <RotateCcw size={16} color={ICON_COLOR_FOREGROUND} />
                <Text className="text-base font-medium text-foreground">Repetir foto</Text>
              </PressableScale>
              <PressableScale
                className="h-11 flex-1 flex-row items-center justify-center gap-2 rounded-lg bg-primary"
                style={CARD_SHADOW}
                onPress={() => void handleConfirm()}
                disabled={isSaving}
              >
                {isSaving ? (
                  <ActivityIndicator color="white" />
                ) : (
                  <>
                    <Check size={16} color={ICON_COLOR_WHITE} />
                    <Text className="text-base font-medium text-primary-foreground">Confirmar</Text>
                  </>
                )}
              </PressableScale>
            </View>
          </View>
        )}
      </ScrollView>
    </SafeAreaView>
  );
}
