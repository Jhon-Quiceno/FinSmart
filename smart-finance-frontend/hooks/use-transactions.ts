import { useCallback, useEffect, useMemo, useState } from "react"
import {
  createTransaction,
  deleteTransaction,
  getTransactions,
  updateTransaction,
} from "@/lib/services/transaction.service"
import type {
  Transaction,
  TransactionFilters,
  TransactionRequest,
  PaginatedResponse,
} from "@/lib/types/transaction"

const transactionsCache = new Map<string, PaginatedResponse<Transaction>>()
const transactionListeners = new Set<() => void>()

function notifyTransactionListeners() {
  transactionListeners.forEach((listener) => listener())
}

export function invalidateTransactionsCache() {
  transactionsCache.clear()
  notifyTransactionListeners()
}

export function useTransactions(filters: TransactionFilters) {
  const cacheKey = useMemo(() => JSON.stringify(filters), [filters])
  const cachedData = transactionsCache.get(cacheKey)

  const [data, setData] = useState<PaginatedResponse<Transaction>>({
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
    transactionListeners.add(listener)
    return () => {
      transactionListeners.delete(listener)
    }
  }, [])

  const refetch = useCallback(async () => {
    const cached = transactionsCache.get(cacheKey)
    if (cached) {
      setData(cached)
      setIsLoading(false)
      return
    }

    setIsLoading(true)
    setError(null)

    try {
      const response = await getTransactions(filters)
      transactionsCache.set(cacheKey, response)
      setData(response)
    } catch (err) {
      setError(err instanceof Error ? err.message : "No fue posible cargar las transacciones")
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
    transactions: data,
    isLoading,
    error,
    refetch,
  }
}

export function useCreateTransaction() {
  const [isLoading, setIsLoading] = useState(false)

  const mutate = useCallback(async (payload: TransactionRequest) => {
    setIsLoading(true)
    try {
      const response = await createTransaction(payload)
      invalidateTransactionsCache()
      return response
    } finally {
      setIsLoading(false)
    }
  }, [])

  return {
    createTransaction: mutate,
    isLoading,
  }
}

export function useUpdateTransaction() {
  const [isLoading, setIsLoading] = useState(false)

  const mutate = useCallback(async (id: number, payload: TransactionRequest) => {
    setIsLoading(true)
    try {
      const response = await updateTransaction(id, payload)
      invalidateTransactionsCache()
      return response
    } finally {
      setIsLoading(false)
    }
  }, [])

  return {
    updateTransaction: mutate,
    isLoading,
  }
}

export function useDeleteTransaction() {
  const [isLoading, setIsLoading] = useState(false)

  const mutate = useCallback(async (id: number) => {
    setIsLoading(true)
    try {
      await deleteTransaction(id)
      invalidateTransactionsCache()
    } finally {
      setIsLoading(false)
    }
  }, [])

  return {
    deleteTransaction: mutate,
    isLoading,
  }
}
