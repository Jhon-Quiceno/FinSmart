import MockAdapter from "axios-mock-adapter"
import { apiClient } from "../api-client"
import { getAnalysisRecommendations, getAnalysisSummary, getPrediction } from "./analysis.service"

describe("analysis service", () => {
  let mock: MockAdapter

  beforeEach(() => {
    mock = new MockAdapter(apiClient)
    mock.onGet("/api/users/csrf").reply(200, { token: "csrf-token" })
  })

  afterEach(() => {
    mock.restore()
  })

  describe("getAnalysisSummary", () => {
    it("fetches the summary for the current period when no filters are given", async () => {
      const mockData = {
        totalIncome: 30000,
        totalExpense: 21000,
        savings: 9000,
        expenseRatio: 0.7,
        expenseAlert: false,
        totalDebt: 12000,
        debtRatio: 0.4,
        annualSavingsProjection: 108000,
        topCategories: [
          { categoryId: 1, categoryName: "Comida", totalAmount: 9500, percentage: 45.2 },
        ],
        monthlySeries: [
          { year: 2026, month: 2, totalIncome: 28000, totalExpense: 19000 },
          { year: 2026, month: 3, totalIncome: 30000, totalExpense: 21000 },
        ],
        recentTransactions: [
          {
            id: 10,
            type: "EXPENSE",
            amount: 500,
            description: "Supermercado",
            date: "2026-07-01",
            categoryName: "Comida",
          },
        ],
      }

      mock.onGet("/api/analysis/summary", { params: { year: undefined, month: undefined } }).reply(200, mockData)

      const result = await getAnalysisSummary()

      expect(result.totalIncome).toBe(30000)
      expect(result.expenseAlert).toBe(false)
      expect(result.topCategories).toHaveLength(1)
      expect(result.recentTransactions[0].type).toBe("EXPENSE")
    })

    it("fetches the summary for a specific year and month", async () => {
      const mockData = {
        totalIncome: 0,
        totalExpense: 0,
        savings: 0,
        expenseRatio: 0,
        expenseAlert: false,
        totalDebt: 0,
        debtRatio: 0,
        annualSavingsProjection: 0,
        topCategories: [],
        monthlySeries: [],
        recentTransactions: [],
      }

      mock.onGet("/api/analysis/summary", { params: { year: 2026, month: 5 } }).reply(200, mockData)

      const result = await getAnalysisSummary(2026, 5)

      expect(result.totalIncome).toBe(0)
      expect(result.topCategories).toHaveLength(0)
    })
  })

  describe("getAnalysisRecommendations", () => {
    it("fetches recommendations for the current period", async () => {
      const mockData = [
        {
          type: "ALERT",
          severity: "HIGH",
          categoryId: 1,
          message: "Tus gastos superan el 80% de tus ingresos.",
        },
        {
          type: "RECOMMENDATION",
          severity: "MEDIUM",
          categoryId: null,
          message: "Considera reducir tus gastos en comida.",
        },
      ]

      mock
        .onGet("/api/analysis/recommendations", { params: { year: undefined, month: undefined } })
        .reply(200, mockData)

      const result = await getAnalysisRecommendations()

      expect(result).toHaveLength(2)
      expect(result[0].type).toBe("ALERT")
    })

    it("fetches recommendations for a specific year and month", async () => {
      mock.onGet("/api/analysis/recommendations", { params: { year: 2026, month: 3 } }).reply(200, [])

      const result = await getAnalysisRecommendations(2026, 3)

      expect(result).toEqual([])
    })
  })

  describe("getPrediction", () => {
    it("fetches the month-end prediction", async () => {
      const mockData = {
        projectedExpense: 25000,
        projectedBalance: 5000,
        avgDailySpend: 800,
        daysRemaining: 10,
        recommendedDailyMax: 500,
        alert: false,
      }

      mock.onGet("/api/analysis/prediction").reply(200, mockData)

      const result = await getPrediction()

      expect(result.projectedBalance).toBe(5000)
      expect(result.alert).toBe(false)
    })

    it("surfaces the alert flag when the projected balance is negative", async () => {
      const mockData = {
        projectedExpense: 40000,
        projectedBalance: -3000,
        avgDailySpend: 1200,
        daysRemaining: 5,
        recommendedDailyMax: 0,
        alert: true,
      }

      mock.onGet("/api/analysis/prediction").reply(200, mockData)

      const result = await getPrediction()

      expect(result.alert).toBe(true)
      expect(result.recommendedDailyMax).toBe(0)
    })

    it("propagates errors when the backend fails", async () => {
      mock.onGet("/api/analysis/prediction").reply(500)

      await expect(getPrediction()).rejects.toBeTruthy()
    })
  })
})
