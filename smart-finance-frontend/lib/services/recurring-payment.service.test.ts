import MockAdapter from "axios-mock-adapter"
import { afterEach, beforeEach, describe, expect, it } from "vitest"
import { apiClient } from "../api-client"
import {
  createRecurringPayment,
  deleteRecurringPayment,
  getRecurringPayments,
  payRecurringPayment,
  toggleRecurringPayment,
  updateRecurringPayment,
} from "./recurring-payment.service"
import type { RecurringPaymentCreateRequest, RecurringPaymentUpdateRequest } from "../types/recurring-payment"

describe("recurring payment service", () => {
  let mock: MockAdapter

  beforeEach(() => {
    mock = new MockAdapter(apiClient)
    mock.onGet("/api/users/csrf").reply(200, { token: "csrf-token" })
  })

  afterEach(() => {
    mock.restore()
  })

  describe("getRecurringPayments", () => {
    it("fetches recurring payments with default filters", async () => {
      const mockData = {
        content: [
          {
            id: 1,
            name: "Netflix",
            amount: 219,
            frequency: "MONTHLY",
            nextPaymentDate: "2026-07-05",
            isActive: true,
            createdAt: "2026-01-01T00:00:00Z",
            updatedAt: "2026-01-01T00:00:00Z",
          },
        ],
        number: 0,
        size: 10,
        totalElements: 1,
        totalPages: 1,
      }

      mock.onGet("/api/recurring").reply(200, mockData)

      const result = await getRecurringPayments()

      expect(result.content).toHaveLength(1)
      expect(result.content[0].name).toBe("Netflix")
      expect(result.content[0].frequency).toBe("MONTHLY")
    })
  })

  describe("createRecurringPayment", () => {
    it("creates a recurring payment", async () => {
      const payload: RecurringPaymentCreateRequest = {
        name: "Spotify",
        amount: 115,
        frequency: "MONTHLY",
        firstPaymentDate: "2026-07-10",
      }

      const mockResponse = {
        id: 2,
        name: "Spotify",
        amount: 115,
        frequency: "MONTHLY",
        nextPaymentDate: "2026-07-10",
        isActive: true,
        createdAt: "2026-06-01T00:00:00Z",
        updatedAt: "2026-06-01T00:00:00Z",
      }

      mock.onPost("/api/recurring").reply(201, mockResponse)

      const result = await createRecurringPayment(payload)

      expect(result.id).toBe(2)
      expect(result.nextPaymentDate).toBe("2026-07-10")
    })
  })

  describe("updateRecurringPayment", () => {
    it("updates a recurring payment without touching nextPaymentDate", async () => {
      const payload: RecurringPaymentUpdateRequest = {
        name: "Spotify Premium",
        amount: 145,
        frequency: "MONTHLY",
      }

      const mockResponse = {
        id: 2,
        name: "Spotify Premium",
        amount: 145,
        frequency: "MONTHLY",
        nextPaymentDate: "2026-07-10",
        isActive: true,
        createdAt: "2026-06-01T00:00:00Z",
        updatedAt: "2026-06-05T00:00:00Z",
      }

      mock.onPut("/api/recurring/2").reply(200, mockResponse)

      const result = await updateRecurringPayment(2, payload)

      expect(result.name).toBe("Spotify Premium")
      expect(result.nextPaymentDate).toBe("2026-07-10")
    })
  })

  describe("deleteRecurringPayment", () => {
    it("deletes a recurring payment by id", async () => {
      mock.onDelete("/api/recurring/2").reply(204)

      await expect(deleteRecurringPayment(2)).resolves.toBeUndefined()
    })
  })

  describe("toggleRecurringPayment", () => {
    it("toggles the active state", async () => {
      const mockResponse = {
        id: 2,
        name: "Spotify",
        amount: 115,
        frequency: "MONTHLY",
        nextPaymentDate: "2026-07-10",
        isActive: false,
        createdAt: "2026-06-01T00:00:00Z",
        updatedAt: "2026-06-10T00:00:00Z",
      }

      mock.onPatch("/api/recurring/2/toggle").reply(200, mockResponse)

      const result = await toggleRecurringPayment(2)

      expect(result.isActive).toBe(false)
    })
  })

  describe("payRecurringPayment", () => {
    it("marks the recurring payment as paid, recalculates the next date, and returns the generated expense id", async () => {
      const mockResponse = {
        recurringPayment: {
          id: 2,
          name: "Spotify",
          amount: 115,
          frequency: "MONTHLY",
          nextPaymentDate: "2026-08-10",
          isActive: true,
          createdAt: "2026-06-01T00:00:00Z",
          updatedAt: "2026-07-10T00:00:00Z",
        },
        expenseId: 42,
      }

      mock.onPatch("/api/recurring/2/pay").reply(200, mockResponse)

      const result = await payRecurringPayment(2)

      expect(result.recurringPayment.nextPaymentDate).toBe("2026-08-10")
      expect(result.expenseId).toBe(42)
    })
  })
})
