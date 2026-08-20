import { Plus } from 'lucide-react-native';

import { useIconColors } from '@/constants/icon-colors';
import { PressableScale } from '@/components/pressable-scale';

export function Fab({ onPress }: { onPress?: () => void }) {
  const { ICON_COLOR_WHITE } = useIconColors();

  return (
    <PressableScale
      onPress={onPress}
      className="items-center justify-center rounded-full bg-primary"
      style={{
        position: 'absolute',
        right: 20,
        bottom: 20,
        width: 56,
        height: 56,
        shadowColor: '#000',
        shadowOffset: { width: 0, height: 4 },
        shadowOpacity: 0.3,
        shadowRadius: 8,
        elevation: 6,
      }}
    >
      <Plus size={24} color={ICON_COLOR_WHITE} />
    </PressableScale>
  );
}
