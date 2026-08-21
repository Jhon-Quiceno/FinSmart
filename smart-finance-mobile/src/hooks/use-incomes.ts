import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query"

import {
  createIncome,
  deleteIncome,
  getIncomes,
  updateIncome,
  type IncomeFilters,
} from "@/lib/services/income.service"
import type { IncomeRequest } from "@/lib/types/income"

export const INCOMES_QUERY_KEY = "incomes" as const

export function incomesQueryKey(filters: IncomeFilters = {}) {
  return [INCOMES_QUERY_KEY, filters] as const
}

export function useIncomes(filters: IncomeFilters = {}) {
  return useQuery({
    queryKey: incomesQueryKey(filters),
    queryFn: () => getIncomes(filters),
  })
}

export function useCreateIncome() {
  const queryClient = useQueryClient()
  return useMutation({
    mutationFn: (payload: IncomeRequest) => createIncome(payload),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: [INCOMES_QUERY_KEY] })
    },
  })
}

export function useUpdateIncome() {
  const queryClient = useQueryClient()
  return useMutation({
    mutationFn: ({ id, payload }: { id: number; payload: IncomeRequest }) => updateIncome(id, payload),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: [INCOMES_QUERY_KEY] })
    },
  })
}

export function useDeleteIncome() {
  const queryClient = useQueryClient()
  return useMutation({
    mutationFn: (id: number) => deleteIncome(id),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: [INCOMES_QUERY_KEY] })
    },
  })
}
