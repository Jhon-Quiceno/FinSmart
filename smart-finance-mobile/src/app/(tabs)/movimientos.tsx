import { ArrowDownRight, ArrowUpRight, Repeat, Search, Sparkles, Tag } from 'lucide-react-native';
import { useMemo, useState } from 'react';
import { ActivityIndicator, FlatList, ScrollView, View } from 'react-native';

import { AppText as Text, AppTextInput as TextInput } from '@/components/app-text';
import { SafeAreaView } from 'react-native-safe-area-context';

import { Fab } from '@/components/fab';
import { PressableScale } from '@/components/pressable-scale';
import { NotificationBell } from '@/components/notification-bell';
import { ProfileBubble } from '@/components/profile-bubble';
import { QuickAddExpenseModal } from '@/components/quick-add-expense-modal';
import { SegmentedControl } from '@/components/segmented-control';
import { useIconColors } from '@/constants/icon-colors';
import { useCategories } from '@/hooks/use-categories';
import { useExpenses } from '@/hooks/use-expenses';
import { useIncomes } from '@/hooks/use-incomes';
import { useRecurringPayments } from '@/hooks/use-recurring-payments';
import { getApiErrorMessage } from '@/lib/api-client';
import { getCategoryIcon } from '@/lib/category-icons';
import { formatCurrency, formatDate } from '@/lib/format';
import { mergeMovements, type Movement } from '@/lib/merge-movements';
import type { CategoryType } from '@/lib/types/category';
import type { PaymentMethodType } from '@/lib/types/expense';
import { FREQUENCY_LABELS } from '@/constants/recurring-frequency';
import { CARD_SHADOW } from '@/lib/shadows';

const PAYMENT_METHOD_LABELS: Record<PaymentMethodType, string> = {
  CASH: 'Efectivo',
  DEBIT_CARD: 'Débito',
  CREDIT_CARD: 'Crédito',
  TRANSFER: 'Transferencia',
  OTHER: 'Otro',
};

type Filter = 'ALL' | 'INCOME' | 'EXPENSE';
type Section = 'MOVS' | 'CATS' | 'SERVS';

const FILTERS: { value: Filter; label: string }[] = [
  { value: 'ALL', label: 'Todos' },
  { value: 'INCOME', label: 'Ingresos' },
  { value: 'EXPENSE', label: 'Gastos' },
];

const SECTIONS: { value: Section; label: string }[] = [
  { value: 'MOVS', label: 'Movimientos' },
  { value: 'CATS', label: 'Categorías' },
  { value: 'SERVS', label: 'Servicios' },
];

const CATEGORY_FILTERS: { value: CategoryType; label: string }[] = [
  { value: 'EXPENSE', label: 'Gastos' },
  { value: 'INCOME', label: 'Ingresos' },
];

function MovementRow({ movement }: { movement: Movement }) {
  const isIncome = movement.type === 'INCOME';
  const { ICON_COLOR_SUCCESS, ICON_COLOR_DESTRUCTIVE } = useIconColors();
  const CategoryIcon = getCategoryIcon(movement.categoryName) ?? (isIncome ? ArrowUpRight : ArrowDownRight);
  return (
    <View className="flex-row items-center justify-between rounded-xl bg-card p-4" style={CARD_SHADOW}>
      <View className="flex-1 flex-row items-center gap-3">
        <View className={`h-12 w-12 items-center justify-center rounded-full ${isIncome ? 'bg-success/15' : 'bg-destructive/10'}`}>
          <CategoryIcon size={20} color={isIncome ? ICON_COLOR_SUCCESS : ICON_COLOR_DESTRUCTIVE} />
        </View>
        <View className="flex-1">
          <Text className="text-sm font-semibold text-foreground" numberOfLines={1}>{movement.description}</Text>
          <Text className="text-xs text-muted-foreground">
            {movement.categoryName} · {formatDate(movement.date)}
            {movement.paymentMethod ? ` · ${PAYMENT_METHOD_LABELS[movement.paymentMethod]}` : ''}
          </Text>
        </View>
      </View>
      <Text className={`text-base font-semibold ${isIncome ? 'text-success' : 'text-foreground'}`}>
        {isIncome ? '+' : '-'}
        {formatCurrency(movement.amount)}
      </Text>
    </View>
  );
}

