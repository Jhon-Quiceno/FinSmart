import { useCallback, useEffect, useMemo, useState } from "react"
import {
  getNotifications,
  getPreferences,
  getUnreadCount,
  markAllAsRead,
  markAsRead,
  updatePreferences,
} from "@/lib/services/notification.service"
import type { NotificationFilters } from "@/lib/services/notification.service"
import type { Notification, NotificationPreference, NotificationPreferenceRequest } from "@/lib/types/notification"
import type { PaginatedResponse } from "@/lib/types/pagination"

const UNREAD_COUNT_POLL_INTERVAL_MS = 60_000

const notificationsCache = new Map<string, PaginatedResponse<Notification>>()
const notificationListeners = new Set<() => void>()
let unreadCountCache: number | null = null
let preferencesCache: NotificationPreference | null = null

function notifyNotificationListeners() {
  notificationListeners.forEach((listener) => listener())
}

/**
 * Clears every notification-related cache (list, unread count, preferences) and notifies all
 * active hook instances to refetch. Call this after any mutation that changes notification state
 * (mark as read, mark all as read, update preferences).
 */
export function invalidateNotificationsCache() {
  notificationsCache.clear()
  unreadCountCache = null
  notifyNotificationListeners()
}

export function useNotifications(filters: NotificationFilters = {}) {
  const cacheKey = useMemo(() => JSON.stringify(filters), [filters])
  const cachedData = notificationsCache.get(cacheKey)

  const [data, setData] = useState<PaginatedResponse<Notification>>({
    content: cachedData?.content ?? [],
    number: cachedData?.number ?? 0,
    size: filters.size ?? 20,
    totalElements: cachedData?.totalElements ?? 0,
    totalPages: cachedData?.totalPages ?? 0,
  })
  const [isLoading, setIsLoading] = useState(!cachedData)
  const [error, setError] = useState<string | null>(null)
  const [refreshIndex, setRefreshIndex] = useState(0)

  useEffect(() => {
    const listener = () => setRefreshIndex((value) => value + 1)
    notificationListeners.add(listener)
    return () => {
      notificationListeners.delete(listener)
    }
  }, [])

  const refetch = useCallback(async () => {
    const cached = notificationsCache.get(cacheKey)
    if (cached) {
      setData(cached)
      setIsLoading(false)
      return
    }

    setIsLoading(true)
    setError(null)

    try {
      const response = await getNotifications(filters)
      notificationsCache.set(cacheKey, response)
      setData(response)
    } catch (err) {
      setError(err instanceof Error ? err.message : "No fue posible cargar las notificaciones")
    } finally {
      setIsLoading(false)
    }
  }, [cacheKey, filters])

  useEffect(() => {
    const timeoutId = setTimeout(() => {
      void refetch()
    }, 0)

    return () => {
      clearTimeout(timeoutId)
    }
  }, [cacheKey, refetch, refreshIndex])

  return {
    notifications: data,
    isLoading,
    error,
    refetch,
  }
}

export function useUnreadCount() {
  const [count, setCount] = useState(unreadCountCache ?? 0)
  const [isLoading, setIsLoading] = useState(unreadCountCache === null)
  const [error, setError] = useState<string | null>(null)

  const refetch = useCallback(async () => {
    try {
      const response = await getUnreadCount()
      unreadCountCache = response
      setCount(response)
      setError(null)
    } catch {
      // Quiet failure: a notification badge should never crash the navbar when the backend is
      // unreachable, it should just stay at its last known value.
      setError("No fue posible actualizar las notificaciones")
    } finally {
      setIsLoading(false)
    }
  }, [])

  useEffect(() => {
    const timeoutId = setTimeout(() => {
      void refetch()
    }, 0)

    const listener = () => {
      void refetch()
    }
    notificationListeners.add(listener)

    const intervalId = setInterval(() => {
      void refetch()
    }, UNREAD_COUNT_POLL_INTERVAL_MS)

    return () => {
      clearTimeout(timeoutId)
      notificationListeners.delete(listener)
      clearInterval(intervalId)
    }
  }, [refetch])

  return { unreadCount: count, isLoading, error, refetch }
}

export function useMarkAsRead() {
  const [isLoading, setIsLoading] = useState(false)

  const mutate = useCallback(async (id: number) => {
    setIsLoading(true)
    try {
      const response = await markAsRead(id)
      invalidateNotificationsCache()
      return response
    } finally {
      setIsLoading(false)
    }
  }, [])

  return { markAsRead: mutate, isLoading }
}

export function useMarkAllAsRead() {
  const [isLoading, setIsLoading] = useState(false)

  const mutate = useCallback(async () => {
    setIsLoading(true)
    try {
      await markAllAsRead()
      invalidateNotificationsCache()
    } finally {
      setIsLoading(false)
    }
  }, [])

  return { markAllAsRead: mutate, isLoading }
}

export function useNotificationPreferences() {
  const [preferences, setPreferences] = useState<NotificationPreference | null>(preferencesCache)
  const [isLoading, setIsLoading] = useState(!preferencesCache)
  const [error, setError] = useState<string | null>(null)

  const refetch = useCallback(async () => {
    if (preferencesCache) {
      setPreferences(preferencesCache)
      setIsLoading(false)
      return
    }

    setIsLoading(true)
    setError(null)

    try {
      const response = await getPreferences()
      preferencesCache = response
      setPreferences(response)
    } catch (err) {
      setError(err instanceof Error ? err.message : "No fue posible cargar las preferencias de notificacion")
    } finally {
      setIsLoading(false)
    }
  }, [])

  useEffect(() => {
    const timeoutId = setTimeout(() => {
      void refetch()
    }, 0)

    const listener = () => {
      void refetch()
    }
    notificationListeners.add(listener)

    return () => {
      clearTimeout(timeoutId)
      notificationListeners.delete(listener)
    }
  }, [refetch])

  return { preferences, isLoading, error, refetch }
}

export function useUpdateNotificationPreferences() {
  const [isLoading, setIsLoading] = useState(false)

  const mutate = useCallback(async (payload: NotificationPreferenceRequest) => {
    setIsLoading(true)
    try {
      const response = await updatePreferences(payload)
      preferencesCache = response
      notifyNotificationListeners()
      return response
    } finally {
      setIsLoading(false)
    }
  }, [])

  return { updatePreferences: mutate, isLoading }
}
