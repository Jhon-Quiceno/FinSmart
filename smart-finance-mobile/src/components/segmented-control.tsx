import { View, type ViewStyle } from 'react-native';

import { AppText as Text } from '@/components/app-text';
import { PressableScale } from '@/components/pressable-scale';

interface SegmentedControlOption<T extends string> {
  value: T;
  label: string;
}

interface SegmentedControlProps<T extends string> {
  options: SegmentedControlOption<T>[];
  value: T;
  onChange: (value: T) => void;
}

// Elevación real (no solo `active:`) en el segmento activo — mismo patrón que un
// UISegmentedControl: una pista con el fondo de la app y una "chip" flotante encima.
const ACTIVE_SHADOW: ViewStyle = {
  shadowColor: '#000',
  shadowOffset: { width: 0, height: 1 },
  shadowOpacity: 0.08,
  shadowRadius: 3,
  elevation: 2,
};

/**
 * Segmented control con pista (track) — reemplaza los pares/tríos de pills sueltas que se
 * repetían en Deudas, Movimientos y el filtro Todos/Ingresos/Gastos. Un solo componente para
 * los tres lugares, así el patrón visual queda consistente en toda la app.
 */
export function SegmentedControl<T extends string>({ options, value, onChange }: SegmentedControlProps<T>) {
  return (
    <View className="flex-row gap-1 rounded-lg bg-secondary p-1">
      {options.map((option) => {
        const active = option.value === value;
        return (
          <PressableScale
            key={option.value}
            scaleTo={0.97}
            onPress={() => onChange(option.value)}
            className={`flex-1 items-center rounded-md py-2 ${active ? 'bg-card' : ''}`}
            style={active ? ACTIVE_SHADOW : undefined}
          >
            <Text className={`text-sm ${active ? 'font-semibold text-primary' : 'font-medium text-muted-foreground'}`}>
              {option.label}
            </Text>
          </PressableScale>
        );
      })}
    </View>
  );
}
