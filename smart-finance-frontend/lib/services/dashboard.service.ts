import { getTransactions } from "@/lib/services/transaction.service"
import { formatDateInput } from "@/lib/date"

export interface MonthlyTotals {
  incomeTotal: number
  expenseTotal: number
}

export async function getMonthlyTotals(month: number, year: number): Promise<MonthlyTotals> {
  const firstDay = new Date(year, month - 1, 1)
  const lastDay = new Date(year, month, 0)

  const from = formatDateInput(firstDay)
  const to = formatDateInput(lastDay)

  const [incomePage, expensePage] = await Promise.all([
    getTransactions({ type: "INCOME", from, to, page: 0, size: 200 }),
    getTransactions({ type: "EXPENSE", from, to, page: 0, size: 200 }),
  ])

  const incomeTotal = incomePage.content.reduce((sum, item) => sum + item.amount, 0)
  const expenseTotal = expensePage.content.reduce((sum, item) => sum + item.amount, 0)

  return {
    incomeTotal,
    expenseTotal,
  }
}
