import { AxiosError } from "axios"
import MockAdapter from "axios-mock-adapter"
import { afterEach, beforeEach, describe, expect, it } from "vitest"
import { apiClient, getApiErrorMessage, loginRequest, registerRequest } from "./api-client"

describe("api-client auth requests", () => {
  let mock: MockAdapter

  beforeEach(() => {
    mock = new MockAdapter(apiClient)
  })

  afterEach(() => {
    mock.restore()
  })

  it("registerRequest devuelve el usuario registrado", async () => {
    mock.onPost("/api/users/register").reply(201, {
      token: "stub-token",
      user: { id: 1, name: "Ana", email: "ana@mail.com" },
    })

    const response = await registerRequest("Ana", "ana@mail.com", "secret123")
    expect(response.token).toBe("stub-token")
    expect(response.user.email).toBe("ana@mail.com")
  })

  it("loginRequest devuelve sesión válida", async () => {
    mock.onPost("/api/users/login").reply(200, {
      token: "stub-token",
      user: { id: 1, name: "Ana", email: "ana@mail.com" },
    })

    const response = await loginRequest("ana@mail.com", "secret123")
    expect(response.user.id).toBe(1)
    expect(response.user.name).toBe("Ana")
  })

  it("getApiErrorMessage prioriza el mensaje del backend", async () => {
    mock.onPost("/api/users/login").reply(401, {
      message: "Correo o contraseña inválidos",
    })

    try {
      await loginRequest("ana@mail.com", "bad-pass")
      throw new Error("Se esperaba error de login")
    } catch (error) {
      expect(getApiErrorMessage(error, "Fallback")).toBe("Correo o contraseña inválidos")
    }
  })

  it("getApiErrorMessage devuelve mensaje de conexión cuando no hay response", () => {
    const error = new AxiosError("Network Error", "ERR_NETWORK")
    expect(getApiErrorMessage(error, "Fallback")).toContain("No hay conexión con el backend")
  })

  it("getApiErrorMessage usa fallback cuando no es AxiosError", () => {
    expect(getApiErrorMessage(new Error("x"), "Fallback")).toBe("Fallback")
  })
})
