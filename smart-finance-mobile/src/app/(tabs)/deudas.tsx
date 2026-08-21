import { CreditCard as CreditCardIcon, HandCoins, Landmark, PlusCircle } from 'lucide-react-native';
import { useState, type ReactNode } from 'react';
import { ActivityIndicator, Modal, Pressable, ScrollView, View } from 'react-native';

import { AppText as Text, AppTextInput as TextInput } from '@/components/app-text';
import { SafeAreaView } from 'react-native-safe-area-context';

import { Fab } from '@/components/fab';
import { PressableScale } from '@/components/pressable-scale';
import { NotificationBell } from '@/components/notification-bell';
import { ProfileBubble } from '@/components/profile-bubble';
import { SegmentedControl } from '@/components/segmented-control';
import { useIconColors } from '@/constants/icon-colors';
import { useCreateDebt, useDebts } from '@/hooks/use-debts';
import { useCreateCardPayment, useCreateCardPurchase } from '@/hooks/use-card-movements';
import { useCreditCards } from '@/hooks/use-credit-cards';
import { getApiErrorMessage } from '@/lib/api-client';
import { formatCurrency } from '@/lib/format';
import { CARD_SHADOW } from '@/lib/shadows';
import type { CreditCard } from '@/lib/types/credit-card';
import type { Debt } from '@/lib/types/debt';

function DebtCard({ debt }: { debt: Debt }) {
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

function CardTile({ card, onCargo, onAbono }: { card: CreditCard; onCargo: () => void; onAbono: () => void }) {
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
        <PressableScale
          onPress={onCargo}
          className="flex-1 flex-row items-center justify-center gap-1.5 rounded-lg border border-border py-2"
        >
          <PlusCircle size={14} color={ICON_COLOR_MUTED} />
          <Text className="text-xs font-medium text-foreground">Cargo</Text>
        </PressableScale>
        <PressableScale
          onPress={onAbono}
          className="flex-1 flex-row items-center justify-center gap-1.5 rounded-lg bg-primary py-2"
        >
          <HandCoins size={14} color={ICON_COLOR_WHITE} />
          <Text className="text-xs font-medium text-primary-foreground">Abono</Text>
        </PressableScale>
      </View>
    </View>
  );
}

/** Modal genérico con fondo semitransparente — mismo patrón que ProfileBubble. */
function FormModal({
  visible,
  title,
  onClose,
  children,
}: {
  visible: boolean;
  title: string;
  onClose: () => void;
  children: ReactNode;
}) {
  return (
    <Modal visible={visible} transparent animationType="fade" onRequestClose={onClose}>
      <Pressable className="flex-1 justify-end" style={{ backgroundColor: 'rgba(20,24,31,0.4)' }} onPress={onClose}>
        <Pressable onPress={(event) => event.stopPropagation()}>
          <View className="gap-4 rounded-t-2xl border border-border bg-card p-5" style={CARD_SHADOW}>
            <Text className="text-lg font-bold text-foreground">{title}</Text>
            {children}
          </View>
        </Pressable>
      </Pressable>
    </Modal>
  );
}

function FormField({
  label,
  value,
  onChangeText,
  placeholder,
  keyboardType,
}: {
  label: string;
  value: string;
  onChangeText: (text: string) => void;
  placeholder?: string;
  keyboardType?: 'default' | 'numeric';
}) {
  const { ICON_COLOR_MUTED } = useIconColors();

  return (
    <View className="gap-1.5">
      <Text className="text-sm font-medium text-foreground">{label}</Text>
      <TextInput
        className="rounded-lg border border-input bg-background px-3 py-3 text-base text-foreground"
        value={value}
        onChangeText={onChangeText}
        placeholder={placeholder}
        placeholderTextColor={ICON_COLOR_MUTED}
        keyboardType={keyboardType ?? 'default'}
      />
    </View>
  );
}

function ErrorBanner({ message }: { message: string }) {
  return (
    <View className="rounded-lg border border-destructive/20 bg-destructive/10 p-3">
      <Text className="text-sm text-destructive">{message}</Text>
    </View>
  );
}

/** Cargo (compra) o Abono (pago) sobre una tarjeta puntual. */
function CardMovementModal({ card, kind, onClose }: { card: CreditCard; kind: 'CARGO' | 'ABONO'; onClose: () => void }) {
  const [amount, setAmount] = useState('');
  const [description, setDescription] = useState('');
  const createPurchase = useCreateCardPurchase(card.id);
  const createPayment = useCreateCardPayment(card.id);

  const mutation = kind === 'CARGO' ? createPurchase : createPayment;
  const parsedAmount = Number(amount);
  const isAmountValid = amount.trim().length > 0 && Number.isFinite(parsedAmount) && parsedAmount > 0;

  const handleSubmit = async () => {
    if (!isAmountValid) return;
    try {
      await mutation.mutateAsync({
        amount: parsedAmount,
        description: description.trim() || undefined,
      });
      onClose();
    } catch {
      // El error queda visible en el banner de abajo (mutation.error) — no hace falta relanzar.
    }
  };

  return (
    <FormModal visible title={kind === 'CARGO' ? `Cargo · ${card.name}` : `Abono · ${card.name}`} onClose={onClose}>
      <FormField label="Monto" value={amount} onChangeText={setAmount} placeholder="0" keyboardType="numeric" />
      <FormField label="Descripción (opcional)" value={description} onChangeText={setDescription} placeholder="Detalle" />
      {mutation.isError && <ErrorBanner message={getApiErrorMessage(mutation.error, 'No se pudo registrar el movimiento.')} />}
      <PressableScale
        className="h-11 flex-row items-center justify-center rounded-lg bg-primary"
        onPress={handleSubmit}
        disabled={!isAmountValid || mutation.isPending}
      >
        {mutation.isPending ? (
          <ActivityIndicator color="white" />
        ) : (
          <Text className="text-base font-medium text-primary-foreground">
            {kind === 'CARGO' ? 'Registrar cargo' : 'Registrar abono'}
          </Text>
        )}
      </PressableScale>
    </FormModal>
  );
}

