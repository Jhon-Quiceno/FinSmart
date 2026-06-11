export interface Expense {
  id: number
  amount: number
  description: string | null
  date: string
  isRecurring: boolean
  paymentMethod: string
  categoryId: number | null
  categoryName: string | null
}
