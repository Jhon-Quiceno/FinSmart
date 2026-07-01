import { describe, expect, it } from "vitest"
import { categorySchema } from "./category.schema"
import { incomeSchema } from "./income.schema"
import { expenseSchema } from "./expense.schema"
import { getTodayDateInput } from "../date"

describe("categorySchema", () => {
  it("acepta una categoria valida", () => {
    const result = categorySchema.safeParse({
      name: "Salario",
      type: "INCOME",
    })

    expect(result.success).toBe(true)
  })

  it("rechaza un tipo invalido", () => {
    const result = categorySchema.safeParse({
      name: "Transporte",
      type: "OTHER",
    })

    expect(result.success).toBe(false)
  })
})

describe("incomeSchema", () => {
  it("acepta ingresos validos", () => {
    const result = incomeSchema.safeParse({
      amount: 1000,
      description: "Freelance",
      date: "2026-03-01",
      source: "Freelance",
      categoryId: 2,
    })

    expect(result.success).toBe(true)
  })

  it("rechaza montos en cero", () => {
    const result = incomeSchema.safeParse({
      amount: 0,
      date: "2026-03-01",
      source: "Salario",
      categoryId: null,
    })

    expect(result.success).toBe(false)
  })

  it("acepta la fecha actual", () => {
    const result = incomeSchema.safeParse({
      amount: 10,
      description: "Ingreso rapido",
      date: getTodayDateInput(),
      source: "Otros",
      categoryId: null,
    })

    expect(result.success).toBe(true)
  })
})

describe("expenseSchema", () => {
  it("acepta gastos validos", () => {
    const result = expenseSchema.safeParse({
      amount: 900,
      description: "Supermercado",
      date: "2026-03-01",
      paymentMethod: "CASH",
      categoryId: 3,
    })

    expect(result.success).toBe(true)
  })

  it("requiere un metodo de pago valido", () => {
    const result = expenseSchema.safeParse({
      amount: 900,
      description: "Supermercado",
      date: "2026-03-01",
      paymentMethod: "",
      categoryId: 3,
    })

    expect(result.success).toBe(false)
  })

  it("acepta la fecha actual", () => {
    const result = expenseSchema.safeParse({
      amount: 10,
      description: "Cafe",
      date: getTodayDateInput(),
      paymentMethod: "CASH",
      categoryId: null,
    })

    expect(result.success).toBe(true)
  })
})
