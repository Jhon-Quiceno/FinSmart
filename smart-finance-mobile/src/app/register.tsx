import { zodResolver } from '@hookform/resolvers/zod';
import { Link, Stack } from 'expo-router';
import { Eye, EyeOff, Lock, Mail, User } from 'lucide-react-native';
import { useState } from 'react';
import { Controller, useForm } from 'react-hook-form';
import { ActivityIndicator, Pressable, ScrollView, View } from 'react-native';

import { AppText as Text, AppTextInput as TextInput } from '@/components/app-text';
import { SafeAreaView } from 'react-native-safe-area-context';

import { PressableScale } from '@/components/pressable-scale';
import { useIconColors } from '@/constants/icon-colors';
import { CARD_SHADOW } from '@/lib/shadows';
import { registerSchema, type RegisterFormValues } from '@/lib/schemas/register.schema';
import KorofinLogo from '@/assets/images/logo-korofin.svg';

export default function RegisterScreen() {
  const [showPassword, setShowPassword] = useState(false);
  const [submitError, setSubmitError] = useState<string | null>(null);
  const { ICON_COLOR_MUTED } = useIconColors();

  const {
    control,
    handleSubmit,
    formState: { errors, isSubmitting },
  } = useForm<RegisterFormValues>({
    resolver: zodResolver(registerSchema),
    defaultValues: { name: '', email: '', password: '', confirmPassword: '' },
  });

  // TODO(Fase 0 backend): reemplazar por registerRequest() real contra POST /api/users/register
  // una vez exista el endpoint mobile-friendly del §2 del plan — hoy solo valida el formulario.
  const onSubmit = async (_values: RegisterFormValues) => {
    setSubmitError(null);
    await new Promise((resolve) => setTimeout(resolve, 800));
    setSubmitError('Registro real pendiente — falta el endpoint mobile-friendly del backend (Fase 0).');
  };

  return (
    <SafeAreaView className="flex-1 bg-background">
      <Stack.Screen options={{ headerShown: true, title: 'Crear cuenta' }} />
      <ScrollView
        contentContainerClassName="flex-grow justify-center px-6 py-10"
        keyboardShouldPersistTaps="handled"
      >
        <View className="mb-8 items-center gap-2">
          <KorofinLogo width={64} height={64} />
          <Text className="text-2xl font-bold text-foreground">Creá tu cuenta</Text>
          <Text className="text-sm text-muted-foreground">Empezá a organizar tus finanzas hoy</Text>
        </View>

        <View className="rounded-2xl border border-border bg-card p-5" style={CARD_SHADOW}>
          <View className="gap-4">
            <View className="gap-1.5">
              <Text className="text-sm font-medium text-foreground">Nombre completo</Text>
              <View className="relative justify-center">
                <User size={16} color={ICON_COLOR_MUTED} style={{ position: 'absolute', left: 12, zIndex: 1 }} />
                <Controller
                  control={control}
                  name="name"
                  render={({ field: { onChange, onBlur, value } }) => (
                    <TextInput
                      className="rounded-lg border border-input bg-background py-3 pl-9 pr-3 text-base text-foreground"
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
              </View>
              {errors.name && <Text className="text-xs text-destructive">{errors.name.message}</Text>}
            </View>

            <View className="gap-1.5">
              <Text className="text-sm font-medium text-foreground">Correo electrónico</Text>
              <View className="relative justify-center">
                <Mail size={16} color={ICON_COLOR_MUTED} style={{ position: 'absolute', left: 12, zIndex: 1 }} />
                <Controller
                  control={control}
                  name="email"
                  render={({ field: { onChange, onBlur, value } }) => (
                    <TextInput
                      className="rounded-lg border border-input bg-background py-3 pl-9 pr-3 text-base text-foreground"
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
              </View>
              {errors.email && <Text className="text-xs text-destructive">{errors.email.message}</Text>}
            </View>

            <View className="gap-1.5">
              <Text className="text-sm font-medium text-foreground">Contraseña</Text>
              <View className="relative justify-center">
                <Lock size={16} color={ICON_COLOR_MUTED} style={{ position: 'absolute', left: 12, zIndex: 1 }} />
                <Controller
                  control={control}
                  name="password"
                  render={({ field: { onChange, onBlur, value } }) => (
                    <TextInput
                      className="rounded-lg border border-input bg-background py-3 pl-9 pr-10 text-base text-foreground"
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
              {errors.password && (
                <Text className="text-xs text-destructive">{errors.password.message}</Text>
              )}
            </View>

            <View className="gap-1.5">
              <Text className="text-sm font-medium text-foreground">Confirmar contraseña</Text>
              <View className="relative justify-center">
                <Lock size={16} color={ICON_COLOR_MUTED} style={{ position: 'absolute', left: 12, zIndex: 1 }} />
                <Controller
                  control={control}
                  name="confirmPassword"
                  render={({ field: { onChange, onBlur, value } }) => (
                    <TextInput
                      className="rounded-lg border border-input bg-background py-3 pl-9 pr-3 text-base text-foreground"
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
              </View>
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
                <Text className="text-base font-medium text-primary-foreground">Crear cuenta</Text>
              )}
            </PressableScale>
          </View>

          <Text className="mt-5 text-center text-sm text-muted-foreground">
            ¿Ya tenés una cuenta?{' '}
            <Link href="/login" className="font-medium text-primary">
              Iniciar sesión
            </Link>
          </Text>
        </View>
      </ScrollView>
    </SafeAreaView>
  );
}
