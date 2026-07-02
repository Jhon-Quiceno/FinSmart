import { apiClient } from "../api-client"
import type {
  RecurringPayment,
  RecurringPaymentCreateRequest,
  RecurringPaymentPayResult,
  RecurringPaymentUpdateRequest,
} from "../types/recurring-payment"
import type { PaginatedResponse } from "../types/pagination"

export interface RecurringPaymentFilters {
  page?: number
  size?: number
  sort?: string
}

export async function getRecurringPayments(
  filters: RecurringPaymentFilters = {},
): Promise<PaginatedResponse<RecurringPayment>> {
  const response = await apiClient.get<PaginatedResponse<RecurringPayment>>("/api/recurring", {
    params: filters,
  })

  return response.data
}

export async function createRecurringPayment(payload: RecurringPaymentCreateRequest): Promise<RecurringPayment> {
  const response = await apiClient.post<RecurringPayment>("/api/recurring", payload)
  return response.data
}

export async function updateRecurringPayment(
  id: number,
  payload: RecurringPaymentUpdateRequest,
): Promise<RecurringPayment> {
  const response = await apiClient.put<RecurringPayment>(`/api/recurring/${id}`, payload)
  return response.data
}

export async function deleteRecurringPayment(id: number): Promise<void> {
  await apiClient.delete(`/api/recurring/${id}`)
}

export async function toggleRecurringPayment(id: number): Promise<RecurringPayment> {
  const response = await apiClient.patch<RecurringPayment>(`/api/recurring/${id}/toggle`)
  return response.data
}

export async function payRecurringPayment(id: number): Promise<RecurringPaymentPayResult> {
  const response = await apiClient.patch<RecurringPaymentPayResult>(`/api/recurring/${id}/pay`)
  return response.data
}
