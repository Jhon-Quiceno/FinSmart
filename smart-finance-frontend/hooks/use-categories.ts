import { useCallback, useEffect, useMemo, useState } from "react"
import type { Category, CategoryType } from "@/lib/types/category"
import { getCategories } from "@/lib/services/category.service"

const categoriesCache = new Map<string, Category[]>()
const categoryListeners = new Set<() => void>()

export function invalidateCategoriesCache() {
  categoriesCache.clear()
  categoryListeners.forEach((listener) => listener())
}

export function useCategories(type?: CategoryType) {
  const cacheKey = useMemo(() => type ?? "all", [type])
  const cachedCategories = categoriesCache.get(cacheKey) ?? []

  const [categories, setCategories] = useState<Category[]>([])
  const [isLoading, setIsLoading] = useState(cachedCategories.length === 0)
  const [error, setError] = useState<string | null>(null)
  const [refreshIndex, setRefreshIndex] = useState(0)

  useEffect(() => {
    const listener = () => setRefreshIndex((value) => value + 1)
    categoryListeners.add(listener)
    return () => {
      categoryListeners.delete(listener)
    }
  }, [])

  const refetch = useCallback(async () => {
    const cached = categoriesCache.get(cacheKey)
    if (cached) {
      setCategories(cached)
      setIsLoading(false)
      return
    }

    setIsLoading(true)
    setError(null)

    try {
      const response = await getCategories(type)
      categoriesCache.set(cacheKey, response)
      setCategories(response)
    } catch (err) {
      setError(err instanceof Error ? err.message : "No fue posible cargar las categorias")
    } finally {
      setIsLoading(false)
    }
  }, [cacheKey, type])

  useEffect(() => {
    const timeoutId = setTimeout(() => {
      void refetch()
    }, 0)

    return () => {
      clearTimeout(timeoutId)
    }
  }, [cacheKey, refetch, refreshIndex])

  return {
    categories,
    isLoading,
    error,
    refetch,
  }
}
