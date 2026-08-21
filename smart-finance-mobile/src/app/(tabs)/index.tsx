import { AlertTriangle, ArrowDownRight, ArrowUpRight, Info, TrendingUp } from 'lucide-react-native';
import { ActivityIndicator, ScrollView, View } from 'react-native';

import { AppText as Text } from '@/components/app-text';
import { SafeAreaView } from 'react-native-safe-area-context';

import { NotificationBell } from '@/components/notification-bell';
import { PressableScale } from '@/components/pressable-scale';
import { ProfileBubble } from '@/components/profile-bubble';
import { useIconColors } from '@/constants/icon-colors';
import { useAuth } from '@/context/auth-context';
import { useAnalysisSummary, useRecommendations } from '@/hooks/use-analysis';
import { getApiErrorMessage } from '@/lib/api-client';
import { getCategoryIcon } from '@/lib/category-icons';
import { formatCurrency, formatPercentage } from '@/lib/format';
import { CARD_SHADOW } from '@/lib/shadows';

const MONTH_NAMES = [
  'enero', 'febrero', 'marzo', 'abril', 'mayo', 'junio',
  'julio', 'agosto', 'septiembre', 'octubre', 'noviembre', 'diciembre',
];

const SEVERITY_ICON = { HIGH: AlertTriangle, MEDIUM: Info, LOW: TrendingUp } as const;
const SEVERITY_BG = { HIGH: 'bg-destructive/10 border-destructive/20', MEDIUM: 'bg-warning/10 border-warning/20', LOW: 'bg-success/10 border-success/20' } as const;
const SEVERITY_TITLE = { HIGH: 'text-destructive', MEDIUM: 'text-warning', LOW: 'text-success' } as const;
const SEVERITY_LABEL = { HIGH: 'Atención', MEDIUM: 'A tener en cuenta', LOW: 'Buena noticia' } as const;

// El backend no expone un presupuesto por categoría (a diferencia del mock previo, que lo
// inventaba). En su lugar coloreamos la barra según qué tan grande es la categoría dentro del
// gasto total del mes (AnalysisSummary.topCategories[].percentage) — sigue siendo información
// real, solo que la lectura pasa de "vs. meta" a "peso relativo del gasto".
function categoryWeightColor(percentage: number) {
  if (percentage >= 0.35) return { bar: 'bg-destructive', amount: 'text-destructive' };
  if (percentage >= 0.2) return { bar: 'bg-warning', amount: 'text-warning' };
  return { bar: 'bg-primary', amount: 'text-muted-foreground' };
}

