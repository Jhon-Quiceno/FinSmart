import { useCallback, useEffect, useMemo, useState } from "react"
import { getApiErrorMessage } from "@/lib/api-client"
import { getMonthlyReport, getReportMovements } from "@/lib/services/report.service"
import type { MonthlyReportFilters } from "@/lib/services/report.service"
import type { MonthlyReport, ReportMovementRow } from "@/lib/types/report"

const reportCache = new Map<string, MonthlyReport>()
const reportListeners = new Set<() => void>()

function notifyReportListeners() {
  reportListeners.forEach((listener) => listener())
}

export function invalidateReportCache() {
  reportCache.clear()
  notifyReportListeners()
}

export function useMonthlyReport(filters: MonthlyReportFilters) {
  const cacheKey = useMemo(() => JSON.stringify(filters), [filters])
  const cachedData = reportCache.get(cacheKey)

  const [data, setData] = useState<MonthlyReport | null>(cachedData ?? null)
  const [isLoading, setIsLoading] = useState(!cachedData)
  const [error, setError] = useState<string | null>(null)
  const [refreshIndex, setRefreshIndex] = useState(0)

  useEffect(() => {
    const listener = () => setRefreshIndex((value) => value + 1)
    reportListeners.add(listener)
    return () => {
      reportListeners.delete(listener)
    }
  }, [])

  const refetch = useCallback(async () => {
    const cached = reportCache.get(cacheKey)
    if (cached) {
      setData(cached)
      setIsLoading(false)
      return
    }

    setIsLoading(true)
    setError(null)

    try {
      const response = await getMonthlyReport(filters)
      reportCache.set(cacheKey, response)
      setData(response)
    } catch (err) {
      setError(getApiErrorMessage(err, "No fue posible cargar el reporte mensual"))
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
    report: data,
    isLoading,
    error,
    refetch,
  }
}

const reportMovementsCache = new Map<string, ReportMovementRow[]>()
const reportMovementsListeners = new Set<() => void>()

function notifyReportMovementsListeners() {
  reportMovementsListeners.forEach((listener) => listener())
}

export function invalidateReportMovementsCache() {
  reportMovementsCache.clear()
  notifyReportMovementsListeners()
}

export function useReportMovements(filters: MonthlyReportFilters) {
  const cacheKey = useMemo(() => JSON.stringify(filters), [filters])
  const cachedData = reportMovementsCache.get(cacheKey)

  const [data, setData] = useState<ReportMovementRow[]>(cachedData ?? [])
  const [isLoading, setIsLoading] = useState(!cachedData)
  const [error, setError] = useState<string | null>(null)
  const [refreshIndex, setRefreshIndex] = useState(0)

  useEffect(() => {
    const listener = () => setRefreshIndex((value) => value + 1)
    reportMovementsListeners.add(listener)
    return () => {
      reportMovementsListeners.delete(listener)
    }
  }, [])

  const refetch = useCallback(async () => {
    const cached = reportMovementsCache.get(cacheKey)
    if (cached) {
      setData(cached)
      setIsLoading(false)
      return
    }

    setIsLoading(true)
    setError(null)

    try {
      const response = await getReportMovements(filters)
      reportMovementsCache.set(cacheKey, response)
      setData(response)
    } catch (err) {
      setError(getApiErrorMessage(err, "No fue posible cargar los movimientos"))
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
    movements: data,
    isLoading,
    error,
    refetch,
  }
}
