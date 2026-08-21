import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query"

import {
  createRecurringPayment,
  deleteRecurringPayment,
  getRecurringPayments,
  payRecurringPayment,
  toggleRecurringPayment,
  updateRecurringPayment,
  type RecurringPaymentFilters,
} from "@/lib/services/recurring-payment.service"
import type { RecurringPaymentCreateRequest, RecurringPaymentUpdateRequest } from "@/lib/types/recurring-payment"
import { EXPENSES_QUERY_KEY } from "@/hooks/use-expenses"

export const RECURRING_PAYMENTS_QUERY_KEY = "recurring-payments" as const

export function recurringPaymentsQueryKey(filters: RecurringPaymentFilters = {}) {
  return [RECURRING_PAYMENTS_QUERY_KEY, filters] as const
}

export function useRecurringPayments(filters: RecurringPaymentFilters = {}) {
  return useQuery({
    queryKey: recurringPaymentsQueryKey(filters),
    queryFn: () => getRecurringPayments(filters),
  })
}

export function useCreateRecurringPayment() {
  const queryClient = useQueryClient()
  return useMutation({
    mutationFn: (payload: RecurringPaymentCreateRequest) => createRecurringPayment(payload),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: [RECURRING_PAYMENTS_QUERY_KEY] })
    },
  })
}

export function useUpdateRecurringPayment() {
  const queryClient = useQueryClient()
  return useMutation({
    mutationFn: ({ id, payload }: { id: number; payload: RecurringPaymentUpdateRequest }) =>
      updateRecurringPayment(id, payload),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: [RECURRING_PAYMENTS_QUERY_KEY] })
    },
  })
}

export function useDeleteRecurringPayment() {
  const queryClient = useQueryClient()
  return useMutation({
    mutationFn: (id: number) => deleteRecurringPayment(id),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: [RECURRING_PAYMENTS_QUERY_KEY] })
    },
  })
}

export function useToggleRecurringPayment() {
  const queryClient = useQueryClient()
  return useMutation({
    mutationFn: (id: number) => toggleRecurringPayment(id),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: [RECURRING_PAYMENTS_QUERY_KEY] })
    },
  })
}

// Paying a recurring payment creates an Expense server-side (RecurringPaymentService.payRecurringPayment),
// so both caches must be invalidated for the UI to reflect it without a manual refresh.
export function usePayRecurringPayment() {
  const queryClient = useQueryClient()
  return useMutation({
    mutationFn: (id: number) => payRecurringPayment(id),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: [RECURRING_PAYMENTS_QUERY_KEY] })
      queryClient.invalidateQueries({ queryKey: [EXPENSES_QUERY_KEY] })
    },
  })
}
