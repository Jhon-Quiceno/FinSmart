import { useCallback, useEffect, useMemo, useState } from "react"
import { createDebtCharge, getDebtCharges } from "@/lib/services/debt-charge.service"
import type { DebtChargeFilters } from "@/lib/services/debt-charge.service"
import type { Debt } from "@/lib/types/debt"
import type { DebtCharge, DebtChargeRequest } from "@/lib/types/debt-charge"
import type { PaginatedResponse } from "@/lib/types/pagination"
import { invalidateDebtsCache } from "./use-debts"

const debtChargesCache = new Map<string, PaginatedResponse<DebtCharge>>()
const debtChargeListeners = new Set<() => void>()

function notifyDebtChargeListeners() {
  debtChargeListeners.forEach((listener) => listener())
}

export function invalidateDebtChargesCache() {
  debtChargesCache.clear()
  notifyDebtChargeListeners()
}

export function useDebtCharges(debtId: number | null, filters: DebtChargeFilters = {}) {
  const cacheKey = useMemo(() => JSON.stringify({ debtId, ...filters }), [debtId, filters])
  const cachedData = debtChargesCache.get(cacheKey)

  const [data, setData] = useState<PaginatedResponse<DebtCharge>>({
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
    debtChargeListeners.add(listener)
    return () => {
      debtChargeListeners.delete(listener)
    }
  }, [])

  const refetch = useCallback(async () => {
    if (debtId === null) return

    const cached = debtChargesCache.get(cacheKey)
    if (cached) {
      setData(cached)
      setIsLoading(false)
      return
    }

    setIsLoading(true)
    setError(null)

    try {
      const response = await getDebtCharges(debtId, filters)
      debtChargesCache.set(cacheKey, response)
      setData(response)
    } catch (err) {
      setError(err instanceof Error ? err.message : "No fue posible cargar el historial de cargos")
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
    charges: data,
    isLoading,
    error,
    refetch,
  }
}

export function useCreateDebtCharge() {
  const [isLoading, setIsLoading] = useState(false)

  const mutate = useCallback(async (debtId: number, payload: DebtChargeRequest): Promise<Debt> => {
    setIsLoading(true)
    try {
      const response = await createDebtCharge(debtId, payload)
      invalidateDebtChargesCache()
      invalidateDebtsCache()
      return response
    } finally {
      setIsLoading(false)
    }
  }, [])

  return {
    createDebtCharge: mutate,
    isLoading,
  }
}
