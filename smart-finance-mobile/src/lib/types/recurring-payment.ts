export const RECURRING_FREQUENCIES = ["MONTHLY", "WEEKLY"] as const

export type RecurringFrequency = (typeof RECURRING_FREQUENCIES)[number]

export interface RecurringPayment {
  id: number
  name: string
  amount: number
  frequency: RecurringFrequency
  nextPaymentDate: string
  isActive: boolean
  createdAt: string
  updatedAt: string
}

export interface RecurringPaymentCreateRequest {
  name: string
  amount: number
  frequency: RecurringFrequency
  firstPaymentDate: string
}

export interface RecurringPaymentUpdateRequest {
  name: string
  amount: number
  frequency: RecurringFrequency
}

export interface RecurringPaymentPayResult {
  recurringPayment: RecurringPayment
  expenseId: number
}
