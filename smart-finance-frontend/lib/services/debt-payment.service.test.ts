import MockAdapter from "axios-mock-adapter"
import { afterEach, beforeEach, describe, expect, it } from "vitest"
import { apiClient } from "../api-client"
import { createDebtPayment, getDebtPayments } from "./debt-payment.service"
import type { DebtPaymentRequest } from "../types/debt-payment"

describe("debt payment service", () => {
  let mock: MockAdapter

  beforeEach(() => {
    mock = new MockAdapter(apiClient)
    mock.onGet("/api/users/csrf").reply(200, { token: "csrf-token" })
  })

  afterEach(() => {
    mock.restore()
  })

  describe("getDebtPayments", () => {
    it("fetches payments for a debt", async () => {
      const mockData = {
        content: [
          {
            id: 10,
            debtId: 1,
            amount: 1500,
            paymentDate: "2026-06-15",
            note: "Pago mensual",
            createdAt: "2026-06-15T00:00:00Z",
            expenseId: null,
          },
        ],
        number: 0,
        size: 10,
        totalElements: 1,
        totalPages: 1,
      }

      mock.onGet("/api/debts/1/payments").reply(200, mockData)

      const result = await getDebtPayments(1)

      expect(result.content).toHaveLength(1)
      expect(result.content[0].debtId).toBe(1)
      expect(result.content[0].amount).toBe(1500)
    })
  })

  describe("createDebtPayment", () => {
    it("registers a payment against a debt", async () => {
      const payload: DebtPaymentRequest = {
        amount: 2000,
        paymentDate: "2026-06-20",
        note: "Abono extra",
      }

      const mockResponse = {
        id: 11,
        debtId: 1,
        amount: 2000,
        paymentDate: "2026-06-20",
        note: "Abono extra",
        createdAt: "2026-06-20T00:00:00Z",
        expenseId: 42,
      }

      mock.onPost("/api/debts/1/payments").reply(201, mockResponse)

      const result = await createDebtPayment(1, payload)

      expect(result.id).toBe(11)
      expect(result.amount).toBe(2000)
      expect(result.expenseId).toBe(42)
    })
  })
})
