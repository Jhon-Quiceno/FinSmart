import { apiClient } from "../api-client"
import type { Debt } from "../types/debt"
import type { DebtCharge, DebtChargeRequest } from "../types/debt-charge"
import type { PaginatedResponse } from "../types/pagination"

export interface DebtChargeFilters {
  page?: number
  size?: number
  sort?: string
}

export async function getDebtCharges(
  debtId: number,
  filters: DebtChargeFilters = {},
): Promise<PaginatedResponse<DebtCharge>> {
  const response = await apiClient.get<PaginatedResponse<DebtCharge>>(`/api/debts/${debtId}/charges`, {
    params: filters,
  })

  return response.data
}

// El backend devuelve la Debt actualizada (con el nuevo remainingAmount), no el DebtCharge creado.
export async function createDebtCharge(debtId: number, payload: DebtChargeRequest): Promise<Debt> {
  const response = await apiClient.post<Debt>(`/api/debts/${debtId}/charges`, payload)
  return response.data
}
