import { apiClient } from "../api-client"
import type {
  Transaction,
  TransactionFilters,
  TransactionRequest,
  PaginatedResponse,
} from "../types/transaction"

interface ApiPage<T> {
  content: T[]
  number: number
  size: number
  totalElements: number
  totalPages: number
}

function normalizeTransaction(item: Record<string, unknown>): Transaction {
  return {
    id: Number(item.id),
    accountId: item.accountId === null || item.accountId === undefined
      ? item.account_id === null || item.account_id === undefined
        ? null
        : Number(item.account_id)
      : Number(item.accountId),
    accountName:
      typeof item.accountName === "string"
        ? item.accountName
        : typeof item.account_name === "string"
          ? item.account_name
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
          : typeof item.category === "object" &&
              item.category &&
              "name" in item.category
            ? String((item.category as { name?: unknown }).name ?? "")
            : null,
    type: String(item.type ?? "EXPENSE") as TransactionType,
    amount: Number(item.amount),
    description: typeof item.description === "string" ? item.description : null,
    transactionDate: String(item.transactionDate ?? item.transaction_date ?? item.date ?? ""),
    paymentMethod:
      typeof item.paymentMethod === "string"
        ? item.paymentMethod
        : typeof item.payment_method === "string"
          ? item.payment_method
          : null,
    notes: typeof item.notes === "string" ? item.notes : typeof item.notes === "string" ? item.notes : null,
    createdAt: String(item.createdAt ?? item.created_at ?? ""),
    updatedAt: String(item.updatedAt ?? item.updated_at ?? ""),
  }
}

function normalizePage(
  payload: ApiPage<Record<string, unknown>> | Record<string, unknown>[]
): PaginatedResponse<Transaction> {
  if (Array.isArray(payload)) {
    return {
      content: payload.map(normalizeTransaction),
      number: 0,
      size: payload.length,
      totalElements: payload.length,
      totalPages: 1,
    }
  }

  return {
    content: payload.content.map(normalizeTransaction),
    number: payload.number,
    size: payload.size,
    totalElements: payload.totalElements,
    totalPages: payload.totalPages,
  }
}

export async function getTransactions(
  filters: TransactionFilters = {}
): Promise<PaginatedResponse<Transaction>> {
  const response = await apiClient.get<
    ApiPage<Record<string, unknown>> | Record<string, unknown>[]
  >("/api/transactions", {
    params: filters,
  })

  return normalizePage(response.data)
}

export async function createTransaction(
  payload: TransactionRequest
): Promise<Transaction> {
  const response = await apiClient.post<Record<string, unknown>>(
    "/api/transactions",
    payload
  )
  return normalizeTransaction(response.data)
}

export async function updateTransaction(
  id: number,
  payload: TransactionRequest
): Promise<Transaction> {
  const response = await apiClient.put<Record<string, unknown>>(
    `/api/transactions/${id}`,
    payload
  )
  return normalizeTransaction(response.data)
}

export async function deleteTransaction(id: number): Promise<void> {
  await apiClient.delete(`/api/transactions/${id}`)
}
