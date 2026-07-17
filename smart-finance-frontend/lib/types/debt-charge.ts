export interface DebtCharge {
  id: number
  debtId: number
  amount: number
  chargeDate: string
  description: string | null
  createdAt: string
}

export interface DebtChargeRequest {
  amount: number
  chargeDate?: string
  description?: string
}
