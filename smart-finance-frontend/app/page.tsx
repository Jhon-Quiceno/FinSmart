import { AppLayout } from "@/components/layout/app-layout"
import { BalanceCard } from "@/components/dashboard/balance-card"
import { StatsCards } from "@/components/dashboard/stats-cards"
import { IncomeExpensesChart } from "@/components/dashboard/income-expenses-chart"
import { ExpensesByCategoryChart } from "@/components/dashboard/expenses-by-category-chart"
import { AlertsPanel } from "@/components/dashboard/alerts-panel"
import { AIRecommendations } from "@/components/dashboard/ai-recommendations"
import { RecentTransactions } from "@/components/dashboard/recent-transactions"

export default function DashboardPage() {
  return (
    <AppLayout>
      <div className="space-y-6">
        {/* Header */}
        <div>
          <h1 className="text-2xl font-bold text-foreground">Dashboard</h1>
          <p className="text-sm text-muted-foreground">
            Bienvenido de vuelta. Aqui esta tu resumen financiero.
          </p>
        </div>

        {/* Balance and Stats */}
        <div className="grid grid-cols-1 lg:grid-cols-3 gap-6">
          <BalanceCard
            balance={45750.00}
            income={32000}
            expenses={20000}
            percentageChange={12.5}
          />
          <div className="lg:col-span-2">
            <StatsCards
              monthlyIncome={32000}
              monthlyExpenses={20000}
              totalDebts={15000}
              savings={8500}
              incomeChange={8}
              expensesChange={-5}
              debtsChange={-12}
              savingsChange={15}
            />
          </div>
        </div>

        {/* Charts Row */}
        <div className="grid grid-cols-1 lg:grid-cols-3 gap-6">
          <div className="lg:col-span-2">
            <IncomeExpensesChart />
          </div>
          <ExpensesByCategoryChart />
        </div>

        {/* Alerts, AI, and Transactions */}
        <div className="grid grid-cols-1 lg:grid-cols-3 gap-6">
          <AlertsPanel />
          <AIRecommendations />
          <RecentTransactions />
        </div>
      </div>
    </AppLayout>
  )
}
