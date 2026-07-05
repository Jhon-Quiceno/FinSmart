export const NOTIFICATION_TYPES = [
  "PAYMENT_REMINDER",
  "OVERSPEND_ALERT",
  "WEEKLY_SUMMARY",
  "INACTIVITY_REMINDER",
  "MONTH_END_PREDICTION",
  "SYSTEM",
] as const

export type NotificationType = (typeof NOTIFICATION_TYPES)[number]

export interface Notification {
  id: number
  type: NotificationType
  title: string
  message: string
  read: boolean
  readAt: string | null
  createdAt: string
}

export interface NotificationPreference {
  paymentReminders: boolean
  overspendAlerts: boolean
  weeklySummary: boolean
  inactivityReminders: boolean
  emailEnabled: boolean
}

export type NotificationPreferenceRequest = NotificationPreference
