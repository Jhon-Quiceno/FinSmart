export interface Income {
  id: number
  amount: number
  description: string | null
  date: string
  categoryId: number | null
  categoryName: string | null
}

export interface IncomeRequest {
  amount: number
  description?: string
  date: string
  categoryId?: number | null
}
