import { useCallback, useEffect, useMemo, useState } from "react"
import { categorize, chat, generateInsight, getChatHistory, getLatestInsight } from "@/lib/services/ai.service"
import type { ChatHistoryFilters } from "@/lib/services/ai.service"
import type { CategorizeRequest, CategorizeResponse, ChatMessage, ChatReply, Insight } from "@/lib/types/ai"
import type { PaginatedResponse } from "@/lib/types/pagination"

const chatHistoryCache = new Map<string, PaginatedResponse<ChatMessage>>()
const chatListeners = new Set<() => void>()

function notifyChatListeners() {
  chatListeners.forEach((listener) => listener())
}

export function invalidateChatHistoryCache() {
  chatHistoryCache.clear()
  notifyChatListeners()
}

export function useChatHistory(filters: ChatHistoryFilters = {}) {
  const cacheKey = useMemo(() => JSON.stringify(filters), [filters])
  const cachedData = chatHistoryCache.get(cacheKey)

  const [data, setData] = useState<PaginatedResponse<ChatMessage>>({
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
    chatListeners.add(listener)
    return () => {
      chatListeners.delete(listener)
    }
  }, [])

  const refetch = useCallback(async () => {
    const cached = chatHistoryCache.get(cacheKey)
    if (cached) {
      setData(cached)
      setIsLoading(false)
      return
    }

    setIsLoading(true)
    setError(null)

    try {
      const response = await getChatHistory(filters)
      chatHistoryCache.set(cacheKey, response)
      setData(response)
    } catch (err) {
      setError(err instanceof Error ? err.message : "No fue posible cargar el historial del chat")
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

  return { history: data, isLoading, error, refetch }
}

export function useSendMessage() {
  const [isLoading, setIsLoading] = useState(false)

  const mutate = useCallback(async (message: string): Promise<ChatReply> => {
    setIsLoading(true)
    try {
      const response = await chat(message)
      invalidateChatHistoryCache()
      return response
    } finally {
      setIsLoading(false)
    }
  }, [])

  return { sendMessage: mutate, isLoading }
}

let insightCache: Insight | null | undefined
const insightListeners = new Set<() => void>()

function notifyInsightListeners() {
  insightListeners.forEach((listener) => listener())
}

export function invalidateInsightCache() {
  insightCache = undefined
  notifyInsightListeners()
}

export function useInsight() {
  const [insight, setInsight] = useState<Insight | null>(insightCache ?? null)
  const [isLoading, setIsLoading] = useState(insightCache === undefined)
  const [error, setError] = useState<string | null>(null)
  const [refreshIndex, setRefreshIndex] = useState(0)

  useEffect(() => {
    const listener = () => setRefreshIndex((value) => value + 1)
    insightListeners.add(listener)
    return () => {
      insightListeners.delete(listener)
    }
  }, [])

  const refetch = useCallback(async () => {
    if (insightCache !== undefined) {
      setInsight(insightCache)
      setIsLoading(false)
      return
    }

    setIsLoading(true)
    setError(null)

    try {
      const response = await getLatestInsight()
      insightCache = response
      setInsight(response)
    } catch (err) {
      setError(err instanceof Error ? err.message : "No fue posible cargar el ultimo insight")
    } finally {
      setIsLoading(false)
    }
  }, [])

  useEffect(() => {
    const timeoutId = setTimeout(() => {
      void refetch()
    }, 0)

    return () => {
      clearTimeout(timeoutId)
    }
  }, [refetch, refreshIndex])

  return { insight, isLoading, error, refetch }
}

export function useGenerateInsight() {
  const [isLoading, setIsLoading] = useState(false)

  const mutate = useCallback(async (): Promise<Insight> => {
    setIsLoading(true)
    try {
      const response = await generateInsight()
      insightCache = response
      notifyInsightListeners()
      return response
    } finally {
      setIsLoading(false)
    }
  }, [])

  return { generateInsight: mutate, isLoading }
}

export function useCategorize() {
  const [isLoading, setIsLoading] = useState(false)

  const mutate = useCallback(async (payload: CategorizeRequest): Promise<CategorizeResponse> => {
    setIsLoading(true)
    try {
      return await categorize(payload)
    } finally {
      setIsLoading(false)
    }
  }, [])

  return { categorize: mutate, isLoading }
}
