import { useQuery } from "@tanstack/react-query"

import { getAnalysisRecommendations, getAnalysisSummary } from "@/lib/services/analysis.service"

export const ANALYSIS_SUMMARY_QUERY_KEY = "analysis-summary" as const
export const ANALYSIS_RECOMMENDATIONS_QUERY_KEY = "analysis-recommendations" as const

export function useAnalysisSummary(year?: number, month?: number) {
  return useQuery({
    queryKey: [ANALYSIS_SUMMARY_QUERY_KEY, year ?? null, month ?? null],
    queryFn: () => getAnalysisSummary(year, month),
  })
}

export function useRecommendations(year?: number, month?: number) {
  return useQuery({
    queryKey: [ANALYSIS_RECOMMENDATIONS_QUERY_KEY, year ?? null, month ?? null],
    queryFn: () => getAnalysisRecommendations(year, month),
  })
}
