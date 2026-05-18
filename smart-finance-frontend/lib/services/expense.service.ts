import { apiClient } from "@/lib/api-client"
import type { Expense, ExpenseFilters, ExpenseRequest, PaginatedResponse } from "@/lib/types/expense"

interface ApiPage<T> {
  content: T[]
  number: number
  size: number
  totalElements: number
  totalPages: number
}

function normalizeExpense(item: Record<string, unknown>): Expense {
  return {
    id: Number(item.id),
    amount: Number(item.amount),
    description: typeof item.description === "string" ? item.description : null,
    date: String(item.date ?? ""),
    isRecurring: Boolean(item.isRecurring ?? item.is_recurring),
    paymentMethod:
      typeof item.paymentMethod === "string"
        ? item.paymentMethod
        : typeof item.payment_method === "string"
          ? item.payment_method
          : null,
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

function normalizePage(payload: ApiPage<Record<string, unknown>> | Record<string, unknown>[]): PaginatedResponse<Expense> {
  if (Array.isArray(payload)) {
    return {
      content: payload.map(normalizeExpense),
      number: 0,
      size: payload.length,
      totalElements: payload.length,
      totalPages: 1,
    }
  }

  return {
    content: payload.content.map(normalizeExpense),
    number: payload.number,
    size: payload.size,
    totalElements: payload.totalElements,
    totalPages: payload.totalPages,
  }
}

export async function getExpenses(filters: ExpenseFilters = {}): Promise<PaginatedResponse<Expense>> {
  const response = await apiClient.get<ApiPage<Record<string, unknown>> | Record<string, unknown>[]>("/api/expenses", {
    params: filters,
  })

  return normalizePage(response.data)
}

export async function createExpense(payload: ExpenseRequest): Promise<Expense> {
  const response = await apiClient.post<Record<string, unknown>>("/api/expenses", payload)
  return normalizeExpense(response.data)
}

export async function updateExpense(id: number, payload: ExpenseRequest): Promise<Expense> {
  const response = await apiClient.put<Record<string, unknown>>(`/api/expenses/${id}`, payload)
  return normalizeExpense(response.data)
}

export async function deleteExpense(id: number): Promise<void> {
  await apiClient.delete(`/api/expenses/${id}`)
}
