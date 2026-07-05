import { useEffect, useState } from "react"
import { getAnalysisRecommendations, getAnalysisSummary, getPrediction } from "@/lib/services/analysis.service"
import type { AnalysisRecommendation, AnalysisSummary, Prediction } from "@/lib/types/analysis"

export function useAnalysisSummary(year?: number, month?: number) {
  const [data, setData] = useState<AnalysisSummary | null>(null)
  const [isLoading, setIsLoading] = useState(true)
  const [error, setError] = useState<string | null>(null)

  useEffect(() => {
    let cancelled = false

    const load = async () => {
      setIsLoading(true)
      setError(null)

      try {
        const response = await getAnalysisSummary(year, month)
        if (!cancelled) {
          setData(response)
        }
      } catch (err) {
        if (!cancelled) {
          setError(err instanceof Error ? err.message : "No fue posible cargar el resumen financiero")
        }
      } finally {
        if (!cancelled) {
          setIsLoading(false)
        }
      }
    }

    void load()

    return () => {
      cancelled = true
    }
  }, [year, month])

  return { data, isLoading, error }
}

export function usePrediction() {
  const [data, setData] = useState<Prediction | null>(null)
  const [isLoading, setIsLoading] = useState(true)
  const [error, setError] = useState<string | null>(null)

  useEffect(() => {
    let cancelled = false

    const load = async () => {
      setIsLoading(true)
      setError(null)

      try {
        const response = await getPrediction()
        if (!cancelled) {
          setData(response)
        }
      } catch (err) {
        if (!cancelled) {
          setError(err instanceof Error ? err.message : "No fue posible cargar la prediccion de fin de mes")
        }
      } finally {
        if (!cancelled) {
          setIsLoading(false)
        }
      }
    }

    void load()

    return () => {
      cancelled = true
    }
  }, [])

  return { data, isLoading, error }
}

export function useAnalysisRecommendations(year?: number, month?: number) {
  const [data, setData] = useState<AnalysisRecommendation[]>([])
  const [isLoading, setIsLoading] = useState(true)
  const [error, setError] = useState<string | null>(null)

  useEffect(() => {
    let cancelled = false

    const load = async () => {
      setIsLoading(true)
      setError(null)

      try {
        const response = await getAnalysisRecommendations(year, month)
        if (!cancelled) {
          setData(response)
        }
      } catch (err) {
        if (!cancelled) {
          setError(err instanceof Error ? err.message : "No fue posible cargar las recomendaciones")
        }
      } finally {
        if (!cancelled) {
          setIsLoading(false)
        }
      }
    }

    void load()

    return () => {
      cancelled = true
    }
  }, [year, month])

  return { data, isLoading, error }
}
