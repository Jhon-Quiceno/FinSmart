export interface PaginatedResponse<T> {
  content: T[]
  number: number
  size: number
  totalElements: number
  totalPages: number
}

export interface Income {
  id: number
  amount: number
  description: string | null
  date: string
  isRecurring: boolean
  source: string | null
  categoryId: number | null
  categoryName: string | null
}

export type IncomeResponse = Income

export interface IncomeRequest {
  amount: number
  description?: string
  date: string
  isRecurring?: boolean
  source?: string
  categoryId?: number | null
}

export interface IncomeFilters {
  page?: number
  size?: number
  month?: number
  year?: number
}
