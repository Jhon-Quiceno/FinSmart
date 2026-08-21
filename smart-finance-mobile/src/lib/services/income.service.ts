import { apiClient } from "../api-client"
import type { Income, IncomeRequest } from "../types/income"
import type { PaginatedResponse } from "../types/pagination"

export interface IncomeFilters {
  month?: number
  year?: number
  page?: number
  size?: number
  sort?: string
}

export async function getIncomes(filters: IncomeFilters = {}): Promise<PaginatedResponse<Income>> {
  const response = await apiClient.get<PaginatedResponse<Income>>("/api/incomes", {
    params: filters,
  })

  return response.data
}

export async function createIncome(payload: IncomeRequest): Promise<Income> {
  const response = await apiClient.post<Income>("/api/incomes", payload)
  return response.data
}

export async function updateIncome(id: number, payload: IncomeRequest): Promise<Income> {
  const response = await apiClient.put<Income>(`/api/incomes/${id}`, payload)
  return response.data
}

export async function deleteIncome(id: number): Promise<void> {
  await apiClient.delete(`/api/incomes/${id}`)
}
