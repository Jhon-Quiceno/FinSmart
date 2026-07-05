import { apiClient } from "../api-client"
import type { AnalysisRecommendation, AnalysisSummary, Prediction } from "../types/analysis"

export interface AnalysisPeriodFilters {
  year?: number
  month?: number
}

export async function getAnalysisSummary(year?: number, month?: number): Promise<AnalysisSummary> {
  const response = await apiClient.get<AnalysisSummary>("/api/analysis/summary", {
    params: { year, month },
  })

  return response.data
}

export async function getAnalysisRecommendations(
  year?: number,
  month?: number,
): Promise<AnalysisRecommendation[]> {
  const response = await apiClient.get<AnalysisRecommendation[]>("/api/analysis/recommendations", {
    params: { year, month },
  })

  return response.data
}

export async function getPrediction(): Promise<Prediction> {
  const response = await apiClient.get<Prediction>("/api/analysis/prediction")
  return response.data
}
