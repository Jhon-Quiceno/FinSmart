import { zodResolver } from '@hookform/resolvers/zod';
import { Link } from 'expo-router';
import { Check, Eye, EyeOff, Lock, Mail } from 'lucide-react-native';
import { useState } from 'react';
import { Controller, useForm } from 'react-hook-form';
import {
  ActivityIndicator,
  Pressable,
  ScrollView,
  View,
} from 'react-native';
import { SafeAreaView } from 'react-native-safe-area-context';

import { AppText as Text, AppTextInput as TextInput } from '@/components/app-text';
import { PressableScale } from '@/components/pressable-scale';
import { useIconColors } from '@/constants/icon-colors';
import { CARD_SHADOW } from '@/lib/shadows';
import { loginSchema, type LoginFormValues } from '@/lib/schemas/login.schema';
import KorofinLogo from '@/assets/images/logo-korofin.svg';

export default function LoginScreen() {
  const [showPassword, setShowPassword] = useState(false);
  const [submitError, setSubmitError] = useState<string | null>(null);
  const { ICON_COLOR_MUTED } = useIconColors();

  const {
    control,
    handleSubmit,
    formState: { errors, isSubmitting },
  } = useForm<LoginFormValues>({
    resolver: zodResolver(loginSchema),
    defaultValues: { email: '', password: '', rememberMe: false },
  });

  // TODO(Fase 0 backend): reemplazar por loginRequest() real contra
  // POST /api/users/login una vez exista el endpoint mobile-friendly del §2 del plan
  // (docs/plan-app-movil-react-native.md) — hoy solo valida el formulario.
  const onSubmit = async (_values: LoginFormValues) => {
    setSubmitError(null);
    await new Promise((resolve) => setTimeout(resolve, 800));
    setSubmitError('Login real pendiente — falta el endpoint mobile-friendly del backend (Fase 0).');
  };

  return (
    <SafeAreaView className="flex-1 bg-background">
      <ScrollView
        contentContainerClassName="flex-grow justify-center px-6 py-10"
        keyboardShouldPersistTaps="handled"
      >
        <View className="mb-8 items-center gap-2">
          <KorofinLogo width={72} height={72} />
          <Text className="text-2xl font-bold text-foreground">KoroFin</Text>
          <Text className="text-sm text-muted-foreground">Gestión Financiera Inteligente</Text>
        </View>

        <View className="rounded-2xl border border-border bg-card p-5" style={CARD_SHADOW}>
          <Text className="text-center text-xl font-bold text-card-foreground">
            Bienvenido de vuelta
          </Text>
          <Text className="mb-5 mt-1 text-center text-sm text-muted-foreground">
            Ingresá tus credenciales para acceder a tu cuenta
          </Text>

          <View className="gap-4">
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
              <View className="flex-row items-center justify-between">
                <Text className="text-sm font-medium text-foreground">Contraseña</Text>
                <Text className="text-xs font-medium text-primary">¿Olvidaste tu contraseña?</Text>
              </View>
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
                      autoComplete="password"
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

            <Controller
              control={control}
              name="rememberMe"
              render={({ field: { onChange, value } }) => (
                <Pressable
                  className="flex-row items-center gap-2"
                  onPress={() => onChange(!value)}
                  disabled={isSubmitting}
                >
                  <View
                    className={`h-5 w-5 items-center justify-center rounded border ${
                      value ? 'border-primary bg-primary' : 'border-input bg-background'
                    }`}
                  >
                    {value && <Check size={12} color="white" strokeWidth={3} />}
                  </View>
                  <Text className="text-sm text-muted-foreground">Recordar mi sesión</Text>
                </Pressable>
              )}
            />

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
                <Text className="text-base font-medium text-primary-foreground">Iniciar sesión</Text>
              )}
            </PressableScale>
          </View>

          <Text className="mt-5 text-center text-sm text-muted-foreground">
            ¿No tenés una cuenta?{' '}
            <Link href="/register" className="font-medium text-primary">
              Registrate gratis
            </Link>
          </Text>
        </View>

        {/* TODO(Fase 0 backend): retirar este acceso una vez exista login real contra
            POST /api/users/login — hoy es la única forma de llegar al resto del diseño. */}
        <Link
          href="/(tabs)"
          className="mt-6 rounded-lg border border-border px-4 py-3 text-center text-sm font-medium text-muted-foreground"
          style={{ borderStyle: 'dashed' }}
        >
          Continuar sin iniciar sesión (demo)
        </Link>
      </ScrollView>
    </SafeAreaView>
  );
}
