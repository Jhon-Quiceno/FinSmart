import MockAdapter from "axios-mock-adapter"
import { apiClient } from "../api-client"
import { createDebtCharge, getDebtCharges } from "./debt-charge.service"
import type { DebtChargeRequest } from "../types/debt-charge"

describe("debt charge service", () => {
  let mock: MockAdapter

  beforeEach(() => {
    mock = new MockAdapter(apiClient)
    mock.onGet("/api/users/csrf").reply(200, { token: "csrf-token" })
  })

  afterEach(() => {
    mock.restore()
  })

  describe("getDebtCharges", () => {
    it("fetches charges for a debt", async () => {
      const mockData = {
        content: [
          {
            id: 10,
            debtId: 1,
            amount: 1500,
            chargeDate: "2026-06-15",
            description: "Compra supermercado",
            createdAt: "2026-06-15T00:00:00Z",
          },
        ],
        number: 0,
        size: 10,
        totalElements: 1,
        totalPages: 1,
      }

      mock.onGet("/api/debts/1/charges").reply(200, mockData)

      const result = await getDebtCharges(1)

      expect(result.content).toHaveLength(1)
      expect(result.content[0].debtId).toBe(1)
      expect(result.content[0].amount).toBe(1500)
    })
  })

  describe("createDebtCharge", () => {
    it("registers a charge and returns the updated debt", async () => {
      const payload: DebtChargeRequest = {
        amount: 2000,
        chargeDate: "2026-06-20",
        description: "Compra electronica",
      }

      const mockResponse = {
        id: 1,
        name: "Tarjeta Visa",
        totalAmount: 5000,
        remainingAmount: 4000,
        interestRate: null,
        dueDate: null,
        createdAt: "2026-01-01T00:00:00Z",
        updatedAt: "2026-06-20T00:00:00Z",
      }

      mock.onPost("/api/debts/1/charges").reply(201, mockResponse)

      const result = await createDebtCharge(1, payload)

      expect(result.id).toBe(1)
      expect(result.remainingAmount).toBe(4000)
    })
  })
})
