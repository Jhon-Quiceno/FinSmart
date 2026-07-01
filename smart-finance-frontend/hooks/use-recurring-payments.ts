import { useCallback, useEffect, useMemo, useState } from "react"
import {
  createRecurringPayment,
  deleteRecurringPayment,
  getRecurringPayments,
  payRecurringPayment,
  toggleRecurringPayment,
  updateRecurringPayment,
} from "@/lib/services/recurring-payment.service"
import type { RecurringPaymentFilters } from "@/lib/services/recurring-payment.service"
import type {
  RecurringPayment,
  RecurringPaymentCreateRequest,
  RecurringPaymentUpdateRequest,
} from "@/lib/types/recurring-payment"
import type { PaginatedResponse } from "@/lib/types/pagination"
import { invalidateExpensesCache } from "./use-expenses"

const recurringPaymentsCache = new Map<string, PaginatedResponse<RecurringPayment>>()
const recurringPaymentListeners = new Set<() => void>()

function notifyRecurringPaymentListeners() {
  recurringPaymentListeners.forEach((listener) => listener())
}

export function invalidateRecurringPaymentsCache() {
  recurringPaymentsCache.clear()
  notifyRecurringPaymentListeners()
}

export function useRecurringPayments(filters: RecurringPaymentFilters = {}) {
  const cacheKey = useMemo(() => JSON.stringify(filters), [filters])
  const cachedData = recurringPaymentsCache.get(cacheKey)

  const [data, setData] = useState<PaginatedResponse<RecurringPayment>>({
    content: cachedData?.content ?? [],
    number: cachedData?.number ?? 0,
    size: filters.size ?? 10,
    totalElements: cachedData?.totalElements ?? 0,
    totalPages: cachedData?.totalPages ?? 0,
  })
  const [isLoading, setIsLoading] = useState(!cachedData)
  const [error, setError] = useState<string | null>(null)
  const [refreshIndex, setRefreshIndex] = useState(0)

  useEffect(() => {
    const listener = () => setRefreshIndex((value) => value + 1)
    recurringPaymentListeners.add(listener)
    return () => {
      recurringPaymentListeners.delete(listener)
    }
  }, [])

  const refetch = useCallback(async () => {
    const cached = recurringPaymentsCache.get(cacheKey)
    if (cached) {
      setData(cached)
      setIsLoading(false)
      return
    }

    setIsLoading(true)
    setError(null)

    try {
      const response = await getRecurringPayments(filters)
      recurringPaymentsCache.set(cacheKey, response)
      setData(response)
    } catch (err) {
      setError(err instanceof Error ? err.message : "No fue posible cargar los servicios recurrentes")
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
    recurringPayments: data,
    isLoading,
    error,
    refetch,
  }
}

export function useCreateRecurringPayment() {
  const [isLoading, setIsLoading] = useState(false)

  const mutate = useCallback(async (payload: RecurringPaymentCreateRequest) => {
    setIsLoading(true)
    try {
      const response = await createRecurringPayment(payload)
      invalidateRecurringPaymentsCache()
      return response
    } finally {
      setIsLoading(false)
    }
  }, [])

  return {
    createRecurringPayment: mutate,
    isLoading,
  }
}

export function useUpdateRecurringPayment() {
  const [isLoading, setIsLoading] = useState(false)

  const mutate = useCallback(async (id: number, payload: RecurringPaymentUpdateRequest) => {
    setIsLoading(true)
    try {
      const response = await updateRecurringPayment(id, payload)
      invalidateRecurringPaymentsCache()
      return response
    } finally {
      setIsLoading(false)
    }
  }, [])

  return {
    updateRecurringPayment: mutate,
    isLoading,
  }
}

export function useDeleteRecurringPayment() {
  const [isLoading, setIsLoading] = useState(false)

  const mutate = useCallback(async (id: number) => {
    setIsLoading(true)
    try {
      await deleteRecurringPayment(id)
      invalidateRecurringPaymentsCache()
    } finally {
      setIsLoading(false)
    }
  }, [])

  return {
    deleteRecurringPayment: mutate,
    isLoading,
  }
}

export function useToggleRecurringPayment() {
  const [isLoading, setIsLoading] = useState(false)

  const mutate = useCallback(async (id: number) => {
    setIsLoading(true)
    try {
      const response = await toggleRecurringPayment(id)
      invalidateRecurringPaymentsCache()
      return response
    } finally {
      setIsLoading(false)
    }
  }, [])

  return {
    toggleRecurringPayment: mutate,
    isLoading,
  }
}

export function usePayRecurringPayment() {
  const [isLoading, setIsLoading] = useState(false)

  const mutate = useCallback(async (id: number) => {
    setIsLoading(true)
    try {
      const response = await payRecurringPayment(id)
      invalidateRecurringPaymentsCache()
      invalidateExpensesCache()
      return response
    } finally {
      setIsLoading(false)
    }
  }, [])

  return {
    payRecurringPayment: mutate,
    isLoading,
  }
}