export default function DashboardScreen() {
  const { user } = useAuth();
  const { ICON_COLOR_DESTRUCTIVE, ICON_COLOR_SUCCESS, ICON_COLOR_WARNING } = useIconColors();
  const SEVERITY_COLOR = { HIGH: ICON_COLOR_DESTRUCTIVE, MEDIUM: ICON_COLOR_WARNING, LOW: ICON_COLOR_SUCCESS } as const;

  const summaryQuery = useAnalysisSummary();
  const recommendationsQuery = useRecommendations();

  const firstName = user?.name?.split(' ')[0] ?? '';

  if (summaryQuery.isPending) {
    return (
      <SafeAreaView className="flex-1 items-center justify-center bg-background" edges={['top']}>
        <ActivityIndicator size="large" />
      </SafeAreaView>
    );
  }

  if (summaryQuery.isError) {
    return (
      <SafeAreaView className="flex-1 items-center justify-center gap-4 bg-background px-8" edges={['top']}>
        <Text className="text-center text-sm text-muted-foreground">
          {getApiErrorMessage(summaryQuery.error, 'No se pudo cargar el resumen del mes.')}
        </Text>
        <PressableScale
          onPress={() => summaryQuery.refetch()}
          className="rounded-full bg-primary px-5 py-2.5"
        >
          <Text className="text-sm font-semibold text-primary-foreground">Reintentar</Text>
        </PressableScale>
      </SafeAreaView>
    );
  }

  const summary = summaryQuery.data;
  const recommendations = recommendationsQuery.data ?? [];
  const recentTransactions = summary.recentTransactions;

  return (
    <SafeAreaView className="flex-1 bg-background" edges={['top']}>
      <ScrollView contentContainerClassName="gap-5 px-5 py-6" showsVerticalScrollIndicator={false}>
        <View className="flex-row items-start justify-between">
          <View>
            <Text className="text-sm text-muted-foreground">
              {MONTH_NAMES[summary.periodMonth - 1]} {summary.periodYear}
            </Text>
            <Text className="text-2xl font-bold text-foreground">Hola{firstName ? `, ${firstName}` : ''}</Text>
          </View>
          <View className="flex-row items-center gap-2">
            <NotificationBell />
            <ProfileBubble />
          </View>
        </View>

        <View className="gap-4 overflow-hidden rounded-xl bg-primary p-5">
          <View
            className="absolute -right-10 -top-10 h-40 w-40 rounded-full bg-white/10"
            pointerEvents="none"
          />
          <Text className="text-sm text-primary-foreground/80">Balance del mes</Text>
          <Text className="text-3xl font-bold text-primary-foreground">{formatCurrency(summary.savings)}</Text>
          <View className="flex-row gap-6 border-t border-white/20 pt-3">
            <View className="flex-row items-center gap-1.5">
              <ArrowUpRight size={14} color="white" />
              <Text className="text-xs text-primary-foreground/90">{formatCurrency(summary.totalIncome)}</Text>
            </View>
            <View className="flex-row items-center gap-1.5">
              <ArrowDownRight size={14} color="white" />
              <Text className="text-xs text-primary-foreground/90">{formatCurrency(summary.totalExpense)}</Text>
            </View>
          </View>
        </View>

        <View className="rounded-xl border border-border bg-card p-5" style={CARD_SHADOW}>
          <Text className="mb-3 text-base font-semibold text-card-foreground">Categorías principales</Text>
          <View className="gap-3">
            {summary.topCategories.map((category) => {
              const color = categoryWeightColor(category.percentage);
              return (
                <View key={category.categoryName} className="gap-1">
                  <View className="flex-row justify-between">
                    <Text className="text-sm text-foreground">{category.categoryName}</Text>
                    <Text className={`text-sm ${color.amount}`}>
                      {formatCurrency(category.totalAmount)} · {formatPercentage(category.percentage)}
                    </Text>
                  </View>
                  <View className="h-2 overflow-hidden rounded-full bg-muted">
                    <View
                      className={`h-2 rounded-full ${color.bar}`}
                      style={{ width: `${Math.min(category.percentage * 100, 100)}%` }}
                    />
                  </View>
                </View>
              );
            })}
            {summary.topCategories.length === 0 && (
              <Text className="text-sm text-muted-foreground">Sin gastos registrados este mes</Text>
            )}
          </View>
        </View>

        <View className="rounded-xl border border-border bg-card p-5" style={CARD_SHADOW}>
          <Text className="mb-3 text-base font-semibold text-card-foreground">Alertas e insights</Text>
          <View className="gap-2.5">
            {recommendations.map((rec, index) => {
              const Icon = SEVERITY_ICON[rec.severity];
              return (
                <View
                  key={index}
                  className={`flex-row items-start gap-2.5 rounded-lg border p-3 ${SEVERITY_BG[rec.severity]}`}
                >
                  <Icon size={16} color={SEVERITY_COLOR[rec.severity]} style={{ marginTop: 1 }} />
                  <View className="flex-1">
                    <Text className={`text-xs font-semibold ${SEVERITY_TITLE[rec.severity]}`}>
                      {SEVERITY_LABEL[rec.severity]}
                    </Text>
                    <Text className="mt-0.5 text-sm text-muted-foreground">{rec.message}</Text>
                  </View>
                </View>
              );
            })}
            {recommendations.length === 0 && (
              <Text className="text-sm text-muted-foreground">Sin alertas por ahora</Text>
            )}
          </View>
        </View>

        <View className="rounded-xl border border-border bg-card p-5" style={CARD_SHADOW}>
          <Text className="mb-3 text-base font-semibold text-card-foreground">Transacciones recientes</Text>
          <View className="gap-4">
            {recentTransactions.map((tx) => {
              const isIncome = tx.type === 'INCOME';
              const CategoryIcon = getCategoryIcon(tx.categoryName) ?? (isIncome ? ArrowUpRight : ArrowDownRight);
              return (
              <View key={tx.id} className="flex-row items-center justify-between">
                <View className="flex-1 flex-row items-center gap-3">
                  <View
                    className={`h-9 w-9 items-center justify-center rounded-full ${
                      isIncome ? 'bg-success/15' : 'bg-destructive/10'
                    }`}
                  >
                    <CategoryIcon size={16} color={isIncome ? ICON_COLOR_SUCCESS : ICON_COLOR_DESTRUCTIVE} />
                  </View>
                  <View className="flex-1">
                    <Text className="text-sm font-medium text-foreground" numberOfLines={1}>
                      {tx.description}
                    </Text>
                    <Text className="text-xs text-muted-foreground">{tx.categoryName}</Text>
                  </View>
                </View>
                <Text className={`text-sm font-semibold ${tx.type === 'INCOME' ? 'text-success' : 'text-foreground'}`}>
                  {tx.type === 'INCOME' ? '+' : '-'}
                  {formatCurrency(tx.amount)}
                </Text>
              </View>
              );
            })}
            {recentTransactions.length === 0 && (
              <Text className="py-2 text-center text-sm text-muted-foreground">Sin movimientos recientes</Text>
            )}
          </View>
        </View>

        <Text className="text-center text-xs text-muted-foreground">
          Gasto/ingreso mostrados: {formatPercentage(summary.expenseRatio)} de tu ingreso
        </Text>
      </ScrollView>
    </SafeAreaView>
  );
}
