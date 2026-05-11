"use client"

import { TrendingUp, TrendingDown, Wallet } from "lucide-react"
import { cn } from "@/lib/utils"

interface BalanceCardProps {
  balance: number
  income: number
  expenses: number
  percentageChange: number
}

export function BalanceCard({ balance, income, expenses, percentageChange }: BalanceCardProps) {
  const isPositive = percentageChange >= 0

  return (
    <div className="relative overflow-hidden rounded-xl bg-card border border-border p-6">
      {/* Subtle gradient overlay */}
      <div className="absolute inset-0 bg-gradient-to-br from-primary/5 to-transparent pointer-events-none" />
      
      <div className="relative">
        <div className="flex items-center justify-between mb-4">
          <div className="flex items-center gap-2">
            <div className="flex h-10 w-10 items-center justify-center rounded-lg bg-primary/10">
              <Wallet className="h-5 w-5 text-primary" />
            </div>
            <span className="text-sm font-medium text-muted-foreground">Balance Actual</span>
          </div>
          <div className={cn(
            "flex items-center gap-1 rounded-full px-2 py-1 text-xs font-medium",
            isPositive ? "bg-success/10 text-success" : "bg-destructive/10 text-destructive"
          )}>
            {isPositive ? <TrendingUp className="h-3 w-3" /> : <TrendingDown className="h-3 w-3" />}
            <span>{isPositive ? "+" : ""}{percentageChange}%</span>
          </div>
        </div>

        <p className="text-3xl font-bold text-foreground mb-6">
          ${balance.toLocaleString('es-MX', { minimumFractionDigits: 2 })}
        </p>

        <div className="grid grid-cols-2 gap-4">
          <div className="flex items-center gap-3">
            <div className="flex h-8 w-8 items-center justify-center rounded-lg bg-success/10">
              <TrendingUp className="h-4 w-4 text-success" />
            </div>
            <div>
              <p className="text-xs text-muted-foreground">Ingresos</p>
              <p className="text-sm font-semibold text-success">
                +${income.toLocaleString('es-MX', { minimumFractionDigits: 2 })}
              </p>
            </div>
          </div>
          <div className="flex items-center gap-3">
            <div className="flex h-8 w-8 items-center justify-center rounded-lg bg-destructive/10">
              <TrendingDown className="h-4 w-4 text-destructive" />
            </div>
            <div>
              <p className="text-xs text-muted-foreground">Gastos</p>
              <p className="text-sm font-semibold text-destructive">
                -${expenses.toLocaleString('es-MX', { minimumFractionDigits: 2 })}
              </p>
            </div>
          </div>
        </div>
      </div>
    </div>
  )
}
