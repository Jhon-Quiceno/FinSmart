import { apiClient } from "@/lib/api-client"
import type { Income, IncomeFilters, IncomeRequest, PaginatedResponse } from "@/lib/types/income"

interface ApiPage<T> {
  content: T[]
  number: number
  size: number
  totalElements: number
  totalPages: number
}

function normalizeIncome(item: Record<string, unknown>): Income {
  return {
    id: Number(item.id),
    amount: Number(item.amount),
    description: typeof item.description === "string" ? item.description : null,
    date: String(item.date ?? ""),
    isRecurring: Boolean(item.isRecurring ?? item.is_recurring),
    source: typeof item.source === "string" ? item.source : null,
    categoryId:
      item.categoryId === null || item.categoryId === undefined
        ? item.category_id === null || item.category_id === undefined
          ? null
          : Number(item.category_id)
        : Number(item.categoryId),
    categoryName:
      typeof item.categoryName === "string"
        ? item.categoryName
        : typeof item.category_name === "string"
          ? item.category_name
          : typeof item.category === "object" && item.category && "name" in item.category
            ? String((item.category as { name?: unknown }).name ?? "")
            : null,
  }
}

function normalizePage(payload: ApiPage<Record<string, unknown>> | Record<string, unknown>[]): PaginatedResponse<Income> {
  if (Array.isArray(payload)) {
    return {
      content: payload.map(normalizeIncome),
      number: 0,
      size: payload.length,
      totalElements: payload.length,
      totalPages: 1,
    }
  }

  return {
    content: payload.content.map(normalizeIncome),
    number: payload.number,
    size: payload.size,
    totalElements: payload.totalElements,
    totalPages: payload.totalPages,
  }
}

export async function getIncomes(filters: IncomeFilters = {}): Promise<PaginatedResponse<Income>> {
  const response = await apiClient.get<ApiPage<Record<string, unknown>> | Record<string, unknown>[]>("/api/incomes", {
    params: filters,
  })

  return normalizePage(response.data)
}

export async function createIncome(payload: IncomeRequest): Promise<Income> {
  const response = await apiClient.post<Record<string, unknown>>("/api/incomes", payload)
  return normalizeIncome(response.data)
}

export async function updateIncome(id: number, payload: IncomeRequest): Promise<Income> {
  const response = await apiClient.put<Record<string, unknown>>(`/api/incomes/${id}`, payload)
  return normalizeIncome(response.data)
}

export async function deleteIncome(id: number): Promise<void> {
  await apiClient.delete(`/api/incomes/${id}`)
}
