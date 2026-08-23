import { useQuery } from "@tanstack/react-query"

import { getProvidersStatus } from "@/lib/services/ai.service"

export const AI_PROVIDERS_STATUS_QUERY_KEY = "ai-providers-status" as const

/** GET /api/ai/providers/status es de solo lectura por diseño (se administra desde el backend). */
export function useAiProvidersStatus() {
  return useQuery({
    queryKey: [AI_PROVIDERS_STATUS_QUERY_KEY],
    queryFn: () => getProvidersStatus(),
  })
}
