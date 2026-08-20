import { Stack } from 'expo-router';
import { Download } from 'lucide-react-native';
import { Pressable, ScrollView, View } from 'react-native';

import { AppText as Text } from '@/components/app-text';
import { SafeAreaView } from 'react-native-safe-area-context';

import { useIconColors } from '@/constants/icon-colors';
import { formatCurrency, formatDate, formatPercentage } from '@/lib/format';
import { CARD_SHADOW } from '@/lib/shadows';
import { mockMonthlyReport, mockReportMovements } from '@/lib/mock/reportes';

export default function ReportesScreen() {
  const report = mockMonthlyReport;
  const { ICON_COLOR_MUTED } = useIconColors();

  return (
    <SafeAreaView className="flex-1 bg-background">
      <Stack.Screen options={{ headerShown: true, title: 'Reportes' }} />
      <ScrollView contentContainerClassName="gap-5 px-5 py-6" showsVerticalScrollIndicator={false}>
        <View className="rounded-xl border border-border bg-card p-5" style={CARD_SHADOW}>
          <Text className="mb-3 text-base font-semibold text-card-foreground">Resumen del mes</Text>
          <View className="flex-row justify-between">
            <View>
              <Text className="text-xs text-muted-foreground">Ingresos</Text>
              <Text className="text-base font-semibold text-success">{formatCurrency(report.totalIncome)}</Text>
            </View>
            <View>
              <Text className="text-xs text-muted-foreground">Gastos</Text>
              <Text className="text-base font-semibold text-destructive">{formatCurrency(report.totalExpense)}</Text>
            </View>
            <View>
              <Text className="text-xs text-muted-foreground">Ahorro</Text>
              <Text className="text-base font-semibold text-foreground">{formatCurrency(report.savings)}</Text>
            </View>
          </View>
          <View className="mt-4 flex-row gap-4">
            <Text className="text-xs text-muted-foreground">% gasto/ingreso: {formatPercentage(report.expenseRatio)}</Text>
            <Text className="text-xs text-muted-foreground">% deuda/ingreso: {formatPercentage(report.debtRatio)}</Text>
          </View>
        </View>

        <View className="rounded-xl border border-border bg-card p-5" style={CARD_SHADOW}>
          <Text className="mb-3 text-base font-semibold text-card-foreground">Categorías con más gasto</Text>
          <View className="gap-3">
            {report.topCategories.map((category) => (
              <View key={category.categoryName} className="gap-1">
                <View className="flex-row justify-between">
                  <Text className="text-sm text-foreground">{category.categoryName}</Text>
                  <Text className="text-sm text-muted-foreground">{formatCurrency(category.totalAmount)}</Text>
                </View>
                <View className="h-2 overflow-hidden rounded-full bg-muted">
                  <View className="h-2 rounded-full bg-primary" style={{ width: `${category.percentage * 100}%` }} />
                </View>
              </View>
            ))}
          </View>
        </View>

        <View className="rounded-xl border border-border bg-card p-5" style={CARD_SHADOW}>
          <View className="mb-3 flex-row items-center justify-between">
            <Text className="text-base font-semibold text-card-foreground">Movimientos del período</Text>
            {/* TODO(Fase 0 backend): habilitar cuando exista GET /api/reports/export vía expo-sharing. */}
            <Pressable disabled className="flex-row items-center gap-1.5 opacity-50">
              <Download size={14} color={ICON_COLOR_MUTED} />
              <Text className="text-xs text-muted-foreground">Exportar CSV</Text>
            </Pressable>
          </View>
          <View className="gap-3">
            {mockReportMovements.map((row, index) => (
              <View key={index} className="flex-row justify-between">
                <View>
                  <Text className="text-sm text-foreground">{row.description}</Text>
                  <Text className="text-xs text-muted-foreground">{row.categoryName} · {formatDate(row.date)}</Text>
                </View>
                <Text className={`text-sm font-medium ${row.type === 'INCOME' ? 'text-success' : 'text-foreground'}`}>
                  {row.type === 'INCOME' ? '+' : '-'}
                  {formatCurrency(row.amount)}
                </Text>
              </View>
            ))}
          </View>
        </View>
      </ScrollView>
    </SafeAreaView>
  );
}
