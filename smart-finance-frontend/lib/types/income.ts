export interface Income {
  id: number
  amount: number
  description: string | null
  date: string
  source: string | null
  categoryId: number | null
  categoryName: string | null
}

export interface IncomeRequest {
  amount: number
  description?: string
  date: string
  source?: string
  categoryId?: number | null
}
