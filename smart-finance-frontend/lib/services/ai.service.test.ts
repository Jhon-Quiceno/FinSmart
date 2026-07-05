import MockAdapter from "axios-mock-adapter"
import { afterEach, beforeEach, describe, expect, it } from "vitest"
import { apiClient } from "../api-client"
import { categorize, chat, generateInsight, getChatHistory, getLatestInsight } from "./ai.service"

describe("ai service", () => {
  let mock: MockAdapter

  beforeEach(() => {
    mock = new MockAdapter(apiClient)
    mock.onGet("/api/users/csrf").reply(200, { token: "csrf-token" })
  })

  afterEach(() => {
    mock.restore()
  })

  describe("chat", () => {
    it("sends a message and returns the assistant reply", async () => {
      const mockResponse = {
        reply: "Tu balance del mes esta en verde.",
        providerName: "groq",
        model: "llama-3.3-70b-versatile",
        createdAt: "2026-07-03T10:00:00Z",
      }

      mock.onPost("/api/ai/chat", { message: "Como voy este mes?" }).reply(200, mockResponse)

      const result = await chat("Como voy este mes?")

      expect(result.reply).toBe("Tu balance del mes esta en verde.")
      expect(result.providerName).toBe("groq")
    })

    it("propagates a 503 error when the assistant is unavailable", async () => {
      mock.onPost("/api/ai/chat").reply(503, {
        timestamp: "2026-07-03T10:00:00Z",
        status: 503,
        error: "Service Unavailable",
        message: "El asistente no esta disponible en este momento. Intentalo de nuevo mas tarde.",
        path: "/api/ai/chat",
      })

      await expect(chat("Hola")).rejects.toMatchObject({
        response: { status: 503 },
      })
    })
  })

  describe("getChatHistory", () => {
    it("fetches the paginated chat history", async () => {
      const mockData = {
        content: [
          {
            id: 1,
            role: "USER",
            content: "Hola",
            providerName: null,
            model: null,
            createdAt: "2026-07-03T09:00:00Z",
          },
          {
            id: 2,
            role: "ASSISTANT",
            content: "Hola! En que te puedo ayudar?",
            providerName: "groq",
            model: "llama-3.3-70b-versatile",
            createdAt: "2026-07-03T09:00:01Z",
          },
        ],
        number: 0,
        size: 20,
        totalElements: 2,
        totalPages: 1,
      }

      mock.onGet("/api/ai/chat/history").reply(200, mockData)

      const result = await getChatHistory()

      expect(result.content).toHaveLength(2)
      expect(result.content[1].role).toBe("ASSISTANT")
    })
  })

  describe("getLatestInsight", () => {
    it("returns the latest insight when one exists", async () => {
      const mockData = {
        id: 1,
        content: "- Reduce tus gastos en comida\n- Aumenta tu ahorro mensual",
        providerName: "gemini",
        model: "gemini-2.5-flash",
        createdAt: "2026-07-02T08:00:00Z",
      }

      mock.onGet("/api/ai/insights").reply(200, mockData)

      const result = await getLatestInsight()

      expect(result?.id).toBe(1)
    })

    it("returns null when the backend responds 204 No Content", async () => {
      mock.onGet("/api/ai/insights").reply(204)

      const result = await getLatestInsight()

      expect(result).toBeNull()
    })
  })

  describe("generateInsight", () => {
    it("generates a new insight", async () => {
      const mockData = {
        id: 2,
        content: "- Tu gasto en entretenimiento crecio 20%",
        providerName: "groq",
        model: "llama-3.3-70b-versatile",
        createdAt: "2026-07-03T10:00:00Z",
      }

      mock.onPost("/api/ai/insights/generate").reply(200, mockData)

      const result = await generateInsight()

      expect(result.id).toBe(2)
    })

    it("propagates a 503 error when the assistant is unavailable", async () => {
      mock.onPost("/api/ai/insights/generate").reply(503, {
        timestamp: "2026-07-03T10:00:00Z",
        status: 503,
        error: "Service Unavailable",
        message: "El asistente no esta disponible en este momento. Intentalo de nuevo mas tarde.",
        path: "/api/ai/insights/generate",
      })

      await expect(generateInsight()).rejects.toMatchObject({
        response: { status: 503 },
      })
    })
  })

  describe("categorize", () => {
    it("suggests a category for an expense description", async () => {
      const mockData = { categoryId: 3, categoryName: "Comida" }

      mock.onPost("/api/ai/categorize").reply(200, mockData)

      const result = await categorize({ description: "Almuerzo en restaurante", amount: 25000 })

      expect(result.categoryName).toBe("Comida")
    })

    it("returns null fields when no category matches", async () => {
      mock.onPost("/api/ai/categorize").reply(200, { categoryId: null, categoryName: null })

      const result = await categorize({ description: "Algo raro" })

      expect(result.categoryId).toBeNull()
    })
  })
})
