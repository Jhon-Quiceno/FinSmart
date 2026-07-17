import { useCallback, useEffect, useMemo, useState } from "react"
import {
  createCard,
  deleteCard,
  getCards,
  updateCard,
} from "@/lib/services/credit-card.service"
import type { CreditCardFilters } from "@/lib/services/credit-card.service"
import type { CreditCard, CreditCardCreateRequest, CreditCardUpdateRequest } from "@/lib/types/credit-card"
import type { PaginatedResponse } from "@/lib/types/pagination"

const creditCardsCache = new Map<string, PaginatedResponse<CreditCard>>()
const creditCardListeners = new Set<() => void>()

function notifyCreditCardListeners() {
  creditCardListeners.forEach((listener) => listener())
}

// Exported so use-card-movements.ts can invalidate cards too: registering a movement changes
// currentBalance/availableCredit, which lives on the card resource, not the movement.
export function invalidateCreditCardsCache() {
  creditCardsCache.clear()
  notifyCreditCardListeners()
}

export function useCreditCards(filters: CreditCardFilters) {
  const cacheKey = useMemo(() => JSON.stringify(filters), [filters])
  const cachedData = creditCardsCache.get(cacheKey)

  const [data, setData] = useState<PaginatedResponse<CreditCard>>({
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
    creditCardListeners.add(listener)
    return () => {
      creditCardListeners.delete(listener)
    }
  }, [])

  const refetch = useCallback(async () => {
    const cached = creditCardsCache.get(cacheKey)
    if (cached) {
      setData(cached)
      setIsLoading(false)
      return
    }

    setIsLoading(true)
    setError(null)

    try {
      const response = await getCards(filters)
      creditCardsCache.set(cacheKey, response)
      setData(response)
    } catch (err) {
      setError(err instanceof Error ? err.message : "No fue posible cargar las tarjetas")
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
    cards: data,
    isLoading,
    error,
    refetch,
  }
}

export function useCreateCreditCard() {
  const [isLoading, setIsLoading] = useState(false)

  const mutate = useCallback(async (payload: CreditCardCreateRequest) => {
    setIsLoading(true)
    try {
      const response = await createCard(payload)
      invalidateCreditCardsCache()
      return response
    } finally {
      setIsLoading(false)
    }
  }, [])

  return {
    createCreditCard: mutate,
    isLoading,
  }
}

export function useUpdateCreditCard() {
  const [isLoading, setIsLoading] = useState(false)

  const mutate = useCallback(async (id: number, payload: CreditCardUpdateRequest) => {
    setIsLoading(true)
    try {
      const response = await updateCard(id, payload)
      invalidateCreditCardsCache()
      return response
    } finally {
      setIsLoading(false)
    }
  }, [])

  return {
    updateCreditCard: mutate,
    isLoading,
  }
}

export function useDeleteCreditCard() {
  const [isLoading, setIsLoading] = useState(false)

  const mutate = useCallback(async (id: number) => {
    setIsLoading(true)
    try {
      await deleteCard(id)
      invalidateCreditCardsCache()
    } finally {
      setIsLoading(false)
    }
  }, [])

  return {
    deleteCreditCard: mutate,
    isLoading,
  }
}
