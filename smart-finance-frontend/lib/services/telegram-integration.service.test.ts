import MockAdapter from "axios-mock-adapter"
import { afterEach, beforeEach, describe, expect, it } from "vitest"
import { apiClient } from "../api-client"
import { generateTelegramLinkCode } from "./telegram-integration.service"

describe("telegram integration service", () => {
  let mock: MockAdapter

  beforeEach(() => {
    mock = new MockAdapter(apiClient)
    mock.onGet("/api/users/csrf").reply(200, { token: "csrf-token" })
  })

  afterEach(() => {
    mock.restore()
  })

  describe("generateTelegramLinkCode", () => {
    it("requests a new link code", async () => {
      mock.onPost("/api/integrations/telegram/link-code").reply(200, {
        code: "ABC12345",
        expiresInSeconds: 600,
      })

      const result = await generateTelegramLinkCode()

      expect(result.code).toBe("ABC12345")
      expect(result.expiresInSeconds).toBe(600)
    })

    it("propagates errors from the backend", async () => {
      mock.onPost("/api/integrations/telegram/link-code").reply(500)

      await expect(generateTelegramLinkCode()).rejects.toBeTruthy()
    })
  })
})
