export const CARD_MOVEMENT_TYPES = ["PURCHASE", "INSTALLMENT_PURCHASE", "PAYMENT", "INTEREST", "FEE"] as const

export type CardMovementType = (typeof CARD_MOVEMENT_TYPES)[number]

export const INSTALLMENT_STATUSES = ["PENDING", "BILLED"] as const

export type InstallmentStatus = (typeof INSTALLMENT_STATUSES)[number]

export interface CardMovement {
  id: number
  cardId: number
  type: CardMovementType
  amount: number
  date: string
  description: string | null
  // Null on list responses (mirrors DebtPaymentResponse#expenseId list gap on the backend);
  // populated on the create (purchase/payment) response.
  cardBalanceAfter: number | null
  expenseId: number | null
  installmentPlanId: number | null
  createdAt: string
}

export interface CardPurchaseRequest {
  amount: number
  date?: string
  description?: string
  // Omitted or 1 = simple purchase (no installment plan). 2-48 = deferred purchase.
  installmentCount?: number
}

export interface CardPaymentRequest {
  amount: number
  date?: string
  description?: string
}

export interface Installment {
  id: number
  number: number
  capitalAmount: number
  interestAmount: number
  dueDate: string
  status: InstallmentStatus
}
