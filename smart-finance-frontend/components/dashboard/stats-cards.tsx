"use client"

import { TrendingUp, TrendingDown, CreditCard, PiggyBank } from "lucide-react"
import { cn } from "@/lib/utils"
import { TapScale } from "@/components/motion/tap-scale"

interface StatCardProps {
  title: string
  value: string
  change: number
  icon: React.ReactNode
  trend: "up" | "down" | "neutral"
}

function StatCard({ title, value, change, icon, trend }: StatCardProps) {
  return (
    <TapScale className="rounded-xl bg-card border border-border p-5 transition-smooth hover:border-primary/30">
      <div className="flex items-center justify-between mb-3">
        <div className="flex h-10 w-10 items-center justify-center rounded-lg bg-secondary">
          {icon}
        </div>
        <div className={cn(
          "flex items-center gap-1 text-xs font-medium",
          trend === "up" && "text-success",
          trend === "down" && "text-destructive",
          trend === "neutral" && "text-muted-foreground"
        )}>
          {trend === "up" && <TrendingUp className="h-3 w-3" />}
          {trend === "down" && <TrendingDown className="h-3 w-3" />}
          <span>{change > 0 ? "+" : ""}{change}%</span>
        </div>
      </div>
      <p className="text-sm text-muted-foreground mb-1">{title}</p>
      <p className="text-xl font-semibold text-foreground">{value}</p>
    </TapScale>
  )
}

interface StatsCardsProps {
  monthlyIncome: number
  monthlyExpenses: number
  totalDebts: number
  savings: number
  incomeChange: number
  expensesChange: number
  debtsChange: number
  savingsChange: number
}

export function StatsCards({
  monthlyIncome,
  monthlyExpenses,
  totalDebts,
  savings,
  incomeChange,
  expensesChange,
  debtsChange,
  savingsChange,
}: StatsCardsProps) {
  return (
    <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-4 gap-4">
      <StatCard
        title="Ingresos del Mes"
        value={`$${monthlyIncome.toLocaleString('es-MX')}`}
        change={incomeChange}
        icon={<TrendingUp className="h-5 w-5 text-success" />}
        trend={incomeChange >= 0 ? "up" : "down"}
      />
      <StatCard
        title="Gastos del Mes"
        value={`$${monthlyExpenses.toLocaleString('es-MX')}`}
        change={expensesChange}
        icon={<TrendingDown className="h-5 w-5 text-destructive" />}
        trend={expensesChange > 0 ? "up" : "down"}
      />
      <StatCard
        title="Deudas Totales"
        value={`$${totalDebts.toLocaleString('es-MX')}`}
        change={debtsChange}
        icon={<CreditCard className="h-5 w-5 text-warning" />}
        trend={debtsChange === 0 ? "neutral" : debtsChange < 0 ? "up" : "down"}
      />
      <StatCard
        title="Ahorros"
        value={`$${savings.toLocaleString('es-MX')}`}
        change={savingsChange}
        icon={<PiggyBank className="h-5 w-5 text-primary" />}
        trend={savingsChange > 0 ? "up" : "down"}
      />
    </div>
  )
}
