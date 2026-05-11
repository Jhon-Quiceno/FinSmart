"use client"

import { ShoppingBag, Car, Coffee, Zap, Heart, GraduationCap, Shirt, MoreHorizontal } from "lucide-react"
import { cn } from "@/lib/utils"

export interface Expense {
  id: number
  description: string
  amount: number
  category: string
  paymentMethod: string
  date: string
}

interface ExpensesTableProps {
  expenses: Expense[]
  onViewExpense?: (expense: Expense) => void
}

const categoryIcons: Record<string, React.ReactNode> = {
  Alimentacion: <ShoppingBag className="h-4 w-4" />,
  Transporte: <Car className="h-4 w-4" />,
  Entretenimiento: <Coffee className="h-4 w-4" />,
  Servicios: <Zap className="h-4 w-4" />,
  Salud: <Heart className="h-4 w-4" />,
  Educacion: <GraduationCap className="h-4 w-4" />,
  Ropa: <Shirt className="h-4 w-4" />,
  Otros: <MoreHorizontal className="h-4 w-4" />,
}

const categoryColors: Record<string, string> = {
  Alimentacion: "bg-success/10 text-success",
  Transporte: "bg-destructive/10 text-destructive",
  Entretenimiento: "bg-warning/10 text-warning",
  Servicios: "bg-primary/10 text-primary",
  Salud: "bg-pink-500/10 text-pink-500",
  Educacion: "bg-blue-500/10 text-blue-500",
  Ropa: "bg-purple-500/10 text-purple-500",
  Otros: "bg-muted text-muted-foreground",
}

export function ExpensesTable({ expenses, onViewExpense }: ExpensesTableProps) {
  return (
    <div className="rounded-xl bg-card border border-border overflow-hidden">
      {/* Desktop Table */}
      <div className="hidden md:block overflow-x-auto">
        <table className="w-full">
          <thead>
            <tr className="border-b border-border bg-secondary/50">
              <th className="px-4 py-3 text-left text-sm font-medium text-muted-foreground">
                Descripcion
              </th>
              <th className="px-4 py-3 text-left text-sm font-medium text-muted-foreground">
                Categoria
              </th>
              <th className="px-4 py-3 text-left text-sm font-medium text-muted-foreground">
                Monto
              </th>
              <th className="px-4 py-3 text-left text-sm font-medium text-muted-foreground">
                Metodo
              </th>
              <th className="px-4 py-3 text-left text-sm font-medium text-muted-foreground">
                Fecha
              </th>
            </tr>
          </thead>
          <tbody>
            {expenses.map((expense) => (
              <tr
                key={expense.id}
                className="border-b border-border last:border-0 hover:bg-secondary/30 transition-smooth cursor-pointer"
                onClick={() => onViewExpense?.(expense)}
              >
                <td className="px-4 py-3">
                  <div className="flex items-center gap-3">
                    <div className={cn("flex h-8 w-8 items-center justify-center rounded-lg", categoryColors[expense.category])}>
                      {categoryIcons[expense.category]}
                    </div>
                    <span className="text-sm font-medium text-foreground">
                      {expense.description}
                    </span>
                  </div>
                </td>
                <td className="px-4 py-3">
                  <span className={cn("text-xs font-medium px-2 py-1 rounded-md", categoryColors[expense.category])}>
                    {expense.category}
                  </span>
                </td>
                <td className="px-4 py-3">
                  <span className="text-sm font-semibold text-destructive">
                    -${expense.amount.toLocaleString("es-MX", { minimumFractionDigits: 2 })}
                  </span>
                </td>
                <td className="px-4 py-3">
                  <span className="text-sm text-muted-foreground">
                    {expense.paymentMethod}
                  </span>
                </td>
                <td className="px-4 py-3">
                  <span className="text-sm text-muted-foreground">
                    {expense.date}
                  </span>
                </td>
              </tr>
            ))}
          </tbody>
        </table>
      </div>

      {/* Mobile List */}
      <div className="md:hidden divide-y divide-border">
        {expenses.map((expense) => (
          <div
            key={expense.id}
            className="p-4 hover:bg-secondary/30 transition-smooth"
            onClick={() => onViewExpense?.(expense)}
          >
            <div className="flex items-center justify-between mb-2">
              <div className="flex items-center gap-3">
                <div className={cn("flex h-10 w-10 items-center justify-center rounded-lg", categoryColors[expense.category])}>
                  {categoryIcons[expense.category]}
                </div>
                <div>
                  <p className="text-sm font-medium text-foreground">{expense.description}</p>
                  <p className="text-xs text-muted-foreground">{expense.category}</p>
                </div>
              </div>
              <div className="text-right">
                <p className="text-sm font-semibold text-destructive">
                  -${expense.amount.toLocaleString("es-MX", { minimumFractionDigits: 2 })}
                </p>
                <p className="text-xs text-muted-foreground">{expense.date}</p>
              </div>
            </div>
          </div>
        ))}
      </div>
    </div>
  )
}
