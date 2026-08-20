import * as Haptics from 'expo-haptics';
import { useCallback } from 'react';
import { Pressable, type GestureResponderEvent, type PressableProps } from 'react-native';
import Animated, { useAnimatedStyle, useSharedValue, withSpring } from 'react-native-reanimated';

const AnimatedPressable = Animated.createAnimatedComponent(Pressable);

interface PressableScaleProps extends PressableProps {
  /** Escala al presionar (1 = sin efecto). Default 0.94 — sutil, no exagerado. */
  scaleTo?: number;
  /** Vibración liviana al soltar. Default true — desactivar en toques muy frecuentes. */
  haptic?: boolean;
}

/**
 * Pressable con feedback táctil real: achica levemente al tocar (Reanimated, corre en el
 * hilo de UI, sin tirones) + vibración liviana opcional (expo-haptics). Reemplaza el
 * `active:` genérico de NativeWind en los puntos de contacto principales de la app (FAB,
 * selectores de pestaña/pill, botones primarios) — no se usó en cada elemento tocable.
 */
export function PressableScale({
  scaleTo = 0.94,
  haptic = true,
  onPressIn,
  onPressOut,
  onPress,
  style,
  ...rest
}: PressableScaleProps) {
  const scale = useSharedValue(1);

  const animatedStyle = useAnimatedStyle(() => ({
    transform: [{ scale: scale.value }],
  }));

  const handlePressIn = useCallback(
    (event: GestureResponderEvent) => {
      scale.value = withSpring(scaleTo, { damping: 16, stiffness: 320 });
      onPressIn?.(event);
    },
    [onPressIn, scale, scaleTo],
  );

  const handlePressOut = useCallback(
    (event: GestureResponderEvent) => {
      scale.value = withSpring(1, { damping: 16, stiffness: 320 });
      onPressOut?.(event);
    },
    [onPressOut, scale],
  );

  const handlePress = useCallback(
    (event: GestureResponderEvent) => {
      if (haptic) {
        void Haptics.impactAsync(Haptics.ImpactFeedbackStyle.Light);
      }
      onPress?.(event);
    },
    [haptic, onPress],
  );

  return (
    <AnimatedPressable
      onPressIn={handlePressIn}
      onPressOut={handlePressOut}
      onPress={handlePress}
      style={[animatedStyle, style]}
      {...rest}
    />
  );
}
