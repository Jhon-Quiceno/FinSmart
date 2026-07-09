import { describe, expect, it } from "vitest"
import { categorySchema } from "./category.schema"
import { incomeSchema } from "./income.schema"
import { expenseSchema } from "./expense.schema"
import { debtSchema, debtEditSchema } from "./debt.schema"
import { debtPaymentSchema } from "./debt-payment.schema"
import { recurringPaymentSchema, recurringPaymentEditSchema } from "./recurring-payment.schema"
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
      categoryId: 2,
    })

    expect(result.success).toBe(true)
  })

  it("rechaza montos en cero", () => {
    const result = incomeSchema.safeParse({
      amount: 0,
      date: "2026-03-01",
      categoryId: null,
    })

    expect(result.success).toBe(false)
  })

  it("acepta la fecha actual", () => {
    const result = incomeSchema.safeParse({
      amount: 10,
      description: "Ingreso rapido",
      date: getTodayDateInput(),
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

describe("debtSchema", () => {
  it("acepta una deuda valida", () => {
    const result = debtSchema.safeParse({
      name: "Tarjeta BBVA",
      totalAmount: 25000,
      interestRate: 28.5,
      dueDate: "2026-08-25",
    })

    expect(result.success).toBe(true)
  })

  it("acepta una deuda sin interes ni fecha", () => {
    const result = debtSchema.safeParse({
      name: "Prestamo personal",
      totalAmount: 5000,
    })

    expect(result.success).toBe(true)
  })

  it("requiere un nombre", () => {
    const result = debtSchema.safeParse({
      name: "",
      totalAmount: 5000,
    })

    expect(result.success).toBe(false)
  })

  it("requiere que el monto total sea mayor a 0", () => {
    const result = debtSchema.safeParse({
      name: "Tarjeta",
      totalAmount: 0,
    })

    expect(result.success).toBe(false)
  })

  it("rechaza una tasa de interes negativa", () => {
    const result = debtSchema.safeParse({
      name: "Tarjeta",
      totalAmount: 5000,
      interestRate: -1,
    })

    expect(result.success).toBe(false)
  })

  it("rechaza una fecha de vencimiento invalida", () => {
    const result = debtSchema.safeParse({
      name: "Tarjeta",
      totalAmount: 5000,
      dueDate: "31-13-2026",
    })

    expect(result.success).toBe(false)
  })
})

describe("debtEditSchema", () => {
  it("no incluye el monto total", () => {
    const result = debtEditSchema.safeParse({
      name: "Tarjeta BBVA",
      interestRate: 20,
      dueDate: "2026-09-01",
    })

    expect(result.success).toBe(true)
    expect(result.success && "totalAmount" in result.data).toBe(false)
  })

  it("requiere un nombre", () => {
    const result = debtEditSchema.safeParse({
      name: "",
    })

    expect(result.success).toBe(false)
  })
})

describe("debtPaymentSchema", () => {
  it("acepta un abono valido", () => {
    const result = debtPaymentSchema.safeParse({
      amount: 1500,
      paymentDate: "2026-06-15",
      note: "Pago mensual",
    })

    expect(result.success).toBe(true)
  })

  it("requiere que el monto sea mayor a 0", () => {
    const result = debtPaymentSchema.safeParse({
      amount: 0,
      paymentDate: "2026-06-15",
    })

    expect(result.success).toBe(false)
  })

  it("requiere una fecha de pago", () => {
    const result = debtPaymentSchema.safeParse({
      amount: 100,
      paymentDate: "",
    })

    expect(result.success).toBe(false)
  })
})

describe("recurringPaymentSchema", () => {
  it("acepta un servicio recurrente valido", () => {
    const result = recurringPaymentSchema.safeParse({
      name: "Netflix",
      amount: 219,
      frequency: "MONTHLY",
      firstPaymentDate: "2026-07-05",
    })

    expect(result.success).toBe(true)
  })

  it("requiere una frecuencia valida", () => {
    const result = recurringPaymentSchema.safeParse({
      name: "Netflix",
      amount: 219,
      frequency: "YEARLY",
      firstPaymentDate: "2026-07-05",
    })

    expect(result.success).toBe(false)
  })

  it("requiere que el monto sea mayor a 0", () => {
    const result = recurringPaymentSchema.safeParse({
      name: "Netflix",
      amount: 0,
      frequency: "MONTHLY",
      firstPaymentDate: "2026-07-05",
    })

    expect(result.success).toBe(false)
  })
})

describe("recurringPaymentEditSchema", () => {
  it("no incluye la fecha del primer pago", () => {
    const result = recurringPaymentEditSchema.safeParse({
      name: "Netflix",
      amount: 250,
      frequency: "WEEKLY",
    })

    expect(result.success).toBe(true)
    expect(result.success && "firstPaymentDate" in result.data).toBe(false)
  })
})

