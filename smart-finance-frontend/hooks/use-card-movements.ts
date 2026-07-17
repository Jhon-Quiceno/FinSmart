import { useCallback, useEffect, useMemo, useState } from "react"
import { getMovements, registerPayment, registerPurchase } from "@/lib/services/card-movement.service"
import type { CardMovementFilters } from "@/lib/services/card-movement.service"
import type { CardMovement, CardPaymentRequest, CardPurchaseRequest } from "@/lib/types/card-movement"
import type { PaginatedResponse } from "@/lib/types/pagination"
import { invalidateCreditCardsCache } from "./use-credit-cards"

const cardMovementsCache = new Map<string, PaginatedResponse<CardMovement>>()
const cardMovementListeners = new Set<() => void>()

function notifyCardMovementListeners() {
  cardMovementListeners.forEach((listener) => listener())
}

export function invalidateCardMovementsCache() {
  cardMovementsCache.clear()
  notifyCardMovementListeners()
}

export function useCardMovements(cardId: number | null, filters: CardMovementFilters = {}) {
  const cacheKey = useMemo(() => JSON.stringify({ cardId, ...filters }), [cardId, filters])
  const cachedData = cardMovementsCache.get(cacheKey)

  const [data, setData] = useState<PaginatedResponse<CardMovement>>({
    content: cachedData?.content ?? [],
    number: cachedData?.number ?? 0,
    size: filters.size ?? 10,
    totalElements: cachedData?.totalElements ?? 0,
    totalPages: cachedData?.totalPages ?? 0,
  })
  const [isLoading, setIsLoading] = useState(!cachedData && cardId !== null)
  const [error, setError] = useState<string | null>(null)
  const [refreshIndex, setRefreshIndex] = useState(0)

  useEffect(() => {
    const listener = () => setRefreshIndex((value) => value + 1)
    cardMovementListeners.add(listener)
    return () => {
      cardMovementListeners.delete(listener)
    }
  }, [])

  const refetch = useCallback(async () => {
    if (cardId === null) return

    const cached = cardMovementsCache.get(cacheKey)
    if (cached) {
      setData(cached)
      setIsLoading(false)
      return
    }

    setIsLoading(true)
    setError(null)

    try {
      const response = await getMovements(cardId, filters)
      cardMovementsCache.set(cacheKey, response)
      setData(response)
    } catch (err) {
      setError(err instanceof Error ? err.message : "No fue posible cargar los movimientos de la tarjeta")
    } finally {
      setIsLoading(false)
    }
  }, [cacheKey, cardId, filters])

  useEffect(() => {
    if (cardId === null) return

    const timeoutId = setTimeout(() => {
      void refetch()
    }, 0)

    return () => {
      clearTimeout(timeoutId)
    }
  }, [cacheKey, cardId, refetch, refreshIndex])

  return {
    movements: data,
    isLoading,
    error,
    refetch,
  }
}

// Registering a purchase changes the card's currentBalance/availableCredit (and, when deferred,
// creates an installment plan), so both caches must be invalidated — mirrors useCreateDebtCharge's
// invalidateDebtChargesCache() + invalidateDebtsCache() pair.
export function useRegisterPurchase() {
  const [isLoading, setIsLoading] = useState(false)

  const mutate = useCallback(async (cardId: number, payload: CardPurchaseRequest): Promise<CardMovement> => {
    setIsLoading(true)
    try {
      const response = await registerPurchase(cardId, payload)
      invalidateCardMovementsCache()
      invalidateCreditCardsCache()
      return response
    } finally {
      setIsLoading(false)
    }
  }, [])

  return {
    registerPurchase: mutate,
    isLoading,
  }
}

// Registering a payment also changes the card's currentBalance/availableCredit — same
// dual-invalidation reasoning as useRegisterPurchase above.
export function useRegisterPayment() {
  const [isLoading, setIsLoading] = useState(false)

  const mutate = useCallback(async (cardId: number, payload: CardPaymentRequest): Promise<CardMovement> => {
    setIsLoading(true)
    try {
      const response = await registerPayment(cardId, payload)
      invalidateCardMovementsCache()
      invalidateCreditCardsCache()
      return response
    } finally {
      setIsLoading(false)
    }
  }, [])

  return {
    registerPayment: mutate,
    isLoading,
  }
}
