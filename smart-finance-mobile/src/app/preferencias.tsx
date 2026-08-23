import { Stack } from 'expo-router';
import { Fingerprint, Globe, Palette, RotateCcw, Wallet } from 'lucide-react-native';
import { useEffect, useRef, useState } from 'react';
import { ActivityIndicator, Pressable, ScrollView, Switch, View } from 'react-native';

import { AppText as Text } from '@/components/app-text';
import { SafeAreaView } from 'react-native-safe-area-context';

import { useThemePreference, type ThemePreference } from '@/components/korofin-theme-provider';
import { PressableScale } from '@/components/pressable-scale';
import { useIconColors } from '@/constants/icon-colors';
import { useUpdateUserPreferences, useUserPreferences } from '@/hooks/use-user-preferences';
import { getApiErrorMessage } from '@/lib/api-client';
import {
  isBiometricAvailable,
  isBiometricLockEnabled,
  setBiometricLockEnabled,
} from '@/lib/biometric-lock';
import { CARD_SHADOW } from '@/lib/shadows';
import {
  CURRENCIES,
  LANGUAGES,
  toBackendTheme,
  toLocalTheme,
  type CurrencyCode,
  type LanguageCode,
} from '@/lib/types/preferences';

const THEME_PREFERENCE_ORDER: ThemePreference[] = ['system', 'light', 'dark'];

const THEME_PREFERENCE_LABELS: Record<ThemePreference, string> = {
  system: 'Sistema',
  light: 'Claro',
  dark: 'Oscuro',
};

const CURRENCY_LABELS: Record<CurrencyCode, string> = {
  COP: 'COP - Peso colombiano',
  USD: 'USD - Dólar estadounidense',
  MXN: 'MXN - Peso mexicano',
  ARS: 'ARS - Peso argentino',
  EUR: 'EUR - Euro',
};

const LANGUAGE_LABELS: Record<LanguageCode, string> = {
  ES: 'Español',
  EN: 'English',
};

/**
 * M3 del track móvil (docs/plan-sprints-movil-nativo.md): tema, moneda e idioma persisten en el
 * backend (GET/PATCH /api/users/preferences), no solo en estado local. El tema sigue aplicándose
 * de inmediato vía NativeWind + expo-secure-store (useThemePreference, sin tocar esa pieza); acá
 * además se sincroniza con el backend para que sobreviva a una reinstalación/otro dispositivo.
 *
 * Nota: la app todavía no tiene una capa de i18n real — elegir "English" guarda la preferencia
 * pero no traduce ningún texto todavía (los literales siguen en español). El formato de moneda en
 * el resto de la app (`formatCurrency`) tampoco lee esta preferencia todavía — hacerlo requiere un
 * refactor más amplio, fuera de alcance de este milestone.
 */