/** Nueva deuda desde el FAB. */
function NewDebtModal({ onClose }: { onClose: () => void }) {
  const [name, setName] = useState('');
  const [totalAmount, setTotalAmount] = useState('');
  const [interestRate, setInterestRate] = useState('');
  const [dueDate, setDueDate] = useState('');
  const createDebt = useCreateDebt();

  const parsedTotalAmount = Number(totalAmount);
  const isValid = name.trim().length > 0 && totalAmount.trim().length > 0 && Number.isFinite(parsedTotalAmount) && parsedTotalAmount > 0;

  const handleSubmit = async () => {
    if (!isValid) return;
    try {
      await createDebt.mutateAsync({
        name: name.trim(),
        totalAmount: parsedTotalAmount,
        interestRate: interestRate.trim() ? Number(interestRate) : null,
        dueDate: dueDate.trim() || null,
      });
      onClose();
    } catch {
      // El error queda visible en el banner de abajo (createDebt.error).
    }
  };

  return (
    <FormModal visible title="Nueva deuda" onClose={onClose}>
      <FormField label="Nombre" value={name} onChangeText={setName} placeholder="Préstamo, tarjeta, etc." />
      <FormField label="Monto total" value={totalAmount} onChangeText={setTotalAmount} placeholder="0" keyboardType="numeric" />
      <FormField label="Tasa de interés % m.v. (opcional)" value={interestRate} onChangeText={setInterestRate} placeholder="0" keyboardType="numeric" />
      <FormField label="Fecha límite AAAA-MM-DD (opcional)" value={dueDate} onChangeText={setDueDate} placeholder="2027-02-01" />
      {createDebt.isError && <ErrorBanner message={getApiErrorMessage(createDebt.error, 'No se pudo crear la deuda.')} />}
      <PressableScale
        className="h-11 flex-row items-center justify-center rounded-lg bg-primary"
        onPress={handleSubmit}
        disabled={!isValid || createDebt.isPending}
      >
        {createDebt.isPending ? (
          <ActivityIndicator color="white" />
        ) : (
          <Text className="text-base font-medium text-primary-foreground">Crear deuda</Text>
        )}
      </PressableScale>
    </FormModal>
  );
}

type View_ = 'DEBTS' | 'CARDS';

const FILTERS: { value: View_; label: string }[] = [
  { value: 'DEBTS', label: 'Deudas' },
  { value: 'CARDS', label: 'Tarjetas' },
];

type CardMovementModalState = { card: CreditCard; kind: 'CARGO' | 'ABONO' } | null;

export default function DeudasScreen() {
  const [view, setView] = useState<View_>('DEBTS');
  const [cardMovementModal, setCardMovementModal] = useState<CardMovementModalState>(null);
  const [newDebtModalOpen, setNewDebtModalOpen] = useState(false);

  const debtsQuery = useDebts();
  const cardsQuery = useCreditCards();

  const activeQuery = view === 'DEBTS' ? debtsQuery : cardsQuery;

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

      {activeQuery.isLoading ? (
        <View className="flex-1 items-center justify-center">
          <ActivityIndicator />
        </View>
      ) : activeQuery.isError ? (
        <View className="px-5">
          <ErrorBanner
            message={getApiErrorMessage(
              activeQuery.error,
              view === 'DEBTS' ? 'No se pudieron cargar las deudas.' : 'No se pudieron cargar las tarjetas.',
            )}
          />
        </View>
      ) : (
        <ScrollView contentContainerClassName="gap-3 px-5 pb-24" showsVerticalScrollIndicator={false}>
          {view === 'DEBTS'
            ? debtsQuery.data?.content.map((debt) => <DebtCard key={debt.id} debt={debt} />)
            : cardsQuery.data?.content.map((card) => (
                <CardTile
                  key={card.id}
                  card={card}
                  onCargo={() => setCardMovementModal({ card, kind: 'CARGO' })}
                  onAbono={() => setCardMovementModal({ card, kind: 'ABONO' })}
                />
              ))}
        </ScrollView>
      )}

      <Fab onPress={() => setNewDebtModalOpen(true)} />

      {cardMovementModal && (
        <CardMovementModal
          key={`${cardMovementModal.kind}-${cardMovementModal.card.id}`}
          card={cardMovementModal.card}
          kind={cardMovementModal.kind}
          onClose={() => setCardMovementModal(null)}
        />
      )}

      {newDebtModalOpen && <NewDebtModal onClose={() => setNewDebtModalOpen(false)} />}
    </SafeAreaView>
  );
}
