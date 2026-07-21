import MockAdapter from "axios-mock-adapter"
import { afterEach, beforeEach, describe, expect, it } from "vitest"
import { apiClient } from "../api-client"
import { confirmImport, previewStatement } from "./statement-import.service"
import type { StatementConfirmRequest } from "../types/statement-import"

describe("statement import service", () => {
  let mock: MockAdapter

  beforeEach(() => {
    mock = new MockAdapter(apiClient)
    mock.onGet("/api/users/csrf").reply(200, { token: "csrf-token" })
  })

  afterEach(() => {
    mock.restore()
  })

  describe("previewStatement", () => {
    it("sends the file as multipart form data and parses the preview response", async () => {
      const mockResponse = {
        rows: [
          {
            date: "2026-05-10",
            description: "Supermercado",
            amount: 150.5,
            movementType: "EXPENSE",
            isDuplicate: false,
            suggestedCategoryId: 3,
            suggestedCategoryName: "Comida",
          },
        ],
        totalRows: 1,
        duplicateRows: 0,
      }

      mock.onPost("/api/statement-imports/preview").reply((config) => {
        expect(config.data).toBeInstanceOf(FormData)
        return [200, mockResponse]
      })

      const file = new File(["contenido"], "extracto.pdf", { type: "application/pdf" })
      const result = await previewStatement(file)

      expect(result.totalRows).toBe(1)
      expect(result.duplicateRows).toBe(0)
      expect(result.rows[0].description).toBe("Supermercado")
      expect(result.rows[0].movementType).toBe("EXPENSE")
    })

    it("includes the password field when provided", async () => {
      let capturedFormData: FormData | undefined

      mock.onPost("/api/statement-imports/preview").reply((config) => {
        capturedFormData = config.data as FormData
        return [200, { rows: [], totalRows: 0, duplicateRows: 0 }]
      })

      const file = new File(["contenido"], "extracto.pdf", { type: "application/pdf" })
      await previewStatement(file, "secreta123")

      expect(capturedFormData?.get("password")).toBe("secreta123")
      expect(capturedFormData?.get("file")).toBeInstanceOf(File)
    })

    it("omits the password field when not provided", async () => {
      let capturedFormData: FormData | undefined

      mock.onPost("/api/statement-imports/preview").reply((config) => {
        capturedFormData = config.data as FormData
        return [200, { rows: [], totalRows: 0, duplicateRows: 0 }]
      })

      const file = new File(["contenido"], "extracto.csv", { type: "text/csv" })
      await previewStatement(file)

      expect(capturedFormData?.has("password")).toBe(false)
    })

    it("propagates errors from the backend", async () => {
      mock.onPost("/api/statement-imports/preview").reply(422, {
        message: "La contrasena del PDF no es correcta",
      })

      const file = new File(["contenido"], "extracto.pdf", { type: "application/pdf" })

      await expect(previewStatement(file)).rejects.toBeTruthy()
    })
  })

  describe("confirmImport", () => {
    it("posts the confirmed rows as JSON and returns the created count", async () => {
      const request: StatementConfirmRequest = {
        rows: [
          {
            movementType: "EXPENSE",
            amount: 150.5,
            date: "2026-05-10",
            description: "Supermercado",
            categoryId: 3,
            paymentMethod: "OTHER",
          },
        ],
      }

      mock.onPost("/api/statement-imports/confirm", request).reply(200, { createdCount: 1 })

      const result = await confirmImport(request)

      expect(result.createdCount).toBe(1)
    })

    it("propagates errors from the backend", async () => {
      const request: StatementConfirmRequest = { rows: [] }

      mock.onPost("/api/statement-imports/confirm").reply(400, { message: "Debe confirmar al menos un movimiento" })

      await expect(confirmImport(request)).rejects.toBeTruthy()
    })
  })
})
