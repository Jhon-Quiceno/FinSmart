"use client"

import { useEffect, useMemo, useState } from "react"
import { toast } from "sonner"
import { AppLayout } from "@/components/layout/app-layout"
import { BalanceCard } from "@/components/dashboard/balance-card"
import { StatsCards } from "@/components/dashboard/stats-cards"
import { IncomeExpensesChart } from "@/components/dashboard/income-expenses-chart"
import { ExpensesByCategoryChart } from "@/components/dashboard/expenses-by-category-chart"
import { AlertsPanel } from "@/components/dashboard/alerts-panel"
import { AIRecommendations } from "@/components/dashboard/ai-recommendations"
import { RecentTransactions } from "@/components/dashboard/recent-transactions"
import { Skeleton } from "@/components/ui/skeleton"
import { getApiErrorMessage } from "@/lib/api-client"
import { getMonthlyTotals } from "@/lib/services/dashboard.service"

export default function DashboardPage() {
  const now = useMemo(() => new Date(), [])
  const [monthlyIncome, setMonthlyIncome] = useState(0)
  const [monthlyExpenses, setMonthlyExpenses] = useState(0)
  const [isLoading, setIsLoading] = useState(true)

  useEffect(() => {
    const loadTotals = async () => {
      setIsLoading(true)
      try {
        const totals = await getMonthlyTotals(now.getMonth() + 1, now.getFullYear())
        setMonthlyIncome(totals.incomeTotal)
        setMonthlyExpenses(totals.expenseTotal)
      } catch (error) {
        toast.error(getApiErrorMessage(error, "No fue posible cargar el resumen del dashboard"))
      } finally {
        setIsLoading(false)
      }
    }

    void loadTotals()
  }, [now])

  const balance = monthlyIncome - monthlyExpenses

  return (
    <AppLayout>
      <div className="flex flex-col gap-6">
        <div>
          <h1 className="text-2xl font-bold text-foreground">Dashboard</h1>
          <p className="text-sm text-muted-foreground">Bienvenido de vuelta. Aqui esta tu resumen financiero.</p>
        </div>

        <div className="grid grid-cols-1 gap-6 lg:grid-cols-3">
          <div>
            {isLoading ? (
              <div className="rounded-xl border border-border bg-card p-6">
                <Skeleton className="h-6 w-40" />
                <Skeleton className="mt-4 h-10 w-56" />
                <Skeleton className="mt-6 h-16 w-full" />
              </div>
            ) : (
              <BalanceCard
                balance={balance}
                income={monthlyIncome}
                expenses={monthlyExpenses}
                percentageChange={0}
              />
            )}
          </div>
          <div className="lg:col-span-2">
            {isLoading ? (
              <div className="grid grid-cols-1 gap-4 sm:grid-cols-2 lg:grid-cols-4">
                {Array.from({ length: 4 }).map((_, index) => (
                  <Skeleton key={`dashboard-card-${index}`} className="h-32 w-full" />
                ))}
              </div>
            ) : (
              <StatsCards
                monthlyIncome={monthlyIncome}
                monthlyExpenses={monthlyExpenses}
                totalDebts={15000}
                savings={Math.max(balance, 0)}
                incomeChange={0}
                expensesChange={0}
                debtsChange={-12}
                savingsChange={0}
              />
            )}
          </div>
        </div>

        <div className="grid grid-cols-1 gap-6 lg:grid-cols-3">
          <div className="lg:col-span-2">
            <IncomeExpensesChart />
          </div>
          <ExpensesByCategoryChart />
        </div>

        <div className="grid grid-cols-1 gap-6 lg:grid-cols-3">
          <AlertsPanel />
          <AIRecommendations />
          <RecentTransactions />
        </div>
      </div>
    </AppLayout>
  )
}
