import { zodResolver } from '@hookform/resolvers/zod';
import { Eye, EyeOff, X } from 'lucide-react-native';
import { useEffect, useState } from 'react';
import { Controller, useForm } from 'react-hook-form';
import { ActivityIndicator, Modal, Pressable, ScrollView, View } from 'react-native';
import { useSafeAreaInsets } from 'react-native-safe-area-context';

import { AppText as Text, AppTextInput as TextInput } from '@/components/app-text';
import { PressableScale } from '@/components/pressable-scale';
import { useIconColors } from '@/constants/icon-colors';
import { getApiErrorMessage } from '@/lib/api-client';
import { changePasswordSchema, type ChangePasswordFormValues } from '@/lib/schemas/user.schema';
import { changePassword } from '@/lib/services/user.service';
import { CARD_SHADOW } from '@/lib/shadows';

interface ChangePasswordModalProps {
  visible: boolean;
  onClose: () => void;
}

const EMPTY_VALUES: ChangePasswordFormValues = {
  currentPassword: '',
  newPassword: '',
  confirmPassword: '',
};

/**
 * Cambio real de contraseña contra PUT /api/users/password. Mismo patrón de Modal
 * transparente + bottom sheet que EditProfileModal/QuickAddExpenseModal.
 */
export function ChangePasswordModal({ visible, onClose }: ChangePasswordModalProps) {
  const insets = useSafeAreaInsets();
  const { ICON_COLOR_MUTED } = useIconColors();
  const [showPassword, setShowPassword] = useState(false);
  const [submitError, setSubmitError] = useState<string | null>(null);

  const {
    control,
    handleSubmit,
    reset,
    formState: { errors, isSubmitting },
  } = useForm<ChangePasswordFormValues>({
    resolver: zodResolver(changePasswordSchema),
    defaultValues: EMPTY_VALUES,
  });

  useEffect(() => {
    if (visible) {
      setSubmitError(null);
      setShowPassword(false);
      reset(EMPTY_VALUES);
    }
  }, [visible, reset]);

  const onSubmit = async (values: ChangePasswordFormValues) => {
    setSubmitError(null);
    try {
      await changePassword(values);
      onClose();
    } catch (error) {
      setSubmitError(getApiErrorMessage(error, 'No se pudo cambiar la contraseña.'));
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
              <Text className="text-lg font-bold text-foreground">Cambiar contraseña</Text>
              <Pressable onPress={onClose} hitSlop={8}>
                <X size={20} color={ICON_COLOR_MUTED} />
              </Pressable>
            </View>

            <View className="gap-4">
              <View className="gap-1.5">
                <Text className="text-sm font-medium text-foreground">Contraseña actual</Text>
                <Controller
                  control={control}
                  name="currentPassword"
                  render={({ field: { onChange, onBlur, value } }) => (
                    <TextInput
                      className="rounded-lg border border-input bg-background px-3 py-2.5 text-base text-foreground"
                      placeholder="••••••••"
                      placeholderTextColor={ICON_COLOR_MUTED}
                      secureTextEntry={!showPassword}
                      autoCapitalize="none"
                      autoComplete="password"
                      editable={!isSubmitting}
                      onBlur={onBlur}
                      onChangeText={onChange}
                      value={value}
                    />
                  )}
                />
                {errors.currentPassword && (
                  <Text className="text-xs text-destructive">{errors.currentPassword.message}</Text>
                )}
              </View>

              <View className="gap-1.5">
                <Text className="text-sm font-medium text-foreground">Nueva contraseña</Text>
                <View className="relative justify-center">
                  <Controller
                    control={control}
                    name="newPassword"
                    render={({ field: { onChange, onBlur, value } }) => (
                      <TextInput
                        className="rounded-lg border border-input bg-background px-3 py-2.5 pr-10 text-base text-foreground"
                        placeholder="••••••••"
                        placeholderTextColor={ICON_COLOR_MUTED}
                        secureTextEntry={!showPassword}
                        autoCapitalize="none"
                        autoComplete="password-new"
                        editable={!isSubmitting}
                        onBlur={onBlur}
                        onChangeText={onChange}
                        value={value}
                      />
                    )}
                  />
                  <Pressable
                    onPress={() => setShowPassword((prev) => !prev)}
                    style={{ position: 'absolute', right: 12, zIndex: 1 }}
                    hitSlop={8}
                  >
                    {showPassword ? (
                      <EyeOff size={16} color={ICON_COLOR_MUTED} />
                    ) : (
                      <Eye size={16} color={ICON_COLOR_MUTED} />
                    )}
                  </Pressable>
                </View>
                {errors.newPassword && (
                  <Text className="text-xs text-destructive">{errors.newPassword.message}</Text>
                )}
              </View>

              <View className="gap-1.5">
                <Text className="text-sm font-medium text-foreground">Confirmar nueva contraseña</Text>
                <Controller
                  control={control}
                  name="confirmPassword"
                  render={({ field: { onChange, onBlur, value } }) => (
                    <TextInput
                      className="rounded-lg border border-input bg-background px-3 py-2.5 text-base text-foreground"
                      placeholder="••••••••"
                      placeholderTextColor={ICON_COLOR_MUTED}
                      secureTextEntry={!showPassword}
                      autoCapitalize="none"
                      editable={!isSubmitting}
                      onBlur={onBlur}
                      onChangeText={onChange}
                      value={value}
                    />
                  )}
                />
                {errors.confirmPassword && (
                  <Text className="text-xs text-destructive">{errors.confirmPassword.message}</Text>
                )}
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
                  <Text className="text-base font-medium text-primary-foreground">Guardar contraseña</Text>
                )}
              </PressableScale>
            </View>
          </ScrollView>
        </Pressable>
      </Pressable>
    </Modal>
  );
}
