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

  const checkLockState = useCallback(async () => {
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
    const success = await authenticate('Desbloqueá KoroFin');
    if (success) setLocked(false);
  }

  // null: todavía resolviendo si el bloqueo aplica — no mostrar nada tapado ni destapado por un
  // instante. false: bloqueo desactivado o sin biometría enrolada — pasa directo.
  if (locked === null || locked === false) {
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
