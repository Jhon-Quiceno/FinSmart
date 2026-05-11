"use client"

import { ShoppingBag, Car, Coffee, Zap, MoreHorizontal } from "lucide-react"
import { cn } from "@/lib/utils"

interface Transaction {
  id: number
  description: string
  amount: number
  type: "income" | "expense"
  category: string
  date: string
  icon: React.ReactNode
}

const transactions: Transaction[] = [
  {
    id: 1,
    description: "Supermercado Walmart",
    amount: 1250,
    type: "expense",
    category: "Alimentacion",
    date: "Hoy",
    icon: <ShoppingBag className="h-4 w-4" />,
  },
  {
    id: 2,
    description: "Gasolina",
    amount: 850,
    type: "expense",
    category: "Transporte",
    date: "Hoy",
    icon: <Car className="h-4 w-4" />,
  },
  {
    id: 3,
    description: "Salario Quincenal",
    amount: 15000,
    type: "income",
    category: "Salario",
    date: "Ayer",
    icon: <Zap className="h-4 w-4" />,
  },
  {
    id: 4,
    description: "Starbucks",
    amount: 180,
    type: "expense",
    category: "Entretenimiento",
    date: "Ayer",
    icon: <Coffee className="h-4 w-4" />,
  },
  {
    id: 5,
    description: "Pago Luz CFE",
    amount: 520,
    type: "expense",
    category: "Servicios",
    date: "20 Mar",
    icon: <Zap className="h-4 w-4" />,
  },
]

export function RecentTransactions() {
  return (
    <div className="rounded-xl bg-card border border-border p-5">
      <div className="flex items-center justify-between mb-4">
        <h3 className="text-lg font-semibold text-foreground">Transacciones Recientes</h3>
        <button className="text-sm text-primary hover:text-primary/80 transition-smooth">
          Ver todas
        </button>
      </div>

      <div className="space-y-3">
        {transactions.map((transaction) => (
          <div
            key={transaction.id}
            className="flex items-center gap-3 rounded-lg p-3 hover:bg-secondary/50 transition-smooth"
          >
            <div
              className={cn(
                "flex h-10 w-10 items-center justify-center rounded-lg shrink-0",
                transaction.type === "income" ? "bg-success/10 text-success" : "bg-secondary text-muted-foreground"
              )}
            >
              {transaction.icon}
            </div>
            <div className="flex-1 min-w-0">
              <p className="text-sm font-medium text-foreground truncate">
                {transaction.description}
              </p>
              <p className="text-xs text-muted-foreground">
                {transaction.category} · {transaction.date}
              </p>
            </div>
            <div className="text-right shrink-0">
              <p
                className={cn(
                  "text-sm font-semibold",
                  transaction.type === "income" ? "text-success" : "text-foreground"
                )}
              >
                {transaction.type === "income" ? "+" : "-"}$
                {transaction.amount.toLocaleString("es-MX")}
              </p>
            </div>
          </div>
        ))}
      </div>
    </div>
  )
}