export default function PreferenciasScreen() {
  const { ICON_COLOR_MUTED } = useIconColors();
  const { preference, setPreference } = useThemePreference();

  const preferencesQuery = useUserPreferences();
  const updatePreferences = useUpdateUserPreferences();
  const [currency, setCurrency] = useState<CurrencyCode>('COP');
  const [language, setLanguage] = useState<LanguageCode>('ES');
  const [errorMessage, setErrorMessage] = useState<string | null>(null);
  const hydratedFromBackend = useRef(false);

  const [biometricAvailable, setBiometricAvailable] = useState(false);
  const [biometricLockEnabled, setBiometricLockEnabledState] = useState(false);

  useEffect(() => {
    void (async () => {
      const [available, enabled] = await Promise.all([isBiometricAvailable(), isBiometricLockEnabled()]);
      setBiometricAvailable(available);
      setBiometricLockEnabledState(enabled && available);
    })();
  }, []);

  async function toggleBiometricLock(value: boolean) {
    setBiometricLockEnabledState(value);
    await setBiometricLockEnabled(value);
  }

  // Hidrata una sola vez desde el backend (fuente de verdad entre dispositivos); cambios
  // posteriores del usuario en esta pantalla no deben pisarse con un refetch en segundo plano.
  useEffect(() => {
    if (hydratedFromBackend.current || !preferencesQuery.data) return;
    hydratedFromBackend.current = true;

    const backend = preferencesQuery.data;
    setCurrency(backend.currency);
    setLanguage(backend.language);
    if (toLocalTheme(backend.theme) !== preference) {
      setPreference(toLocalTheme(backend.theme));
    }
    // Solo debe correr cuando llega el dato del backend, no en cada cambio de `preference`.
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [preferencesQuery.data]);

  async function persist(next: { theme: ThemePreference; currency: CurrencyCode; language: LanguageCode }) {
    // Salvaguarda: si el GET inicial nunca llegó a hidratar (falló, o el usuario alcanzó a tocar
    // algo antes de que resuelva), no hay forma de saber los valores reales del servidor — mandar
    // el PATCH acá pisaría lo que haya guardado con los defaults locales (COP/ES). La UI ya evita
    // este caso ocultando los controles hasta preferencesQuery.isSuccess, esto es defensa en
    // profundidad si algo cambia ese gate más adelante.
    if (!hydratedFromBackend.current) return;

    setErrorMessage(null);
    try {
      await updatePreferences.mutateAsync({
        theme: toBackendTheme(next.theme),
        currency: next.currency,
        language: next.language,
      });
    } catch (error) {
      setErrorMessage(getApiErrorMessage(error, 'No se pudieron guardar las preferencias.'));
    }
  }

  function cycleTheme() {
    const currentIndex = THEME_PREFERENCE_ORDER.indexOf(preference);
    const next = THEME_PREFERENCE_ORDER[(currentIndex + 1) % THEME_PREFERENCE_ORDER.length];
    setPreference(next);
    void persist({ theme: next, currency, language });
  }

  function selectCurrency(next: CurrencyCode) {
    setCurrency(next);
    void persist({ theme: preference, currency: next, language });
  }

  function selectLanguage(next: LanguageCode) {
    setLanguage(next);
    void persist({ theme: preference, currency, language: next });
  }

  return (
    <SafeAreaView className="flex-1 bg-background">
      <Stack.Screen options={{ headerShown: true, title: 'Preferencias' }} />
      <ScrollView contentContainerClassName="gap-3 px-5 py-6">
        {preferencesQuery.isPending ? (
          <View className="items-center py-10">
            <ActivityIndicator />
          </View>
        ) : preferencesQuery.isError ? (
          <View className="gap-3 rounded-xl border border-border bg-card p-4" style={CARD_SHADOW}>
            <Text className="text-sm text-muted-foreground">
              No se pudieron cargar tus preferencias. Cambiarlas ahora pisaría lo que tengas guardado.
            </Text>
            <Pressable
              className="h-10 flex-row items-center justify-center gap-2 rounded-lg border border-border bg-background active:opacity-70"
              onPress={() => void preferencesQuery.refetch()}
            >
              <RotateCcw size={16} color={ICON_COLOR_MUTED} />
              <Text className="text-sm font-medium text-foreground">Reintentar</Text>
            </Pressable>
          </View>
        ) : (
          <View className="overflow-hidden rounded-xl border border-border bg-card" style={CARD_SHADOW}>
            <Pressable
              className="flex-row items-center gap-3 border-b border-border px-4 py-3.5 active:opacity-70"
              onPress={cycleTheme}
              disabled={updatePreferences.isPending}
            >
              <View className="h-9 w-9 items-center justify-center rounded-full bg-secondary">
                <Palette size={16} color={ICON_COLOR_MUTED} />
              </View>
              <Text className="flex-1 text-sm font-medium text-foreground">Tema</Text>
              <Text className="text-sm text-muted-foreground">{THEME_PREFERENCE_LABELS[preference]}</Text>
            </Pressable>

            <View className="flex-row items-center gap-3 border-b border-border px-4 py-3.5">
              <View className="h-9 w-9 items-center justify-center rounded-full bg-secondary">
                <Wallet size={16} color={ICON_COLOR_MUTED} />
              </View>
              <Text className="flex-1 text-sm font-medium text-foreground">Moneda</Text>
            </View>
            <ScrollView horizontal showsHorizontalScrollIndicator={false} contentContainerClassName="gap-2 border-b border-border px-4 pb-3">
              {CURRENCIES.map((code) => {
                const active = code === currency;
                return (
                  <PressableScale
                    key={code}
                    scaleTo={0.97}
                    onPress={() => selectCurrency(code)}
                    disabled={updatePreferences.isPending}
                    className={`rounded-full border px-3 py-1.5 ${active ? 'border-primary bg-primary' : 'border-border bg-background'}`}
                  >
                    <Text className={`text-xs font-medium ${active ? 'text-primary-foreground' : 'text-foreground'}`}>
                      {CURRENCY_LABELS[code]}
                    </Text>
                  </PressableScale>
                );
              })}
            </ScrollView>

            <View className="flex-row items-center gap-3 px-4 py-3.5">
              <View className="h-9 w-9 items-center justify-center rounded-full bg-secondary">
                <Globe size={16} color={ICON_COLOR_MUTED} />
              </View>
              <Text className="flex-1 text-sm font-medium text-foreground">Idioma</Text>
            </View>
            <View className="flex-row gap-2 px-4 pb-4">
              {LANGUAGES.map((code) => {
                const active = code === language;
                return (
                  <PressableScale
                    key={code}
                    scaleTo={0.97}
                    onPress={() => selectLanguage(code)}
                    disabled={updatePreferences.isPending}
                    className={`rounded-full border px-3 py-1.5 ${active ? 'border-primary bg-primary' : 'border-border bg-background'}`}
                  >
                    <Text className={`text-xs font-medium ${active ? 'text-primary-foreground' : 'text-foreground'}`}>
                      {LANGUAGE_LABELS[code]}
                    </Text>
                  </PressableScale>
                );
              })}
            </View>
          </View>
        )}

        {biometricAvailable && (
          <View
            className="flex-row items-center justify-between rounded-xl border border-border bg-card p-4"
            style={CARD_SHADOW}
          >
            <View className="flex-1 flex-row items-center gap-3 pr-3">
              <View className="h-9 w-9 items-center justify-center rounded-full bg-secondary">
                <Fingerprint size={16} color={ICON_COLOR_MUTED} />
              </View>
              <View className="flex-1">
                <Text className="text-sm font-medium text-foreground">Bloqueo con biometría</Text>
                <Text className="text-xs text-muted-foreground">Face ID o huella para abrir la app</Text>
              </View>
            </View>
            <Switch value={biometricLockEnabled} onValueChange={(value) => void toggleBiometricLock(value)} />
          </View>
        )}

        {errorMessage && (
          <View className="rounded-lg border border-destructive/20 bg-destructive/10 p-3">
            <Text className="text-sm text-destructive">{errorMessage}</Text>
          </View>
        )}
      </ScrollView>
    </SafeAreaView>
  );
}
