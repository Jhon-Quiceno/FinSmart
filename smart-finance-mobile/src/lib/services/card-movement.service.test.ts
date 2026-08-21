import MockAdapter from "axios-mock-adapter"
import { apiClient } from "../api-client"
import { getInstallments, getMovements, registerPayment, registerPurchase } from "./card-movement.service"
import type { CardPaymentRequest, CardPurchaseRequest } from "../types/card-movement"

describe("card movement service", () => {
  let mock: MockAdapter

  beforeEach(() => {
    mock = new MockAdapter(apiClient)
    mock.onGet("/api/users/csrf").reply(200, { token: "csrf-token" })
  })

  afterEach(() => {
    mock.restore()
  })

  describe("getMovements", () => {
    it("fetches movements for a card", async () => {
      const mockData = {
        content: [
          {
            id: 10,
            cardId: 1,
            type: "PURCHASE",
            amount: 150000,
            date: "2026-06-15",
            description: "Compra supermercado",
            cardBalanceAfter: null,
            expenseId: 5,
            installmentPlanId: null,
            createdAt: "2026-06-15T00:00:00Z",
          },
        ],
        number: 0,
        size: 20,
        totalElements: 1,
        totalPages: 1,
      }

      mock.onGet("/api/cards/1/movements").reply(200, mockData)

      const result = await getMovements(1)

      expect(result.content).toHaveLength(1)
      expect(result.content[0].cardId).toBe(1)
    })

    it("filters movements by type", async () => {
      const mockData = {
        content: [],
        number: 0,
        size: 20,
        totalElements: 0,
        totalPages: 0,
      }

      mock.onGet("/api/cards/1/movements", { params: { type: "PAYMENT" } }).reply(200, mockData)

      const result = await getMovements(1, { type: "PAYMENT" })

      expect(result.content).toHaveLength(0)
    })
  })

  describe("registerPurchase", () => {
    it("registers a simple purchase and returns the updated balance", async () => {
      const payload: CardPurchaseRequest = {
        amount: 700000,
        date: "2026-06-20",
        description: "Compra electronica",
      }

      const mockResponse = {
        id: 11,
        cardId: 1,
        type: "PURCHASE",
        amount: 700000,
        date: "2026-06-20",
        description: "Compra electronica",
        cardBalanceAfter: 1400000,
        expenseId: 6,
        installmentPlanId: null,
        createdAt: "2026-06-20T00:00:00Z",
      }

      mock.onPost("/api/cards/1/purchases").reply(201, mockResponse)

      const result = await registerPurchase(1, payload)

      expect(result.cardBalanceAfter).toBe(1400000)
      expect(result.installmentPlanId).toBeNull()
    })

    it("registers a deferred purchase with an installment plan", async () => {
      const payload: CardPurchaseRequest = {
        amount: 700000,
        date: "2026-06-20",
        installmentCount: 3,
      }

      const mockResponse = {
        id: 12,
        cardId: 1,
        type: "INSTALLMENT_PURCHASE",
        amount: 700000,
        date: "2026-06-20",
        description: null,
        cardBalanceAfter: 1400000,
        expenseId: 7,
        installmentPlanId: 4,
        createdAt: "2026-06-20T00:00:00Z",
      }

      mock.onPost("/api/cards/1/purchases").reply(201, mockResponse)

      const result = await registerPurchase(1, payload)

      expect(result.type).toBe("INSTALLMENT_PURCHASE")
      expect(result.installmentPlanId).toBe(4)
    })
  })

  describe("registerPayment", () => {
    it("registers a payment and returns the updated balance", async () => {
      const payload: CardPaymentRequest = {
        amount: 200000,
        date: "2026-06-25",
      }

      const mockResponse = {
        id: 13,
        cardId: 1,
        type: "PAYMENT",
        amount: 200000,
        date: "2026-06-25",
        description: null,
        cardBalanceAfter: 500000,
        expenseId: null,
        installmentPlanId: null,
        createdAt: "2026-06-25T00:00:00Z",
      }

      mock.onPost("/api/cards/1/payments").reply(201, mockResponse)

      const result = await registerPayment(1, payload)

      expect(result.cardBalanceAfter).toBe(500000)
      expect(result.expenseId).toBeNull()
    })
  })

  describe("getInstallments", () => {
    it("fetches the installment schedule of a deferred purchase", async () => {
      const mockInstallments = [
        {
          id: 1,
          number: 1,
          capitalAmount: 233333.33,
          interestAmount: 14700,
          dueDate: "2026-07-15",
          status: "PENDING",
        },
        {
          id: 2,
          number: 2,
          capitalAmount: 233333.33,
          interestAmount: 9800,
          dueDate: "2026-08-15",
          status: "PENDING",
        },
      ]

      mock.onGet("/api/cards/1/movements/12/installments").reply(200, mockInstallments)

      const result = await getInstallments(1, 12)

      expect(result).toHaveLength(2)
      expect(result[0].status).toBe("PENDING")
    })
  })
})
