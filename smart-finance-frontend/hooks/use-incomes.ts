import { useCallback, useEffect, useMemo, useState } from "react"
import {
  createIncome,
  deleteIncome,
  getIncomes,
  updateIncome,
} from "@/lib/services/income.service"
import type { IncomeFilters } from "@/lib/services/income.service"
import type { Income, IncomeRequest } from "@/lib/types/income"
import type { PaginatedResponse } from "@/lib/types/pagination"

const incomesCache = new Map<string, PaginatedResponse<Income>>()
const incomeListeners = new Set<() => void>()

function notifyIncomeListeners() {
  incomeListeners.forEach((listener) => listener())
}

export function invalidateIncomesCache() {
  incomesCache.clear()
  notifyIncomeListeners()
}

export function useIncomes(filters: IncomeFilters) {
  const cacheKey = useMemo(() => JSON.stringify(filters), [filters])
  const cachedData = incomesCache.get(cacheKey)

  const [data, setData] = useState<PaginatedResponse<Income>>({
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
    incomeListeners.add(listener)
    return () => {
      incomeListeners.delete(listener)
    }
  }, [])

  const refetch = useCallback(async () => {
    const cached = incomesCache.get(cacheKey)
    if (cached) {
      setData(cached)
      setIsLoading(false)
      return
    }

    setIsLoading(true)
    setError(null)

    try {
      const response = await getIncomes(filters)
      incomesCache.set(cacheKey, response)
      setData(response)
    } catch (err) {
      setError(err instanceof Error ? err.message : "No fue posible cargar los ingresos")
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
    incomes: data,
    isLoading,
    error,
    refetch,
  }
}

export function useCreateIncome() {
  const [isLoading, setIsLoading] = useState(false)

  const mutate = useCallback(async (payload: IncomeRequest) => {
    setIsLoading(true)
    try {
      const response = await createIncome(payload)
      invalidateIncomesCache()
      return response
    } finally {
      setIsLoading(false)
    }
  }, [])

  return {
    createIncome: mutate,
    isLoading,
  }
}

export function useUpdateIncome() {
  const [isLoading, setIsLoading] = useState(false)

  const mutate = useCallback(async (id: number, payload: IncomeRequest) => {
    setIsLoading(true)
    try {
      const response = await updateIncome(id, payload)
      invalidateIncomesCache()
      return response
    } finally {
      setIsLoading(false)
    }
  }, [])

  return {
    updateIncome: mutate,
    isLoading,
  }
}

export function useDeleteIncome() {
  const [isLoading, setIsLoading] = useState(false)

  const mutate = useCallback(async (id: number) => {
    setIsLoading(true)
    try {
      await deleteIncome(id)
      invalidateIncomesCache()
    } finally {
      setIsLoading(false)
    }
  }, [])

  return {
    deleteIncome: mutate,
    isLoading,
  }
}
