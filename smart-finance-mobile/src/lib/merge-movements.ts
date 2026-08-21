import type { Expense, PaymentMethodType } from "./types/expense"
import type { Income } from "./types/income"

export type MovementType = "INCOME" | "EXPENSE"

// No hay un endpoint unico de "movimientos" en el backend: se piden gastos e ingresos por
// separado y se combinan aca para la lista de Movimientos.
export interface Movement {
  id: number
  type: MovementType
  amount: number
  description: string
  date: string
  paymentMethod: PaymentMethodType | null
  categoryName: string | null
}

export function mergeMovements(expenses: Expense[], incomes: Income[]): Movement[] {
  const expenseMovements: Movement[] = expenses.map((expense) => ({
    id: expense.id,
    type: "EXPENSE",
    amount: expense.amount,
    description: expense.description ?? "",
    date: expense.date,
    paymentMethod: expense.paymentMethod,
    categoryName: expense.categoryName,
  }))

  const incomeMovements: Movement[] = incomes.map((income) => ({
    id: income.id,
    type: "INCOME",
    amount: income.amount,
    description: income.description ?? "",
    date: income.date,
    paymentMethod: null,
    categoryName: income.categoryName,
  }))

  // Array.prototype.sort is a stable sort (guaranteed since ES2019 / all supported RN engines),
  // so movements sharing the same date keep their original relative order instead of jittering
  // on every re-render.
  return [...expenseMovements, ...incomeMovements].sort((a, b) => (a.date < b.date ? 1 : a.date > b.date ? -1 : 0))
}
