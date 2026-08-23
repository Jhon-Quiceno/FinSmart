import MockAdapter from "axios-mock-adapter"
import { apiClient } from "../api-client"
import { scanReceipt } from "./receipt.service"

describe("receipt service", () => {
  let mock: MockAdapter

  beforeEach(() => {
    mock = new MockAdapter(apiClient)
    mock.onGet("/api/users/csrf").reply(200, { token: "csrf-token" })
  })

  afterEach(() => {
    mock.restore()
  })

  describe("scanReceipt", () => {
    it("returns the extraction for a valid receipt", async () => {
      const mockResponse = {
        isReceipt: true,
        description: "Supermercado",
        amount: 45000,
        movementType: "EXPENSE",
        categoryId: 2,
        categoryName: "Alimentación",
      }

      mock.onPost("/api/receipts/scan").reply(200, mockResponse)

      const result = await scanReceipt("data:image/jpeg;base64,ZmFrZQ==")

      expect(result.isReceipt).toBe(true)
      expect(result.amount).toBe(45000)
      expect(result.categoryName).toBe("Alimentación")
    })

    it("returns isReceipt false for an unrelated image", async () => {
      const mockResponse = {
        isReceipt: false,
        description: null,
        amount: null,
        movementType: null,
        categoryId: null,
        categoryName: null,
      }

      mock.onPost("/api/receipts/scan").reply(200, mockResponse)

      const result = await scanReceipt("data:image/jpeg;base64,ZmFrZQ==")

      expect(result.isReceipt).toBe(false)
    })
  })
})
