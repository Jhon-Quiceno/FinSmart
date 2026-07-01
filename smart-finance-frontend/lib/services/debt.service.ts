import { apiClient } from "../api-client"
import type { Debt, DebtCreateRequest, DebtUpdateRequest } from "../types/debt"
import type { PaginatedResponse } from "../types/pagination"

export interface DebtFilters {
  page?: number
  size?: number
  sort?: string
}

export async function getDebts(filters: DebtFilters = {}): Promise<PaginatedResponse<Debt>> {
  const response = await apiClient.get<PaginatedResponse<Debt>>("/api/debts", {
    params: filters,
  })

  return response.data
}

export async function createDebt(payload: DebtCreateRequest): Promise<Debt> {
  const response = await apiClient.post<Debt>("/api/debts", payload)
  return response.data
}

export async function updateDebt(id: number, payload: DebtUpdateRequest): Promise<Debt> {
  const response = await apiClient.put<Debt>(`/api/debts/${id}`, payload)
  return response.data
}

export async function deleteDebt(id: number): Promise<void> {
  await apiClient.delete(`/api/debts/${id}`)
}
