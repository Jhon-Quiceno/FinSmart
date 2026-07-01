import { apiClient } from "../api-client"
import type { DebtPayment, DebtPaymentRequest } from "../types/debt-payment"
import type { PaginatedResponse } from "../types/pagination"

export interface DebtPaymentFilters {
  page?: number
  size?: number
  sort?: string
}

export async function getDebtPayments(
  debtId: number,
  filters: DebtPaymentFilters = {},
): Promise<PaginatedResponse<DebtPayment>> {
  const response = await apiClient.get<PaginatedResponse<DebtPayment>>(`/api/debts/${debtId}/payments`, {
    params: filters,
  })

  return response.data
}

export async function createDebtPayment(debtId: number, payload: DebtPaymentRequest): Promise<DebtPayment> {
  const response = await apiClient.post<DebtPayment>(`/api/debts/${debtId}/payments`, payload)
  return response.data
}
