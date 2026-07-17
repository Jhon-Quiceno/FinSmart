import MockAdapter from "axios-mock-adapter"
import { afterEach, beforeEach, describe, expect, it } from "vitest"
import { apiClient } from "../api-client"
import { createCard, deleteCard, getCard, getCards, updateCard } from "./credit-card.service"
import type { CreditCardCreateRequest, CreditCardUpdateRequest } from "../types/credit-card"

describe("credit card service", () => {
  let mock: MockAdapter

  beforeEach(() => {
    mock = new MockAdapter(apiClient)
    mock.onGet("/api/users/csrf").reply(200, { token: "csrf-token" })
  })

  afterEach(() => {
    mock.restore()
  })

  const mockCard = {
    id: 1,
    name: "Visa Platinum",
    bank: "Bancolombia",
    franchise: "VISA",
    creditLimit: 5000000,
    monthlyRate: 0.025,
    cutoffDay: 15,
    paymentDueDay: 5,
    currentBalance: 700000,
    availableCredit: 4300000,
    lastCutoffDate: null,
    createdAt: "2026-01-01T00:00:00Z",
    updatedAt: "2026-01-01T00:00:00Z",
  }

  describe("getCards", () => {
    it("fetches a paginated list of cards", async () => {
      const mockData = {
        content: [mockCard],
        number: 0,
        size: 20,
        totalElements: 1,
        totalPages: 1,
      }

      mock.onGet("/api/cards").reply(200, mockData)

      const result = await getCards()

      expect(result.content).toHaveLength(1)
      expect(result.content[0].name).toBe("Visa Platinum")
    })
  })

  describe("getCard", () => {
    it("fetches a single card by id", async () => {
      mock.onGet("/api/cards/1").reply(200, mockCard)

      const result = await getCard(1)

      expect(result.id).toBe(1)
      expect(result.availableCredit).toBe(4300000)
    })
  })

  describe("createCard", () => {
    it("creates a card", async () => {
      const payload: CreditCardCreateRequest = {
        name: "Visa Platinum",
        bank: "Bancolombia",
        franchise: "VISA",
        creditLimit: 5000000,
        monthlyRate: 0.025,
        cutoffDay: 15,
        paymentDueDay: 5,
      }

      mock.onPost("/api/cards").reply(201, { ...mockCard, currentBalance: 0, availableCredit: 5000000 })

      const result = await createCard(payload)

      expect(result.id).toBe(1)
      expect(result.currentBalance).toBe(0)
    })
  })

  describe("updateCard", () => {
    it("updates a card and returns the updated resource", async () => {
      const payload: CreditCardUpdateRequest = {
        name: "Visa Platinum Renovada",
        bank: "Bancolombia",
        monthlyRate: 0.02,
        cutoffDay: 20,
        paymentDueDay: 10,
      }

      mock.onPut("/api/cards/1").reply(200, { ...mockCard, name: "Visa Platinum Renovada" })

      const result = await updateCard(1, payload)

      expect(result.name).toBe("Visa Platinum Renovada")
    })
  })

  describe("deleteCard", () => {
    it("deletes a card", async () => {
      mock.onDelete("/api/cards/1").reply(204)

      await expect(deleteCard(1)).resolves.toBeUndefined()
    })
  })
})
