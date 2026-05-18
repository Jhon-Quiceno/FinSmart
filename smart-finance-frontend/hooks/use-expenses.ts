import { useCallback, useEffect, useMemo, useState } from "react"
import {
  createExpense,
  deleteExpense,
  getExpenses,
  updateExpense,
} from "@/lib/services/expense.service"
import type { Expense, ExpenseFilters, ExpenseRequest, PaginatedResponse } from "@/lib/types/expense"

const expensesCache = new Map<string, PaginatedResponse<Expense>>()
const expenseListeners = new Set<() => void>()

function notifyExpenseListeners() {
  expenseListeners.forEach((listener) => listener())
}

export function invalidateExpensesCache() {
  expensesCache.clear()
  notifyExpenseListeners()
}

export function useExpenses(filters: ExpenseFilters) {
  const cacheKey = useMemo(() => JSON.stringify(filters), [filters])
  const cachedData = expensesCache.get(cacheKey)

  const [data, setData] = useState<PaginatedResponse<Expense>>({
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
    expenseListeners.add(listener)
    return () => {
      expenseListeners.delete(listener)
    }
  }, [])

  const refetch = useCallback(async () => {
    const cached = expensesCache.get(cacheKey)
    if (cached) {
      setData(cached)
      setIsLoading(false)
      return
    }

    setIsLoading(true)
    setError(null)

    try {
      const response = await getExpenses(filters)
      expensesCache.set(cacheKey, response)
      setData(response)
    } catch (err) {
      setError(err instanceof Error ? err.message : "No fue posible cargar los gastos")
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
    expenses: data,
    isLoading,
    error,
    refetch,
  }
}

export function useCreateExpense() {
  const [isLoading, setIsLoading] = useState(false)

  const mutate = useCallback(async (payload: ExpenseRequest) => {
    setIsLoading(true)
    try {
      const response = await createExpense(payload)
      invalidateExpensesCache()
      return response
    } finally {
      setIsLoading(false)
    }
  }, [])

  return {
    createExpense: mutate,
    isLoading,
  }
}

export function useUpdateExpense() {
  const [isLoading, setIsLoading] = useState(false)

  const mutate = useCallback(async (id: number, payload: ExpenseRequest) => {
    setIsLoading(true)
    try {
      const response = await updateExpense(id, payload)
      invalidateExpensesCache()
      return response
    } finally {
      setIsLoading(false)
    }
  }, [])

  return {
    updateExpense: mutate,
    isLoading,
  }
}

export function useDeleteExpense() {
  const [isLoading, setIsLoading] = useState(false)

  const mutate = useCallback(async (id: number) => {
    setIsLoading(true)
    try {
      await deleteExpense(id)
      invalidateExpensesCache()
    } finally {
      setIsLoading(false)
    }
  }, [])

  return {
    deleteExpense: mutate,
    isLoading,
  }
}
