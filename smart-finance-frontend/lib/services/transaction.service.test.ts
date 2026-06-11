import MockAdapter from "axios-mock-adapter"
import { afterEach, beforeEach, describe, expect, it } from "vitest"
import { apiClient } from "../api-client"
import {
  createTransaction,
  deleteTransaction,
  getTransactions,
  updateTransaction,
} from "./transaction.service"
import type { TransactionRequest } from "../types/transaction"

describe("transaction service", () => {
  let mock: MockAdapter

  beforeEach(() => {
    mock = new MockAdapter(apiClient)
    mock.onGet("/api/users/csrf").reply(200, { token: "csrf-token" })
  })

  afterEach(() => {
    mock.restore()
  })

  describe("getTransactions", () => {
    it("fetches transactions with default filters", async () => {
      const mockData = {
        content: [
          {
            id: 1,
            accountId: null,
            accountName: null,
            categoryId: 1,
            categoryName: "Salario",
            type: "INCOME",
            amount: 1000.0,
            description: "Sueldo mensual",
            transactionDate: "2026-05-01",
            paymentMethod: null,
            notes: null,
            createdAt: "2026-05-01T00:00:00Z",
            updatedAt: "2026-05-01T00:00:00Z",
          },
        ],
        number: 0,
        size: 10,
        totalElements: 1,
        totalPages: 1,
      }

      mock.onGet("/api/transactions").reply(200, mockData)

      const result = await getTransactions()

      expect(result.content).toHaveLength(1)
      expect(result.content[0].id).toBe(1)
      expect(result.content[0].type).toBe("INCOME")
      expect(result.content[0].amount).toBe(1000.0)
    })

    it("fetches INCOME transactions", async () => {
      const mockData = {
        content: [
          {
            id: 1,
            accountId: null,
            accountName: null,
            categoryId: 1,
            categoryName: "Salario",
            type: "INCOME",
            amount: 1000.0,
            description: "Sueldo mensual",
            transactionDate: "2026-05-01",
            paymentMethod: null,
            notes: null,
            createdAt: "2026-05-01T00:00:00Z",
            updatedAt: "2026-05-01T00:00:00Z",
          },
        ],
        number: 0,
        size: 10,
        totalElements: 1,
        totalPages: 1,
      }

      mock.onGet("/api/transactions", { params: { type: "INCOME" } }).reply(200, mockData)

      const result = await getTransactions({ type: "INCOME" })

      expect(result.content).toHaveLength(1)
      expect(result.content[0].type).toBe("INCOME")
    })

    it("fetches EXPENSE transactions", async () => {
      const mockData = {
        content: [
          {
            id: 2,
            accountId: null,
            accountName: null,
            categoryId: 2,
            categoryName: "Comida",
            type: "EXPENSE",
            amount: 150.5,
            description: "Supermercado",
            transactionDate: "2026-05-15",
            paymentMethod: "DEBIT_CARD",
            notes: null,
            createdAt: "2026-05-15T00:00:00Z",
            updatedAt: "2026-05-15T00:00:00Z",
          },
        ],
        number: 0,
        size: 10,
        totalElements: 1,
        totalPages: 1,
      }

      mock.onGet("/api/transactions", { params: { type: "EXPENSE" } }).reply(200, mockData)

      const result = await getTransactions({ type: "EXPENSE" })

      expect(result.content).toHaveLength(1)
      expect(result.content[0].type).toBe("EXPENSE")
      expect(result.content[0].amount).toBe(150.5)
    })

    it("fetches transactions with date range filters", async () => {
      const mockData = {
        content: [],
        number: 0,
        size: 10,
        totalElements: 0,
        totalPages: 0,
      }

      mock
        .onGet("/api/transactions", {
          params: { from: "2026-05-01", to: "2026-05-31" },
        })
        .reply(200, mockData)

      await getTransactions({ from: "2026-05-01", to: "2026-05-31" })
    })

    it("handles pagination parameters", async () => {
      const mockData = {
        content: [],
        number: 1,
        size: 20,
        totalElements: 50,
        totalPages: 3,
      }

      mock
        .onGet("/api/transactions", {
          params: { page: 1, size: 20 },
        })
        .reply(200, mockData)

      const result = await getTransactions({ page: 1, size: 20 })

      expect(result.number).toBe(1)
      expect(result.size).toBe(20)
      expect(result.totalElements).toBe(50)
      expect(result.totalPages).toBe(3)
    })
  })

  describe("createTransaction", () => {
    it("creates an INCOME transaction", async () => {
      const payload: TransactionRequest = {
        type: "INCOME",
        amount: 2000.0,
        description: "Bono extra",
        transactionDate: "2026-05-20",
        categoryId: 1,
      }

      const mockResponse = {
        id: 10,
        accountId: null,
        accountName: null,
        categoryId: 1,
        categoryName: "Otros ingresos",
        type: "INCOME",
        amount: 2000.0,
        description: "Bono extra",
        transactionDate: "2026-05-20",
        paymentMethod: null,
        notes: null,
        createdAt: "2026-05-20T00:00:00Z",
        updatedAt: "2026-05-20T00:00:00Z",
      }

      mock.onPost("/api/transactions").reply(201, mockResponse)

      const result = await createTransaction(payload)

      expect(result.id).toBe(10)
      expect(result.type).toBe("INCOME")
      expect(result.amount).toBe(2000.0)
    })

    it("creates an EXPENSE transaction with payment method", async () => {
      const payload: TransactionRequest = {
        type: "EXPENSE",
        amount: 300.0,
        description: "Compra en línea",
        transactionDate: "2026-05-18",
        categoryId: 5,
        paymentMethod: "CREDIT_CARD",
      }

      const mockResponse = {
        id: 11,
        accountId: null,
        accountName: null,
        categoryId: 5,
        categoryName: "Compras",
        type: "EXPENSE",
        amount: 300.0,
        description: "Compra en línea",
        transactionDate: "2026-05-18",
        paymentMethod: "CREDIT_CARD",
        notes: null,
        createdAt: "2026-05-18T00:00:00Z",
        updatedAt: "2026-05-18T00:00:00Z",
      }

      mock.onPost("/api/transactions").reply(201, mockResponse)

      const result = await createTransaction(payload)

      expect(result.id).toBe(11)
      expect(result.type).toBe("EXPENSE")
      expect(result.paymentMethod).toBe("CREDIT_CARD")
    })
  })

  describe("updateTransaction", () => {
    it("updates an existing transaction", async () => {
      const payload: TransactionRequest = {
        type: "EXPENSE",
        amount: 450.0,
        description: "Gasto actualizado",
        transactionDate: "2026-05-10",
        categoryId: 3,
      }

      const mockResponse = {
        id: 5,
        accountId: null,
        accountName: null,
        categoryId: 3,
        categoryName: "Servicios",
        type: "EXPENSE",
        amount: 450.0,
        description: "Gasto actualizado",
        transactionDate: "2026-05-10",
        paymentMethod: "DEBIT_CARD",
        notes: "Nota actualizada",
        createdAt: "2026-05-10T00:00:00Z",
        updatedAt: "2026-05-20T00:00:00Z",
      }

      mock.onPut("/api/transactions/5").reply(200, mockResponse)

      const result = await updateTransaction(5, payload)

      expect(result.id).toBe(5)
      expect(result.amount).toBe(450.0)
      expect(result.description).toBe("Gasto actualizado")
    })
  })

  describe("deleteTransaction", () => {
    it("deletes a transaction by id", async () => {
      mock.onDelete("/api/transactions/7").reply(204)

      await expect(deleteTransaction(7)).resolves.toBeUndefined()
    })
  })
})
