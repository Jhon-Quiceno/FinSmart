import MockAdapter from "axios-mock-adapter"
import { afterEach, beforeEach, describe, expect, it } from "vitest"
import { apiClient } from "../api-client"
import { createDebt, deleteDebt, getDebts, updateDebt } from "./debt.service"
import type { DebtCreateRequest, DebtUpdateRequest } from "../types/debt"

describe("debt service", () => {
  let mock: MockAdapter

  beforeEach(() => {
    mock = new MockAdapter(apiClient)
    mock.onGet("/api/users/csrf").reply(200, { token: "csrf-token" })
  })

  afterEach(() => {
    mock.restore()
  })

  describe("getDebts", () => {
    it("fetches debts with default filters", async () => {
      const mockData = {
        content: [
          {
            id: 1,
            name: "Tarjeta BBVA",
            totalAmount: 25000,
            remainingAmount: 18000,
            interestRate: 28.5,
            dueDate: "2026-08-25",
            createdAt: "2026-01-01T00:00:00Z",
            updatedAt: "2026-01-01T00:00:00Z",
          },
        ],
        number: 0,
        size: 10,
        totalElements: 1,
        totalPages: 1,
      }

      mock.onGet("/api/debts").reply(200, mockData)

      const result = await getDebts()

      expect(result.content).toHaveLength(1)
      expect(result.content[0].name).toBe("Tarjeta BBVA")
      expect(result.content[0].remainingAmount).toBe(18000)
    })

    it("fetches debts with pagination filters", async () => {
      const mockData = {
        content: [],
        number: 1,
        size: 5,
        totalElements: 0,
        totalPages: 0,
      }

      mock.onGet("/api/debts", { params: { page: 1, size: 5 } }).reply(200, mockData)

      await getDebts({ page: 1, size: 5 })
    })
  })

  describe("createDebt", () => {
    it("creates a debt", async () => {
      const payload: DebtCreateRequest = {
        name: "Prestamo Auto",
        totalAmount: 150000,
        interestRate: 12.5,
        dueDate: "2026-09-15",
      }

      const mockResponse = {
        id: 2,
        name: "Prestamo Auto",
        totalAmount: 150000,
        remainingAmount: 150000,
        interestRate: 12.5,
        dueDate: "2026-09-15",
        createdAt: "2026-06-01T00:00:00Z",
        updatedAt: "2026-06-01T00:00:00Z",
      }

      mock.onPost("/api/debts").reply(201, mockResponse)

      const result = await createDebt(payload)

      expect(result.id).toBe(2)
      expect(result.remainingAmount).toBe(150000)
    })
  })

  describe("updateDebt", () => {
    it("updates an existing debt without changing the total amount", async () => {
      const payload: DebtUpdateRequest = {
        name: "Tarjeta BBVA actualizada",
        interestRate: 25,
        dueDate: "2026-10-01",
      }

      const mockResponse = {
        id: 1,
        name: "Tarjeta BBVA actualizada",
        totalAmount: 25000,
        remainingAmount: 18000,
        interestRate: 25,
        dueDate: "2026-10-01",
        createdAt: "2026-01-01T00:00:00Z",
        updatedAt: "2026-06-15T00:00:00Z",
      }

      mock.onPut("/api/debts/1").reply(200, mockResponse)

      const result = await updateDebt(1, payload)

      expect(result.id).toBe(1)
      expect(result.name).toBe("Tarjeta BBVA actualizada")
      expect(result.totalAmount).toBe(25000)
    })
  })

  describe("deleteDebt", () => {
    it("deletes a debt by id", async () => {
      mock.onDelete("/api/debts/3").reply(204)

      await expect(deleteDebt(3)).resolves.toBeUndefined()
    })
  })
})
