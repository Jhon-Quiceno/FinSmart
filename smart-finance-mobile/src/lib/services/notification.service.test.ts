import MockAdapter from "axios-mock-adapter"
import { apiClient } from "../api-client"
import {
  getNotifications,
  getPreferences,
  getUnreadCount,
  markAllAsRead,
  markAsRead,
  updatePreferences,
} from "./notification.service"
import type { NotificationPreferenceRequest } from "../types/notification"

describe("notification service", () => {
  let mock: MockAdapter

  beforeEach(() => {
    mock = new MockAdapter(apiClient)
    mock.onGet("/api/users/csrf").reply(200, { token: "csrf-token" })
  })

  afterEach(() => {
    mock.restore()
  })

  describe("getNotifications", () => {
    it("fetches notifications with default filters", async () => {
      const mockData = {
        content: [
          {
            id: 1,
            type: "PAYMENT_REMINDER",
            title: "Pago proximo",
            message: "Tu servicio de Netflix vence en 3 dias",
            read: false,
            readAt: null,
            createdAt: "2026-07-01T10:00:00Z",
          },
        ],
        number: 0,
        size: 20,
        totalElements: 1,
        totalPages: 1,
      }

      mock.onGet("/api/notifications").reply(200, mockData)

      const result = await getNotifications()

      expect(result.content).toHaveLength(1)
      expect(result.content[0].type).toBe("PAYMENT_REMINDER")
      expect(result.content[0].read).toBe(false)
    })
  })

  describe("getUnreadCount", () => {
    it("fetches the unread count", async () => {
      mock.onGet("/api/notifications/unread-count").reply(200, 5)

      const result = await getUnreadCount()

      expect(result).toBe(5)
    })
  })

  describe("markAsRead", () => {
    it("marks a notification as read", async () => {
      const mockResponse = {
        id: 1,
        type: "SYSTEM",
        title: "Bienvenido",
        message: "Gracias por usar KoroFin",
        read: true,
        readAt: "2026-07-03T10:00:00Z",
        createdAt: "2026-07-01T10:00:00Z",
      }

      mock.onPatch("/api/notifications/1/read").reply(200, mockResponse)

      const result = await markAsRead(1)

      expect(result.read).toBe(true)
    })
  })

  describe("markAllAsRead", () => {
    it("marks all notifications as read", async () => {
      mock.onPatch("/api/notifications/read-all").reply(204)

      await expect(markAllAsRead()).resolves.toBeUndefined()
    })
  })

  describe("getPreferences", () => {
    it("fetches the current notification preferences", async () => {
      const mockData = {
        paymentReminders: true,
        overspendAlerts: true,
        weeklySummary: false,
        inactivityReminders: false,
        emailEnabled: false,
      }

      mock.onGet("/api/notifications/preferences").reply(200, mockData)

      const result = await getPreferences()

      expect(result.paymentReminders).toBe(true)
      expect(result.emailEnabled).toBe(false)
    })
  })

  describe("updatePreferences", () => {
    it("updates the notification preferences", async () => {
      const payload: NotificationPreferenceRequest = {
        paymentReminders: true,
        overspendAlerts: false,
        weeklySummary: true,
        inactivityReminders: true,
        emailEnabled: true,
      }

      mock.onPut("/api/notifications/preferences").reply(200, payload)

      const result = await updatePreferences(payload)

      expect(result).toEqual(payload)
    })
  })
})
