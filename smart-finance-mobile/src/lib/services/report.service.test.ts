import MockAdapter from "axios-mock-adapter"
import { apiClient } from "../api-client"
import { exportReport, exportReportBytes, getMonthlyReport, getReportMovements } from "./report.service"

describe("report service", () => {
  let mock: MockAdapter

  beforeEach(() => {
    mock = new MockAdapter(apiClient)
    mock.onGet("/api/users/csrf").reply(200, { token: "csrf-token" })
  })

  afterEach(() => {
    mock.restore()
  })

  describe("getMonthlyReport", () => {
    it("fetches the monthly report for a given year/month", async () => {
      const mockData = {
        periodYear: 2026,
        periodMonth: 7,
        totalIncome: 3000000,
        totalExpense: 2100000,
        savings: 900000,
        expenseRatio: 0.7,
        debtRatio: 0.3,
        topCategories: [
          { categoryId: 5, categoryName: "Comida", totalAmount: 900000, percentage: 0.316 },
        ],
        monthlySeries: [
          { year: 2026, month: 2, totalIncome: 2850000, totalExpense: 1900000 },
        ],
      }

      mock.onGet("/api/reports/monthly", { params: { year: 2026, month: 7 } }).reply(200, mockData)

      const result = await getMonthlyReport({ year: 2026, month: 7 })

      expect(result.periodYear).toBe(2026)
      expect(result.expenseRatio).toBe(0.7)
      expect(result.topCategories[0].categoryName).toBe("Comida")
    })

    it("fetches the report with no filters (backend defaults to the current period)", async () => {
      const mockData = {
        periodYear: 2026,
        periodMonth: 7,
        totalIncome: 0,
        totalExpense: 0,
        savings: 0,
        expenseRatio: 0,
        debtRatio: 0,
        topCategories: [],
        monthlySeries: [],
      }

      mock.onGet("/api/reports/monthly").reply(200, mockData)

      const result = await getMonthlyReport()

      expect(result.topCategories).toHaveLength(0)
    })

    it("propagates a 400 when year/month are out of range", async () => {
      mock
        .onGet("/api/reports/monthly", { params: { year: 1999, month: 6 } })
        .reply(400, { message: "El anio debe estar entre 2000 y 2100" })

      await expect(getMonthlyReport({ year: 1999, month: 6 })).rejects.toBeTruthy()
    })

    it("propagates a 500 server error", async () => {
      mock.onGet("/api/reports/monthly", { params: { year: 2026, month: 7 } }).reply(500)

      await expect(getMonthlyReport({ year: 2026, month: 7 })).rejects.toBeTruthy()
    })
  })

  describe("getReportMovements", () => {
    it("fetches the movements for a given year/month", async () => {
      const mockData = [
        {
          date: "2026-07-01",
          type: "INCOME",
          categoryName: "Salario",
          description: "Pago quincenal",
          amount: 1500000,
          paymentMethod: "TRANSFER",
        },
        {
          date: "2026-07-03",
          type: "EXPENSE",
          categoryName: "Comida",
          description: null,
          amount: 45000,
          paymentMethod: "CASH",
        },
      ]

      mock
        .onGet("/api/reports/movements", { params: { year: 2026, month: 7 } })
        .reply(200, mockData)

      const result = await getReportMovements({ year: 2026, month: 7 })

      expect(result).toHaveLength(2)
      expect(result[0].type).toBe("INCOME")
      expect(result[1].categoryName).toBe("Comida")
    })

    it("fetches movements with no filters (backend defaults to the current period)", async () => {
      mock.onGet("/api/reports/movements").reply(200, [])

      const result = await getReportMovements()

      expect(result).toHaveLength(0)
    })

    it("propagates a 400 when year/month are out of range", async () => {
      mock
        .onGet("/api/reports/movements", { params: { year: 1999, month: 6 } })
        .reply(400, { message: "El anio debe estar entre 2000 y 2100" })

      await expect(getReportMovements({ year: 1999, month: 6 })).rejects.toBeTruthy()
    })

    it("propagates a 500 server error", async () => {
      mock
        .onGet("/api/reports/movements", { params: { year: 2026, month: 7 } })
        .reply(500)

      await expect(getReportMovements({ year: 2026, month: 7 })).rejects.toBeTruthy()
    })
  })

  describe("exportReport", () => {
    it("downloads the CSV export and reads the filename from Content-Disposition", async () => {
      const blob = new Blob(["Fecha,Tipo\n"], { type: "text/csv" })

      mock.onGet("/api/reports/export", { params: { year: 2026, month: 7, format: "csv" } }).reply(
        200,
        blob,
        { "content-disposition": 'attachment; filename="korofin-2026-07.csv"' },
      )

      const result = await exportReport(2026, 7, "csv")

      expect(result.filename).toBe("korofin-2026-07.csv")
      expect(result.blob).toBeInstanceOf(Blob)
    })

    it("falls back to a constructed filename when Content-Disposition is missing", async () => {
      const blob = new Blob(["[]"], { type: "application/json" })

      mock.onGet("/api/reports/export", { params: { year: 2026, month: 3, format: "json" } }).reply(200, blob)

      const result = await exportReport(2026, 3, "json")

      expect(result.filename).toBe("korofin-2026-03.json")
    })

    it("defaults to csv format", async () => {
      const blob = new Blob(["Fecha,Tipo\n"], { type: "text/csv" })

      mock.onGet("/api/reports/export", { params: { year: 2026, month: 7, format: "csv" } }).reply(200, blob)

      const result = await exportReport(2026, 7)

      expect(result.filename).toBe("korofin-2026-07.csv")
    })

    it("propagates a 500 server error", async () => {
      mock.onGet("/api/reports/export", { params: { year: 2026, month: 7, format: "csv" } }).reply(500)

      await expect(exportReport(2026, 7, "csv")).rejects.toBeTruthy()
    })
  })

  describe("exportReportBytes", () => {
    it("downloads the export as raw bytes and reads the filename from Content-Disposition", async () => {
      const buffer = new TextEncoder().encode("Fecha,Tipo\n").buffer

      mock.onGet("/api/reports/export", { params: { year: 2026, month: 7, format: "csv" } }).reply(
        200,
        buffer,
        { "content-disposition": 'attachment; filename="korofin-2026-07.csv"' },
      )

      const result = await exportReportBytes(2026, 7, "csv")

      expect(result.filename).toBe("korofin-2026-07.csv")
      expect(result.data).toBeInstanceOf(ArrayBuffer)
    })

    it("falls back to a constructed filename when Content-Disposition is missing", async () => {
      const buffer = new TextEncoder().encode("Fecha,Tipo\n").buffer

      mock.onGet("/api/reports/export", { params: { year: 2026, month: 3, format: "csv" } }).reply(200, buffer)

      const result = await exportReportBytes(2026, 3, "csv")

      expect(result.filename).toBe("korofin-2026-03.csv")
    })
  })
})