function MovimientosSection() {
  const [filter, setFilter] = useState<Filter>('ALL');
  const [query, setQuery] = useState('');
  const { ICON_COLOR_SUCCESS, ICON_COLOR_MUTED } = useIconColors();

  const expensesQuery = useExpenses({});
  const incomesQuery = useIncomes({});

  const isLoading = expensesQuery.isPending || incomesQuery.isPending;
  const isError = expensesQuery.isError || incomesQuery.isError;

  const movements = useMemo(
    () => mergeMovements(expensesQuery.data?.content ?? [], incomesQuery.data?.content ?? []),
    [expensesQuery.data, incomesQuery.data],
  );

  const filtered = useMemo(() => {
    const byType = filter === 'ALL' ? movements : movements.filter((m) => m.type === filter);
    const normalizedQuery = query.trim().toLowerCase();
    if (!normalizedQuery) return byType;
    return byType.filter(
      (m) =>
        m.description.toLowerCase().includes(normalizedQuery) ||
        (m.categoryName?.toLowerCase().includes(normalizedQuery) ?? false),
    );
  }, [movements, filter, query]);

  return (
    <View className="flex-1">
      <View className="gap-3 px-5 pb-3 pt-4">
        <View className="relative justify-center">
          <Search size={16} color={ICON_COLOR_MUTED} style={{ position: 'absolute', left: 12, zIndex: 1 }} />
          <TextInput
            className="rounded-xl border border-border bg-background py-2.5 pl-9 pr-3 text-sm text-foreground"
            placeholder="Buscar movimientos..."
            placeholderTextColor={ICON_COLOR_MUTED}
            value={query}
            onChangeText={setQuery}
          />
        </View>
        <SegmentedControl options={FILTERS} value={filter} onChange={setFilter} />
      </View>

      {/* La sugerencia de categoría por IA (POST /api/ai/categorize) se conecta en el formulario
          de alta rápida que abre el FAB, no acá — ver QuickAddExpenseModal. */}
      <View className="mx-5 mb-3 flex-row items-start gap-3 rounded-xl border border-border bg-card p-4" style={CARD_SHADOW}>
        <View className="h-10 w-10 items-center justify-center rounded-full bg-primary/10">
          <Sparkles size={18} color={ICON_COLOR_SUCCESS} />
        </View>
        <View className="flex-1">
          <Text className="text-sm font-semibold text-foreground">Sugerencia inteligente</Text>
          <Text className="mt-0.5 text-xs text-muted-foreground">
            Al agregar un gasto nuevo, la IA sugiere una categoría automáticamente.
          </Text>
        </View>
      </View>

      {isLoading && (
        <View className="items-center py-8">
          <ActivityIndicator />
        </View>
      )}

      {!isLoading && isError && (
        <View className="mx-5 items-center gap-3 rounded-xl border border-border bg-card p-5" style={CARD_SHADOW}>
          <Text className="text-center text-sm text-muted-foreground">
            {getApiErrorMessage(expensesQuery.error ?? incomesQuery.error, 'No se pudieron cargar los movimientos.')}
          </Text>
          <PressableScale
            onPress={() => {
              void expensesQuery.refetch();
              void incomesQuery.refetch();
            }}
            className="rounded-full bg-primary px-4 py-2"
          >
            <Text className="text-xs font-semibold text-primary-foreground">Reintentar</Text>
          </PressableScale>
        </View>
      )}

      {!isLoading && !isError && (
        <FlatList
          data={filtered}
          keyExtractor={(item) => `${item.type}-${item.id}`}
          renderItem={({ item }) => <MovementRow movement={item} />}
          contentContainerClassName="gap-3 px-5 pb-24"
          ListEmptyComponent={<Text className="py-8 text-center text-sm text-muted-foreground">Sin movimientos</Text>}
        />
      )}
    </View>
  );
}

function CategoriasSection() {
  const [type, setType] = useState<CategoryType>('EXPENSE');
  const { ICON_COLOR_MUTED } = useIconColors();

  const categoriesQuery = useCategories(type);
  const categories = categoriesQuery.data ?? [];

  return (
    <ScrollView className="flex-1" contentContainerClassName="gap-3 px-5 pb-24 pt-4" showsVerticalScrollIndicator={false}>
      <Text className="text-sm text-muted-foreground">
        Categorías usadas para clasificar tus ingresos y gastos
      </Text>

      <SegmentedControl options={CATEGORY_FILTERS} value={type} onChange={setType} />

      {categoriesQuery.isPending && (
        <View className="items-center py-8">
          <ActivityIndicator />
        </View>
      )}

      {!categoriesQuery.isPending && categoriesQuery.isError && (
        <View className="items-center gap-3 rounded-xl border border-border bg-card p-5" style={CARD_SHADOW}>
          <Text className="text-center text-sm text-muted-foreground">
            {getApiErrorMessage(categoriesQuery.error, 'No se pudieron cargar las categorías.')}
          </Text>
          <PressableScale onPress={() => categoriesQuery.refetch()} className="rounded-full bg-primary px-4 py-2">
            <Text className="text-xs font-semibold text-primary-foreground">Reintentar</Text>
          </PressableScale>
        </View>
      )}

      {!categoriesQuery.isPending &&
        !categoriesQuery.isError &&
        categories.map((category) => (
          <PressableScale
            key={category.id}
            className="flex-row items-center gap-3 rounded-xl border border-border bg-card p-4"
            style={CARD_SHADOW}
          >
            <View className="h-9 w-9 items-center justify-center rounded-full bg-secondary">
              <Tag size={16} color={ICON_COLOR_MUTED} />
            </View>
            <Text className="flex-1 text-sm font-medium text-foreground">{category.name}</Text>
          </PressableScale>
        ))}
      {!categoriesQuery.isPending && !categoriesQuery.isError && categories.length === 0 && (
        <Text className="py-8 text-center text-sm text-muted-foreground">Sin categorías</Text>
      )}
    </ScrollView>
  );
}

