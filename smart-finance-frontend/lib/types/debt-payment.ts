export interface DebtPayment {
  id: number
  debtId: number
  amount: number
  paymentDate: string
  note: string | null
  createdAt: string
  expenseId: number | null
}

export interface DebtPaymentRequest {
  amount: number
  paymentDate?: string
  note?: string
}
