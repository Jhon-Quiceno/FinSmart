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
