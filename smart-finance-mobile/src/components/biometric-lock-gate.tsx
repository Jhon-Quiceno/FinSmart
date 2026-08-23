import { Fingerprint } from 'lucide-react-native';
import { useCallback, useEffect, useRef, useState, type PropsWithChildren } from 'react';
import { AppState, type AppStateStatus, View } from 'react-native';

import { AppText as Text } from '@/components/app-text';
import { PressableScale } from '@/components/pressable-scale';
import { useIconColors } from '@/constants/icon-colors';
import { authenticate, isBiometricAvailable, isBiometricLockEnabled } from '@/lib/biometric-lock';

/**
 * Gatea el acceso a toda la app montada detrás de Face ID/huella cuando el usuario activó
 * "Bloqueo con biometría" en Preferencias — al abrir la app en frío y cada vez que vuelve de
 * segundo plano (no solo al inicio). Ver docs/plan-sprints-movil-nativo.md, milestone M5.
 */
export function BiometricLockGate({ children }: PropsWithChildren) {
  const { ICON_COLOR_MUTED } = useIconColors();
  const [locked, setLocked] = useState<boolean | null>(null);
  const appState = useRef(AppState.currentState);
  // El prompt nativo de authenticateAsync dispara por sí mismo transiciones de AppState
  // (active→inactial→active) mientras está en pantalla. Sin este guard, el listener de abajo
  // interpretaría esa transición como "volvió de background" y llamaría a checkLockState de
  // nuevo, pisando un desbloqueo exitoso que ya estaba en curso (doble prompt).
  const authenticating = useRef(false);

  const checkLockState = useCallback(async () => {
    if (authenticating.current) return;
    const [enabled, available] = await Promise.all([isBiometricLockEnabled(), isBiometricAvailable()]);
    setLocked(enabled && available);
  }, []);

  useEffect(() => {
    void checkLockState();

    const subscription = AppState.addEventListener('change', (nextState: AppStateStatus) => {
      if (/inactive|background/.test(appState.current) && nextState === 'active') {
        void checkLockState();
      }
      appState.current = nextState;
    });

    return () => subscription.remove();
  }, [checkLockState]);

  async function unlock() {
    authenticating.current = true;
    try {
      const success = await authenticate('Desbloqueá KoroFin');
      if (success) setLocked(false);
    } catch {
      // El usuario puede reintentar tocando "Desbloquear" de nuevo.
    } finally {
      authenticating.current = false;
    }
  }

  // null: todavía resolviendo si el bloqueo aplica — no se sabe si mostrar la app o el bloqueo, y
  // mostrar los children acá (aunque sea un instante) expondría el cache financiero persistido en
  // AsyncStorage antes de confirmar que el usuario pasó la biometría. false: bloqueo desactivado o
  // sin biometría enrolada — pasa directo.
  if (locked === null) {
    return null;
  }
  if (locked === false) {
    return <>{children}</>;
  }

  return (
    <View className="flex-1 items-center justify-center gap-4 bg-background px-8">
      <View className="h-16 w-16 items-center justify-center rounded-full bg-primary/15">
        <Fingerprint size={28} color={ICON_COLOR_MUTED} />
      </View>
      <Text className="text-center text-sm text-muted-foreground">KoroFin está bloqueado</Text>
      <PressableScale
        className="h-11 items-center justify-center rounded-lg bg-primary px-6"
        onPress={() => void unlock()}
      >
        <Text className="text-base font-medium text-primary-foreground">Desbloquear</Text>
      </PressableScale>
    </View>
  );
}
