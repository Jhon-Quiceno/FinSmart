import { AxiosError } from "axios"
import MockAdapter from "axios-mock-adapter"
import { afterEach, beforeEach, describe, expect, it } from "vitest"
import { apiClient, getApiErrorMessage } from "./api-client"
import { loginRequest, logoutRequest, refreshRequest, registerRequest } from "./services/auth.service"

describe("auth service requests", () => {
  let mock: MockAdapter

  beforeEach(() => {
    mock = new MockAdapter(apiClient)
  })

  afterEach(() => {
    mock.restore()
  })

  it("registerRequest devuelve el usuario registrado", async () => {
    mock.onPost("/api/users/register").reply(201, {
      accessToken: "access-token",
      tokenType: "Bearer",
      expiresIn: 900,
      user: { id: 1, name: "Ana", email: "ana@mail.com" },
    })

    const response = await registerRequest("Ana", "ana@mail.com", "secret123")
    expect(response.accessToken).toBe("access-token")
    expect(response.user.email).toBe("ana@mail.com")
  })

  it("loginRequest devuelve sesion valida", async () => {
    mock.onPost("/api/users/login").reply(200, {
      accessToken: "access-token",
      tokenType: "Bearer",
      expiresIn: 900,
      user: { id: 1, name: "Ana", email: "ana@mail.com" },
    })

    const response = await loginRequest("ana@mail.com", "secret123")
    expect(response.user.id).toBe(1)
    expect(response.tokenType).toBe("Bearer")
  })

  it("refreshRequest renueva el access token", async () => {
    mock.onPost("/api/users/refresh").reply(200, {
      accessToken: "new-access-token",
      tokenType: "Bearer",
      expiresIn: 900,
      user: { id: 1, name: "Ana", email: "ana@mail.com" },
    })

    const response = await refreshRequest()
    expect(response.accessToken).toBe("new-access-token")
  })

  it("logoutRequest completa sin error", async () => {
    mock.onPost("/api/users/logout").reply(204)
    await expect(logoutRequest()).resolves.toBeUndefined()
  })

  it("getApiErrorMessage prioriza el mensaje del backend", async () => {
    mock.onPost("/api/users/login").reply(401, {
      message: "Correo o contrasena invalidos",
    })

    try {
      await loginRequest("ana@mail.com", "bad-pass")
      throw new Error("Se esperaba error de login")
    } catch (error) {
      expect(getApiErrorMessage(error, "Fallback")).toBe("Correo o contrasena invalidos")
    }
  })

  it("getApiErrorMessage devuelve mensaje de conexion cuando no hay response", () => {
    const error = new AxiosError("Network Error", "ERR_NETWORK")
    expect(getApiErrorMessage(error, "Fallback")).toContain("No hay conexion con el backend")
  })

  it("getApiErrorMessage usa fallback cuando no es AxiosError", () => {
    expect(getApiErrorMessage(new Error("x"), "Fallback")).toBe("Fallback")
  })
})
