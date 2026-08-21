import MockAdapter from "axios-mock-adapter"
import { apiClient } from "../api-client"
import { changePassword, updateProfile } from "./user.service"

describe("user service", () => {
  let mock: MockAdapter

  beforeEach(() => {
    mock = new MockAdapter(apiClient)
    mock.onGet("/api/users/csrf").reply(200, { token: "csrf-token" })
  })

  afterEach(() => {
    mock.restore()
  })

  describe("updateProfile", () => {
    it("updates the profile and returns the updated user", async () => {
      const mockUser = { id: 2, name: "Jhon Quiceno", email: "jhon@example.com" }

      mock
        .onPut("/api/users/profile", { name: "Jhon Quiceno", email: "jhon@example.com" })
        .reply(200, mockUser)

      const result = await updateProfile({ name: "Jhon Quiceno", email: "jhon@example.com" })

      expect(result).toEqual(mockUser)
    })

    it("propagates a 409 when the email is already taken", async () => {
      mock
        .onPut("/api/users/profile", { name: "Jhon Quiceno", email: "taken@example.com" })
        .reply(409, { message: "El correo ya esta en uso" })

      await expect(
        updateProfile({ name: "Jhon Quiceno", email: "taken@example.com" }),
      ).rejects.toBeTruthy()
    })
  })

  describe("changePassword", () => {
    it("changes the password with no response body", async () => {
      mock
        .onPut("/api/users/password", { currentPassword: "old-pass", newPassword: "new-pass" })
        .reply(204)

      await expect(
        changePassword({ currentPassword: "old-pass", newPassword: "new-pass" }),
      ).resolves.toBeUndefined()
    })

    it("propagates a 401 when the current password is wrong", async () => {
      mock
        .onPut("/api/users/password", { currentPassword: "wrong", newPassword: "new-pass" })
        .reply(401, { message: "Contrasena actual incorrecta" })

      await expect(
        changePassword({ currentPassword: "wrong", newPassword: "new-pass" }),
      ).rejects.toBeTruthy()
    })
  })
})
