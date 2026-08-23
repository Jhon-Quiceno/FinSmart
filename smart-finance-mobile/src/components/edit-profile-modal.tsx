import { zodResolver } from '@hookform/resolvers/zod';
import { X } from 'lucide-react-native';
import { useEffect, useState } from 'react';
import { Controller, useForm } from 'react-hook-form';
import { ActivityIndicator, Modal, Pressable, ScrollView, View } from 'react-native';
import { useSafeAreaInsets } from 'react-native-safe-area-context';

import { AppText as Text, AppTextInput as TextInput } from '@/components/app-text';
import { PressableScale } from '@/components/pressable-scale';
import { useIconColors } from '@/constants/icon-colors';
import { useAuth } from '@/context/auth-context';
import { getApiErrorMessage } from '@/lib/api-client';
import { profileSchema, type ProfileFormValues } from '@/lib/schemas/user.schema';
import { updateProfile } from '@/lib/services/user.service';
import { CARD_SHADOW } from '@/lib/shadows';

interface EditProfileModalProps {
  visible: boolean;
  onClose: () => void;
}

/**
 * Edición real de perfil contra PUT /api/users/profile. Reutiliza el patrón de Modal
 * transparente + bottom sheet de QuickAddExpenseModal en vez de inventar uno nuevo, y el mismo
 * patrón de formulario (React Hook Form + Zod, error de envío en un View aparte) que login/register.
 */
export function EditProfileModal({ visible, onClose }: EditProfileModalProps) {
  const insets = useSafeAreaInsets();
  const { ICON_COLOR_MUTED } = useIconColors();
  const { user, updateUser } = useAuth();
  const [submitError, setSubmitError] = useState<string | null>(null);

  const {
    control,
    handleSubmit,
    reset,
    formState: { errors, isSubmitting },
  } = useForm<ProfileFormValues>({
    resolver: zodResolver(profileSchema),
    defaultValues: { name: user?.name ?? '', email: user?.email ?? '' },
  });

  // Cada vez que se abre el modal, resetea el form con el usuario actual — evita arrastrar
  // ediciones sin guardar de una apertura anterior o mostrar datos viejos tras otra edición.
  useEffect(() => {
    if (visible) {
      setSubmitError(null);
      reset({ name: user?.name ?? '', email: user?.email ?? '' });
    }
  }, [visible, user, reset]);

  const onSubmit = async (values: ProfileFormValues) => {
    setSubmitError(null);
    try {
      const updatedUser = await updateProfile(values);
      updateUser(updatedUser);
      onClose();
    } catch (error) {
      setSubmitError(getApiErrorMessage(error, 'No se pudo actualizar el perfil.'));
    }
  };

  return (
    <Modal visible={visible} transparent animationType="slide" onRequestClose={onClose}>
      <Pressable className="flex-1 justify-end" style={{ backgroundColor: 'rgba(20,24,31,0.4)' }} onPress={onClose}>
        <Pressable
          className="rounded-t-2xl border border-border bg-card p-5"
          style={[CARD_SHADOW, { paddingBottom: insets.bottom + 20 }]}
          onPress={(e) => e.stopPropagation()}
        >
          <ScrollView keyboardShouldPersistTaps="handled" showsVerticalScrollIndicator={false}>
            <View className="mb-4 flex-row items-center justify-between">
              <Text className="text-lg font-bold text-foreground">Editar perfil</Text>
              <Pressable onPress={onClose} hitSlop={8}>
                <X size={20} color={ICON_COLOR_MUTED} />
              </Pressable>
            </View>

            <View className="gap-4">
              <View className="gap-1.5">
                <Text className="text-sm font-medium text-foreground">Nombre</Text>
                <Controller
                  control={control}
                  name="name"
                  render={({ field: { onChange, onBlur, value } }) => (
                    <TextInput
                      className="rounded-lg border border-input bg-background px-3 py-2.5 text-base text-foreground"
                      placeholder="Tu nombre"
                      placeholderTextColor={ICON_COLOR_MUTED}
                      autoComplete="name"
                      editable={!isSubmitting}
                      onBlur={onBlur}
                      onChangeText={onChange}
                      value={value}
                    />
                  )}
                />
                {errors.name && <Text className="text-xs text-destructive">{errors.name.message}</Text>}
              </View>

              <View className="gap-1.5">
                <Text className="text-sm font-medium text-foreground">Correo electrónico</Text>
                <Controller
                  control={control}
                  name="email"
                  render={({ field: { onChange, onBlur, value } }) => (
                    <TextInput
                      className="rounded-lg border border-input bg-background px-3 py-2.5 text-base text-foreground"
                      placeholder="tu@email.com"
                      placeholderTextColor={ICON_COLOR_MUTED}
                      autoCapitalize="none"
                      autoComplete="email"
                      keyboardType="email-address"
                      editable={!isSubmitting}
                      onBlur={onBlur}
                      onChangeText={onChange}
                      value={value}
                    />
                  )}
                />
                {errors.email && <Text className="text-xs text-destructive">{errors.email.message}</Text>}
              </View>

              {submitError && (
                <View className="rounded-lg border border-destructive/20 bg-destructive/10 p-3">
                  <Text className="text-sm text-destructive">{submitError}</Text>
                </View>
              )}

              <PressableScale
                className="h-11 flex-row items-center justify-center rounded-lg bg-primary"
                onPress={handleSubmit(onSubmit)}
                disabled={isSubmitting}
              >
                {isSubmitting ? (
                  <ActivityIndicator color="white" />
                ) : (
                  <Text className="text-base font-medium text-primary-foreground">Guardar cambios</Text>
                )}
              </PressableScale>
            </View>
          </ScrollView>
        </Pressable>
      </Pressable>
    </Modal>
  );
}
