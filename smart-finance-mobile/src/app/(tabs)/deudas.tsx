import { CreditCard as CreditCardIcon, HandCoins, Landmark, PlusCircle } from 'lucide-react-native';
import { useState } from 'react';
import { ScrollView, View } from 'react-native';

import { AppText as Text } from '@/components/app-text';
import { SafeAreaView } from 'react-native-safe-area-context';

import { Fab } from '@/components/fab';
import { PressableScale } from '@/components/pressable-scale';
import { NotificationBell } from '@/components/notification-bell';
import { ProfileBubble } from '@/components/profile-bubble';
import { SegmentedControl } from '@/components/segmented-control';
import { useIconColors } from '@/constants/icon-colors';
import { formatCurrency } from '@/lib/format';
import { mockCreditCards, mockDebts, type MockCreditCard, type MockDebt } from '@/lib/mock/deudas';
import { CARD_SHADOW } from '@/lib/shadows';

function DebtCard({ debt }: { debt: MockDebt }) {
  const isPaidOff = debt.remainingAmount === 0;
  const progress = isPaidOff ? 1 : 1 - debt.remainingAmount / debt.totalAmount;
  const { ICON_COLOR_MUTED } = useIconColors();

  return (
    <View className="gap-3 rounded-2xl border border-border bg-card p-4" style={CARD_SHADOW}>
      <View className="flex-row items-center gap-3">
        <View className="h-12 w-12 items-center justify-center rounded-full bg-secondary">
          <Landmark size={20} color={ICON_COLOR_MUTED} />
        </View>
        <View className="flex-1">
          <Text className="text-sm font-medium text-foreground">{debt.name}</Text>
          <Text className="text-xs text-muted-foreground">
            {isPaidOff ? 'Deuda saldada' : `Restante: ${formatCurrency(debt.remainingAmount)}`}
          </Text>
        </View>
      </View>
      <View className="h-2 overflow-hidden rounded-full bg-muted">
        <View className={`h-2 rounded-full ${isPaidOff ? 'bg-success' : 'bg-primary'}`} style={{ width: `${progress * 100}%` }} />
      </View>
      <View className="flex-row justify-between">
        <Text className="text-xs text-muted-foreground">Total: {formatCurrency(debt.totalAmount)}</Text>
        {debt.interestRate !== null && (
          <Text className="text-xs text-muted-foreground">Interés: {debt.interestRate}% m.v.</Text>
        )}
      </View>
    </View>
  );
}

function CardTile({ card }: { card: MockCreditCard }) {
  const utilization = card.currentBalance / card.creditLimit;
  const isHighUtilization = utilization >= 0.8;
  const { ICON_COLOR_MUTED, ICON_COLOR_WHITE } = useIconColors();

  return (
    <View className="gap-3 rounded-xl border border-border bg-card p-4" style={CARD_SHADOW}>
      <View className="flex-row items-center justify-between">
        <View className="flex-row items-center gap-3">
          <View className="h-9 w-9 items-center justify-center rounded-full bg-secondary">
            <CreditCardIcon size={16} color={ICON_COLOR_MUTED} />
          </View>
          <View>
            <Text className="text-sm font-medium text-foreground">{card.name}</Text>
            <Text className="text-xs text-muted-foreground">{card.bank} · {card.franchise}</Text>
          </View>
        </View>
        <View className="rounded-full bg-secondary px-2.5 py-1">
          <Text className="text-[10px] font-bold text-secondary-foreground">Corte día {card.paymentDueDay}</Text>
        </View>
      </View>
      <View className="h-2 overflow-hidden rounded-full bg-muted">
        <View
          className={`h-2 rounded-full ${isHighUtilization ? 'bg-destructive' : 'bg-primary'}`}
          style={{ width: `${Math.min(utilization * 100, 100)}%` }}
        />
      </View>
      <View className="flex-row justify-between">
        <Text className="text-xs text-muted-foreground">Usado: {formatCurrency(card.currentBalance)}</Text>
        <Text className="text-xs text-muted-foreground">Disponible: {formatCurrency(card.availableCredit)}</Text>
      </View>
      <View className="flex-row gap-2">
        <PressableScale className="flex-1 flex-row items-center justify-center gap-1.5 rounded-lg border border-border py-2">
          <PlusCircle size={14} color={ICON_COLOR_MUTED} />
          <Text className="text-xs font-medium text-foreground">Cargo</Text>
        </PressableScale>
        <PressableScale className="flex-1 flex-row items-center justify-center gap-1.5 rounded-lg bg-primary py-2">
          <HandCoins size={14} color={ICON_COLOR_WHITE} />
          <Text className="text-xs font-medium text-primary-foreground">Abono</Text>
        </PressableScale>
      </View>
    </View>
  );
}

type View_ = 'DEBTS' | 'CARDS';

const FILTERS: { value: View_; label: string }[] = [
  { value: 'DEBTS', label: 'Deudas' },
  { value: 'CARDS', label: 'Tarjetas' },
];

export default function DeudasScreen() {
  const [view, setView] = useState<View_>('DEBTS');

  return (
    <SafeAreaView className="flex-1 bg-background" edges={['top']}>
      <View className="flex-row items-center justify-between px-5 pb-2 pt-6">
        <Text className="text-2xl font-bold text-foreground">Deudas y tarjetas</Text>
        <View className="flex-row items-center gap-2">
          <NotificationBell />
          <ProfileBubble />
        </View>
      </View>

      <View className="px-5 pb-3">
        <SegmentedControl options={FILTERS} value={view} onChange={setView} />
      </View>

      <ScrollView contentContainerClassName="gap-3 px-5 pb-24" showsVerticalScrollIndicator={false}>
        {view === 'DEBTS'
          ? mockDebts.map((debt) => <DebtCard key={debt.id} debt={debt} />)
          : mockCreditCards.map((card) => <CardTile key={card.id} card={card} />)}
      </ScrollView>

      <Fab />
    </SafeAreaView>
  );
}
