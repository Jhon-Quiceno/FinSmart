export const PAYMENT_METHODS = ["CASH", "DEBIT_CARD", "CREDIT_CARD", "TRANSFER", "OTHER"] as const

export type PaymentMethodType = (typeof PAYMENT_METHODS)[number]

export interface Expense {
  id: number
  amount: number
  description: string | null
  date: string
  paymentMethod: PaymentMethodType
  categoryId: number | null
  categoryName: string | null
  recurringPaymentId: number | null
}

export interface ExpenseRequest {
  amount: number
  description?: string
  date: string
  paymentMethod: PaymentMethodType
  categoryId?: number | null
}
