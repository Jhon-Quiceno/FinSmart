import MockAdapter from "axios-mock-adapter"
import { apiClient } from "../api-client"
import { createIncome, deleteIncome, getIncomes, updateIncome } from "./income.service"
import type { IncomeRequest } from "../types/income"

describe("income service", () => {
  let mock: MockAdapter

  beforeEach(() => {
    mock = new MockAdapter(apiClient)
    mock.onGet("/api/users/csrf").reply(200, { token: "csrf-token" })
  })

  afterEach(() => {
    mock.restore()
  })

  describe("getIncomes", () => {
    it("fetches incomes with default filters", async () => {
      const mockData = {
        content: [
          {
            id: 1,
            amount: 1000.0,
            description: "Sueldo mensual",
            date: "2026-05-01",
            categoryId: 1,
            categoryName: "Salario",
          },
        ],
        number: 0,
        size: 10,
        totalElements: 1,
        totalPages: 1,
      }

      mock.onGet("/api/incomes").reply(200, mockData)

      const result = await getIncomes()

      expect(result.content).toHaveLength(1)
      expect(result.content[0].id).toBe(1)
      expect(result.content[0].amount).toBe(1000.0)
    })

    it("fetches incomes filtered by month and year", async () => {
      const mockData = {
        content: [],
        number: 0,
        size: 10,
        totalElements: 0,
        totalPages: 0,
      }

      mock.onGet("/api/incomes", { params: { month: 5, year: 2026 } }).reply(200, mockData)

      await getIncomes({ month: 5, year: 2026 })
    })
  })

  describe("createIncome", () => {
    it("creates an income", async () => {
      const payload: IncomeRequest = {
        amount: 2000.0,
        description: "Bono extra",
        date: "2026-05-20",
        categoryId: 1,
      }

      const mockResponse = {
        id: 10,
        amount: 2000.0,
        description: "Bono extra",
        date: "2026-05-20",
        categoryId: 1,
        categoryName: "Otros ingresos",
      }

      mock.onPost("/api/incomes").reply(201, mockResponse)

      const result = await createIncome(payload)

      expect(result.id).toBe(10)
      expect(result.amount).toBe(2000.0)
    })
  })

  describe("updateIncome", () => {
    it("updates an existing income", async () => {
      const payload: IncomeRequest = {
        amount: 450.0,
        description: "Ingreso actualizado",
        date: "2026-05-10",
        categoryId: 3,
      }

      const mockResponse = {
        id: 5,
        amount: 450.0,
        description: "Ingreso actualizado",
        date: "2026-05-10",
        categoryId: 3,
        categoryName: "Freelance",
      }

      mock.onPut("/api/incomes/5").reply(200, mockResponse)

      const result = await updateIncome(5, payload)

      expect(result.id).toBe(5)
      expect(result.amount).toBe(450.0)
    })
  })

  describe("deleteIncome", () => {
    it("deletes an income by id", async () => {
      mock.onDelete("/api/incomes/7").reply(204)

      await expect(deleteIncome(7)).resolves.toBeUndefined()
    })
  })
})
