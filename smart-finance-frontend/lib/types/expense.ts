import type { PaginatedResponse } from "@/lib/types/income"

export type { PaginatedResponse }

export interface Expense {
  id: number
  amount: number
  description: string | null
  date: string
  isRecurring: boolean
  paymentMethod: string | null
  categoryId: number | null
  categoryName: string | null
}

export type ExpenseResponse = Expense

export interface ExpenseRequest {
  amount: number
  description?: string
  date: string
  isRecurring?: boolean
  paymentMethod?: string
  categoryId?: number | null
}

export interface ExpenseFilters {
  page?: number
  size?: number
  categoryId?: number
  from?: string
  to?: string
}
