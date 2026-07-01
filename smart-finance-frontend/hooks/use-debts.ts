import { useCallback, useEffect, useMemo, useState } from "react"
import {
  createDebt,
  deleteDebt,
  getDebts,
  updateDebt,
} from "@/lib/services/debt.service"
import type { DebtFilters } from "@/lib/services/debt.service"
import type { Debt, DebtCreateRequest, DebtUpdateRequest } from "@/lib/types/debt"
import type { PaginatedResponse } from "@/lib/types/pagination"

const debtsCache = new Map<string, PaginatedResponse<Debt>>()
const debtListeners = new Set<() => void>()

function notifyDebtListeners() {
  debtListeners.forEach((listener) => listener())
}

export function invalidateDebtsCache() {
  debtsCache.clear()
  notifyDebtListeners()
}

export function useDebts(filters: DebtFilters) {
  const cacheKey = useMemo(() => JSON.stringify(filters), [filters])
  const cachedData = debtsCache.get(cacheKey)

  const [data, setData] = useState<PaginatedResponse<Debt>>({
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
    debtListeners.add(listener)
    return () => {
      debtListeners.delete(listener)
    }
  }, [])

  const refetch = useCallback(async () => {
    const cached = debtsCache.get(cacheKey)
    if (cached) {
      setData(cached)
      setIsLoading(false)
      return
    }

    setIsLoading(true)
    setError(null)

    try {
      const response = await getDebts(filters)
      debtsCache.set(cacheKey, response)
      setData(response)
    } catch (err) {
      setError(err instanceof Error ? err.message : "No fue posible cargar las deudas")
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
    debts: data,
    isLoading,
    error,
    refetch,
  }
}

export function useCreateDebt() {
  const [isLoading, setIsLoading] = useState(false)

  const mutate = useCallback(async (payload: DebtCreateRequest) => {
    setIsLoading(true)
    try {
      const response = await createDebt(payload)
      invalidateDebtsCache()
      return response
    } finally {
      setIsLoading(false)
    }
  }, [])

  return {
    createDebt: mutate,
    isLoading,
  }
}

export function useUpdateDebt() {
  const [isLoading, setIsLoading] = useState(false)

  const mutate = useCallback(async (id: number, payload: DebtUpdateRequest) => {
    setIsLoading(true)
    try {
      const response = await updateDebt(id, payload)
      invalidateDebtsCache()
      return response
    } finally {
      setIsLoading(false)
    }
  }, [])

  return {
    updateDebt: mutate,
    isLoading,
  }
}

export function useDeleteDebt() {
  const [isLoading, setIsLoading] = useState(false)

  const mutate = useCallback(async (id: number) => {
    setIsLoading(true)
    try {
      await deleteDebt(id)
      invalidateDebtsCache()
    } finally {
      setIsLoading(false)
    }
  }, [])

  return {
    deleteDebt: mutate,
    isLoading,
  }
}
