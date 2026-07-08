"use client"

import { ShoppingBag, TrendingUp } from "lucide-react"
import { cn } from "@/lib/utils"
import type { RecentTransaction } from "@/lib/types/analysis"
import { StaggerContainer, StaggerItem } from "@/components/motion/stagger"
import { HoverCard, HoverCardContent, HoverCardTrigger } from "@/components/ui/hover-card"

interface RecentTransactionsProps {
  transactions: RecentTransaction[]
}

function formatTransactionDate(value: string): string {
  const date = new Date(value)
  if (Number.isNaN(date.getTime())) return value

  return date.toLocaleDateString("es-MX", { day: "2-digit", month: "short" })
}

export function RecentTransactions({ transactions }: RecentTransactionsProps) {
  return (
    <div className="rounded-xl bg-card border border-border p-5">
      <div className="flex items-center justify-between mb-4">
        <h3 className="text-lg font-semibold text-foreground">Transacciones Recientes</h3>
      </div>

      {transactions.length === 0 ? (
        <div className="rounded-lg border border-dashed border-border p-6 text-center text-sm text-muted-foreground">
          Todavia no tienes movimientos registrados en este periodo.
        </div>
      ) : (
        <StaggerContainer className="space-y-3">
          {transactions.map((transaction) => (
            <StaggerItem
              key={`${transaction.type}-${transaction.id}`}
              className="flex items-center gap-3 rounded-lg p-3 hover:bg-secondary/50 transition-smooth"
            >
              <div
                className={cn(
                  "flex h-10 w-10 items-center justify-center rounded-lg shrink-0",
                  transaction.type === "INCOME" ? "bg-success/10 text-success" : "bg-secondary text-muted-foreground"
                )}
              >
                {transaction.type === "INCOME" ? (
                  <TrendingUp className="h-4 w-4" />
                ) : (
                  <ShoppingBag className="h-4 w-4" />
                )}
              </div>
              <div className="flex-1 min-w-0">
                <HoverCard>
                  <HoverCardTrigger asChild>
                    <p className="text-sm font-medium text-foreground truncate cursor-default">
                      {transaction.description}
                    </p>
                  </HoverCardTrigger>
                  <HoverCardContent className="w-auto max-w-xs">
                    <p className="text-sm text-foreground">{transaction.description}</p>
                    <p className="mt-1 text-xs text-muted-foreground">
                      {transaction.categoryName ?? "Sin categoria"} · {formatTransactionDate(transaction.date)}
                    </p>
                  </HoverCardContent>
                </HoverCard>
                <p className="text-xs text-muted-foreground">
                  {transaction.categoryName ?? "Sin categoria"} · {formatTransactionDate(transaction.date)}
                </p>
              </div>
              <div className="text-right shrink-0">
                <p
                  className={cn(
                    "text-sm font-semibold",
                    transaction.type === "INCOME" ? "text-success" : "text-foreground"
                  )}
                >
                  {transaction.type === "INCOME" ? "+" : "-"}$
                  {transaction.amount.toLocaleString("es-MX")}
                </p>
              </div>
            </StaggerItem>
          ))}
        </StaggerContainer>
      )}
    </div>
  )
}
