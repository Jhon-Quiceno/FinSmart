import MockAdapter from "axios-mock-adapter"
import { afterEach, beforeEach, describe, expect, it } from "vitest"
import { apiClient } from "../api-client"
import { createExpense, deleteExpense, getExpenses, updateExpense } from "./expense.service"
import type { ExpenseRequest } from "../types/expense"

describe("expense service", () => {
  let mock: MockAdapter

  beforeEach(() => {
    mock = new MockAdapter(apiClient)
    mock.onGet("/api/users/csrf").reply(200, { token: "csrf-token" })
  })

  afterEach(() => {
    mock.restore()
  })

  describe("getExpenses", () => {
    it("fetches expenses with default filters", async () => {
      const mockData = {
        content: [
          {
            id: 2,
            amount: 150.5,
            description: "Supermercado",
            date: "2026-05-15",
            paymentMethod: "DEBIT_CARD",
            categoryId: 2,
            categoryName: "Comida",
          },
        ],
        number: 0,
        size: 10,
        totalElements: 1,
        totalPages: 1,
      }

      mock.onGet("/api/expenses").reply(200, mockData)

      const result = await getExpenses()

      expect(result.content).toHaveLength(1)
      expect(result.content[0].amount).toBe(150.5)
      expect(result.content[0].paymentMethod).toBe("DEBIT_CARD")
    })

    it("fetches expenses with a date range filter", async () => {
      const mockData = {
        content: [],
        number: 0,
        size: 10,
        totalElements: 0,
        totalPages: 0,
      }

      mock
        .onGet("/api/expenses", { params: { from: "2026-05-01", to: "2026-05-31" } })
        .reply(200, mockData)

      await getExpenses({ from: "2026-05-01", to: "2026-05-31" })
    })
  })

  describe("createExpense", () => {
    it("creates an expense with a payment method", async () => {
      const payload: ExpenseRequest = {
        amount: 300.0,
        description: "Compra en linea",
        date: "2026-05-18",
        categoryId: 5,
        paymentMethod: "CREDIT_CARD",
      }

      const mockResponse = {
        id: 11,
        amount: 300.0,
        description: "Compra en linea",
        date: "2026-05-18",
        paymentMethod: "CREDIT_CARD",
        categoryId: 5,
        categoryName: "Compras",
      }

      mock.onPost("/api/expenses").reply(201, mockResponse)

      const result = await createExpense(payload)

      expect(result.id).toBe(11)
      expect(result.paymentMethod).toBe("CREDIT_CARD")
    })
  })

  describe("updateExpense", () => {
    it("updates an existing expense", async () => {
      const payload: ExpenseRequest = {
        amount: 450.0,
        description: "Gasto actualizado",
        date: "2026-05-10",
        categoryId: 3,
        paymentMethod: "CASH",
      }

      const mockResponse = {
        id: 5,
        amount: 450.0,
        description: "Gasto actualizado",
        date: "2026-05-10",
        paymentMethod: "CASH",
        categoryId: 3,
        categoryName: "Servicios",
      }

      mock.onPut("/api/expenses/5").reply(200, mockResponse)

      const result = await updateExpense(5, payload)

      expect(result.id).toBe(5)
      expect(result.amount).toBe(450.0)
    })
  })

  describe("deleteExpense", () => {
    it("deletes an expense by id", async () => {
      mock.onDelete("/api/expenses/7").reply(204)

      await expect(deleteExpense(7)).resolves.toBeUndefined()
    })
  })
})
