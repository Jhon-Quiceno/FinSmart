import { useMutation } from "@tanstack/react-query"

import { categorize } from "@/lib/services/ai.service"
import type { CategorizeRequest } from "@/lib/types/ai"

export function useAiCategorize() {
  return useMutation({
    mutationFn: (payload: CategorizeRequest) => categorize(payload),
  })
}
