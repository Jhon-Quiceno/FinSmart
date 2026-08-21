import type { Expense } from "./types/expense"
import type { Income } from "./types/income"
import { mergeMovements } from "./merge-movements"

function expense(overrides: Partial<Expense> = {}): Expense {
  return {
    id: 1,
    amount: 100,
    description: "Gasto",
    date: "2026-08-10",
    paymentMethod: "CASH",
    categoryId: null,
    categoryName: null,
    recurringPaymentId: null,
    ...overrides,
  }
}

function income(overrides: Partial<Income> = {}): Income {
  return {
    id: 1,
    amount: 500,
    description: "Ingreso",
    date: "2026-08-10",
    categoryId: null,
    categoryName: null,
    ...overrides,
  }
}

describe("mergeMovements", () => {
  it("merges expenses and incomes sorted by date, most recent first", () => {
    const expenses = [expense({ id: 1, date: "2026-08-01" }), expense({ id: 2, date: "2026-08-15" })]
    const incomes = [income({ id: 1, date: "2026-08-10" })]

    const result = mergeMovements(expenses, incomes)

    expect(result.map((m) => m.date)).toEqual(["2026-08-15", "2026-08-10", "2026-08-01"])
  })

  it("keeps the original relative order for movements on the same date (stable sort)", () => {
    const expenses = [
      expense({ id: 1, date: "2026-08-10", description: "Primero" }),
      expense({ id: 2, date: "2026-08-10", description: "Segundo" }),
    ]
    const incomes = [income({ id: 1, date: "2026-08-10", description: "Tercero" })]

    const result = mergeMovements(expenses, incomes)

    expect(result.map((m) => m.description)).toEqual(["Primero", "Segundo", "Tercero"])
  })

  it("returns an empty list when there are no expenses or incomes", () => {
    expect(mergeMovements([], [])).toEqual([])
  })

  it("tags each movement with its type and carries the expense payment method", () => {
    const result = mergeMovements([expense({ paymentMethod: "CREDIT_CARD" })], [income()])

    const expenseMovement = result.find((m) => m.type === "EXPENSE")
    const incomeMovement = result.find((m) => m.type === "INCOME")

    expect(expenseMovement?.paymentMethod).toBe("CREDIT_CARD")
    expect(incomeMovement?.paymentMethod).toBeNull()
  })
})