function ServiciosSection() {
  const { ICON_COLOR_MUTED } = useIconColors();
  const recurringPaymentsQuery = useRecurringPayments();
  const payments = recurringPaymentsQuery.data?.content ?? [];

  return (
    <ScrollView className="flex-1" contentContainerClassName="gap-3 px-5 pb-24 pt-4" showsVerticalScrollIndicator={false}>
      <Text className="mb-1 text-sm text-muted-foreground">
        Suscripciones y pagos recurrentes registrados
      </Text>

      {recurringPaymentsQuery.isPending && (
        <View className="items-center py-8">
          <ActivityIndicator />
        </View>
      )}

      {!recurringPaymentsQuery.isPending && recurringPaymentsQuery.isError && (
        <View className="items-center gap-3 rounded-xl border border-border bg-card p-5" style={CARD_SHADOW}>
          <Text className="text-center text-sm text-muted-foreground">
            {getApiErrorMessage(recurringPaymentsQuery.error, 'No se pudieron cargar los servicios.')}
          </Text>
          <PressableScale
            onPress={() => recurringPaymentsQuery.refetch()}
            className="rounded-full bg-primary px-4 py-2"
          >
            <Text className="text-xs font-semibold text-primary-foreground">Reintentar</Text>
          </PressableScale>
        </View>
      )}

      {!recurringPaymentsQuery.isPending &&
        !recurringPaymentsQuery.isError &&
        payments.map((payment) => (
          <View key={payment.id} className="flex-row items-center gap-3 rounded-xl border border-border bg-card p-4" style={CARD_SHADOW}>
            <View className="h-9 w-9 items-center justify-center rounded-full bg-secondary">
              <Repeat size={16} color={ICON_COLOR_MUTED} />
            </View>
            <View className="flex-1">
              <Text className="text-sm font-medium text-foreground">{payment.name}</Text>
              <Text className="text-xs text-muted-foreground">
                {FREQUENCY_LABELS[payment.frequency]} · próximo cobro {formatDate(payment.nextPaymentDate)}
              </Text>
            </View>
            <View className="items-end gap-1">
              <Text className="text-sm font-semibold text-foreground">{formatCurrency(payment.amount)}</Text>
              <View className={`rounded-full px-2 py-0.5 ${payment.isActive ? 'bg-success/15' : 'bg-muted'}`}>
                <Text className={`text-[10px] font-medium ${payment.isActive ? 'text-success' : 'text-muted-foreground'}`}>
                  {payment.isActive ? 'Activo' : 'Pausado'}
                </Text>
              </View>
            </View>
          </View>
        ))}
      {!recurringPaymentsQuery.isPending && !recurringPaymentsQuery.isError && payments.length === 0 && (
        <Text className="py-8 text-center text-sm text-muted-foreground">Sin servicios registrados</Text>
      )}
    </ScrollView>
  );
}

export default function MovimientosScreen() {
  const [section, setSection] = useState<Section>('MOVS');
  const [quickAddVisible, setQuickAddVisible] = useState(false);

  return (
    <SafeAreaView className="flex-1 bg-background" edges={['top']}>
      <View className="flex-row items-center justify-between px-5 pb-2 pt-6">
        <Text className="text-2xl font-bold text-foreground">Movimientos</Text>
        <View className="flex-row items-center gap-2">
          <NotificationBell />
          <ProfileBubble />
        </View>
      </View>

      <View className="px-5 pb-3">
        <SegmentedControl options={SECTIONS} value={section} onChange={setSection} />
      </View>

      {section === 'MOVS' && <MovimientosSection />}
      {section === 'CATS' && <CategoriasSection />}
      {section === 'SERVS' && <ServiciosSection />}

      <Fab onPress={() => setQuickAddVisible(true)} />
      <QuickAddExpenseModal visible={quickAddVisible} onClose={() => setQuickAddVisible(false)} />
    </SafeAreaView>
  );
}
