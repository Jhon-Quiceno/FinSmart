import { useCallback, useEffect, useMemo, useState } from "react"
import { createDebtPayment, getDebtPayments } from "@/lib/services/debt-payment.service"
import type { DebtPaymentFilters } from "@/lib/services/debt-payment.service"
import type { DebtPayment, DebtPaymentRequest } from "@/lib/types/debt-payment"
import type { PaginatedResponse } from "@/lib/types/pagination"
import { invalidateDebtsCache } from "./use-debts"
import { invalidateExpensesCache } from "./use-expenses"

const debtPaymentsCache = new Map<string, PaginatedResponse<DebtPayment>>()
const debtPaymentListeners = new Set<() => void>()

function notifyDebtPaymentListeners() {
  debtPaymentListeners.forEach((listener) => listener())
}

export function invalidateDebtPaymentsCache() {
  debtPaymentsCache.clear()
  notifyDebtPaymentListeners()
}

export function useDebtPayments(debtId: number | null, filters: DebtPaymentFilters = {}) {
  const cacheKey = useMemo(() => JSON.stringify({ debtId, ...filters }), [debtId, filters])
  const cachedData = debtPaymentsCache.get(cacheKey)

  const [data, setData] = useState<PaginatedResponse<DebtPayment>>({
    content: cachedData?.content ?? [],
    number: cachedData?.number ?? 0,
    size: filters.size ?? 10,
    totalElements: cachedData?.totalElements ?? 0,
    totalPages: cachedData?.totalPages ?? 0,
  })
  const [isLoading, setIsLoading] = useState(!cachedData && debtId !== null)
  const [error, setError] = useState<string | null>(null)
  const [refreshIndex, setRefreshIndex] = useState(0)

  useEffect(() => {
    const listener = () => setRefreshIndex((value) => value + 1)
    debtPaymentListeners.add(listener)
    return () => {
      debtPaymentListeners.delete(listener)
    }
  }, [])

  const refetch = useCallback(async () => {
    if (debtId === null) return

    const cached = debtPaymentsCache.get(cacheKey)
    if (cached) {
      setData(cached)
      setIsLoading(false)
      return
    }

    setIsLoading(true)
    setError(null)

    try {
      const response = await getDebtPayments(debtId, filters)
      debtPaymentsCache.set(cacheKey, response)
      setData(response)
    } catch (err) {
      setError(err instanceof Error ? err.message : "No fue posible cargar el historial de pagos")
    } finally {
      setIsLoading(false)
    }
  }, [cacheKey, debtId, filters])

  useEffect(() => {
    if (debtId === null) return

    const timeoutId = setTimeout(() => {
      void refetch()
    }, 0)

    return () => {
      clearTimeout(timeoutId)
    }
  }, [cacheKey, debtId, refetch, refreshIndex])

  return {
    payments: data,
    isLoading,
    error,
    refetch,
  }
}

export function useCreateDebtPayment() {
  const [isLoading, setIsLoading] = useState(false)

  const mutate = useCallback(async (debtId: number, payload: DebtPaymentRequest) => {
    setIsLoading(true)
    try {
      const response = await createDebtPayment(debtId, payload)
      invalidateDebtPaymentsCache()
      invalidateDebtsCache()
      invalidateExpensesCache()
      return response
    } finally {
      setIsLoading(false)
    }
  }, [])

  return {
    createDebtPayment: mutate,
    isLoading,
  }
}
