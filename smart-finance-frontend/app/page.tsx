"use client"

import { useEffect, useMemo } from "react"
import { Sparkles } from "lucide-react"
import { toast } from "sonner"
import { AppLayout } from "@/components/layout/app-layout"
import { BalanceCard } from "@/components/dashboard/balance-card"
import { StatsCards } from "@/components/dashboard/stats-cards"
import { IncomeExpensesChart } from "@/components/dashboard/income-expenses-chart"
import { ExpensesByCategoryChart } from "@/components/dashboard/expenses-by-category-chart"
import { AlertsPanel } from "@/components/dashboard/alerts-panel"
import { AIRecommendations } from "@/components/dashboard/ai-recommendations"
import { RecentTransactions } from "@/components/dashboard/recent-transactions"
import { MonthEndPredictionCard } from "@/components/dashboard/month-end-prediction-card"
import { AiInsightsCard } from "@/components/dashboard/ai-insights-card"
import { Skeleton } from "@/components/ui/skeleton"
import { StaggerContainer, StaggerItem } from "@/components/motion/stagger"
import { Empty, EmptyDescription, EmptyHeader, EmptyMedia, EmptyTitle } from "@/components/ui/empty"
import { useAnalysisRecommendations, useAnalysisSummary } from "@/hooks/use-analysis"

function computePercentageChange(current: number, previous: number): number {
  if (previous === 0) return current === 0 ? 0 : 100

  return Number((((current - previous) / Math.abs(previous)) * 100).toFixed(1))
}

export default function DashboardPage() {
  const now = useMemo(() => new Date(), [])
  const year = now.getFullYear()
  const month = now.getMonth() + 1

  const { data: summary, isLoading: isSummaryLoading, error: summaryError } = useAnalysisSummary(year, month)
  const {
    data: recommendations,
    isLoading: isRecommendationsLoading,
    error: recommendationsError,
  } = useAnalysisRecommendations(year, month)

  useEffect(() => {
    if (summaryError) {
      toast.error(summaryError)
    }
  }, [summaryError])

  useEffect(() => {
    if (recommendationsError) {
      toast.error(recommendationsError)
    }
  }, [recommendationsError])

  const series = summary?.monthlySeries ?? []
  const currentPoint = series[series.length - 1]
  const previousPoint = series[series.length - 2]

  const balancePercentageChange = currentPoint
    ? computePercentageChange(
        currentPoint.totalIncome - currentPoint.totalExpense,
        previousPoint ? previousPoint.totalIncome - previousPoint.totalExpense : 0,
      )
    : 0

  const incomeChange = previousPoint && currentPoint
    ? computePercentageChange(currentPoint.totalIncome, previousPoint.totalIncome)
    : 0
  const expensesChange = previousPoint && currentPoint
    ? computePercentageChange(currentPoint.totalExpense, previousPoint.totalExpense)
    : 0
  const savingsChange = previousPoint && currentPoint
    ? computePercentageChange(
        currentPoint.totalIncome - currentPoint.totalExpense,
        previousPoint.totalIncome - previousPoint.totalExpense,
      )
    : 0

  const isLoading = isSummaryLoading
  const hasNoActivity =
    !isLoading &&
    !!summary &&
    summary.totalIncome === 0 &&
    summary.totalExpense === 0 &&
    summary.recentTransactions.length === 0

  return (
    <AppLayout>
      <div className="flex flex-col gap-6">
        <div>
          <h1 className="text-2xl font-bold text-foreground">Dashboard</h1>
          <p className="text-sm text-muted-foreground">Bienvenido de vuelta. Aqui esta tu resumen financiero.</p>
        </div>

        {hasNoActivity ? (
          <Empty>
            <EmptyHeader>
              <EmptyMedia variant="icon">
                <Sparkles />
              </EmptyMedia>
              <EmptyTitle>Todavia no hay movimientos este mes</EmptyTitle>
              <EmptyDescription>
                Registra tus primeros ingresos y gastos para ver tu resumen financiero, graficos y
                recomendaciones aqui.
              </EmptyDescription>
            </EmptyHeader>
          </Empty>
        ) : (
          <StaggerContainer className="flex flex-col gap-6">
            <StaggerItem className="grid grid-cols-1 gap-6 lg:grid-cols-3">
              <div>
                {isLoading || !summary ? (
                  <div className="rounded-xl border border-border bg-card p-6">
                    <Skeleton className="h-6 w-40" />
                    <Skeleton className="mt-4 h-10 w-56" />
                    <Skeleton className="mt-6 h-16 w-full" />
                  </div>
                ) : (
                  <BalanceCard
                    balance={summary.totalIncome - summary.totalExpense}
                    income={summary.totalIncome}
                    expenses={summary.totalExpense}
                    percentageChange={balancePercentageChange}
                  />
                )}
              </div>
              <div className="lg:col-span-2">
                {isLoading || !summary ? (
                  <div className="grid grid-cols-1 gap-4 sm:grid-cols-2 lg:grid-cols-4">
                    {Array.from({ length: 4 }).map((_, index) => (
                      <Skeleton key={`dashboard-card-${index}`} className="h-32 w-full" />
                    ))}
                  </div>
                ) : (
                  <StatsCards
                    monthlyIncome={summary.totalIncome}
                    monthlyExpenses={summary.totalExpense}
                    totalDebts={summary.totalDebt}
                    savings={summary.savings}
                    incomeChange={incomeChange}
                    expensesChange={expensesChange}
                    debtsChange={0}
                    savingsChange={savingsChange}
                  />
                )}
              </div>
            </StaggerItem>

            <StaggerItem className="grid grid-cols-1 gap-6 lg:grid-cols-3">
              <div className="lg:col-span-2">
                {isLoading || !summary ? (
                  <Skeleton className="h-[360px] w-full" />
                ) : (
                  <IncomeExpensesChart series={summary.monthlySeries} />
                )}
              </div>
              {isLoading || !summary ? (
                <Skeleton className="h-[360px] w-full" />
              ) : (
                <ExpensesByCategoryChart topCategories={summary.topCategories} />
              )}
            </StaggerItem>

            <StaggerItem className="grid grid-cols-1 gap-6 lg:grid-cols-2">
              <MonthEndPredictionCard />
              <AiInsightsCard />
            </StaggerItem>

            <StaggerItem className="grid grid-cols-1 gap-6 lg:grid-cols-3">
              {isRecommendationsLoading ? (
                <Skeleton className="h-64 w-full" />
              ) : (
                <AlertsPanel recommendations={recommendations} />
              )}
              {isRecommendationsLoading ? (
                <Skeleton className="h-64 w-full" />
              ) : (
                <AIRecommendations recommendations={recommendations} />
              )}
              {isLoading || !summary ? (
                <Skeleton className="h-64 w-full" />
              ) : (
                <RecentTransactions transactions={summary.recentTransactions} />
              )}
            </StaggerItem>
          </StaggerContainer>
        )}
      </div>
    </AppLayout>
  )
}
