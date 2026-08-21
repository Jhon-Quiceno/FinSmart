import { apiClient } from "../api-client"
import type { Expense, ExpenseRequest, PaymentMethodType } from "../types/expense"
import type { PaginatedResponse } from "../types/pagination"

export interface ExpenseFilters {
  categoryId?: number
  from?: string
  to?: string
  paymentMethod?: PaymentMethodType
  page?: number
  size?: number
  sort?: string
}

export async function getExpenses(filters: ExpenseFilters = {}): Promise<PaginatedResponse<Expense>> {
  const response = await apiClient.get<PaginatedResponse<Expense>>("/api/expenses", {
    params: filters,
  })

  return response.data
}

export async function createExpense(payload: ExpenseRequest): Promise<Expense> {
  const response = await apiClient.post<Expense>("/api/expenses", payload)
  return response.data
}

export async function updateExpense(id: number, payload: ExpenseRequest): Promise<Expense> {
  const response = await apiClient.put<Expense>(`/api/expenses/${id}`, payload)
  return response.data
}

export async function deleteExpense(id: number): Promise<void> {
  await apiClient.delete(`/api/expenses/${id}`)
}
