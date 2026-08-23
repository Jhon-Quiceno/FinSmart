import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query"

import { createWithOfflineFallback } from "@/lib/offline-create"
import {
  createExpense,
  deleteExpense,
  getExpenses,
  updateExpense,
  type ExpenseFilters,
} from "@/lib/services/expense.service"
import type { ExpenseRequest } from "@/lib/types/expense"

export const EXPENSES_QUERY_KEY = "expenses" as const

export function expensesQueryKey(filters: ExpenseFilters = {}) {
  return [EXPENSES_QUERY_KEY, filters] as const
}

export function useExpenses(filters: ExpenseFilters = {}) {
  return useQuery({
    queryKey: expensesQueryKey(filters),
    queryFn: () => getExpenses(filters),
  })
}

export function useCreateExpense() {
  const queryClient = useQueryClient()
  return useMutation({
    mutationFn: (payload: ExpenseRequest) =>
      createWithOfflineFallback("EXPENSE", payload, () => createExpense(payload)),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: [EXPENSES_QUERY_KEY] })
    },
  })
}

export function useUpdateExpense() {
  const queryClient = useQueryClient()
  return useMutation({
    mutationFn: ({ id, payload }: { id: number; payload: ExpenseRequest }) => updateExpense(id, payload),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: [EXPENSES_QUERY_KEY] })
    },
  })
}

export function useDeleteExpense() {
  const queryClient = useQueryClient()
  return useMutation({
    mutationFn: (id: number) => deleteExpense(id),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: [EXPENSES_QUERY_KEY] })
    },
  })
}
