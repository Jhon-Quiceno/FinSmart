export const CARD_FRANCHISES = ["VISA", "MASTERCARD", "AMEX", "DINERS"] as const

export type CardFranchise = (typeof CARD_FRANCHISES)[number]

export interface CreditCard {
  id: number
  name: string
  bank: string | null
  franchise: CardFranchise
  creditLimit: number
  monthlyRate: number
  cutoffDay: number
  paymentDueDay: number
  currentBalance: number
  availableCredit: number
  lastCutoffDate: string | null
  createdAt: string
  updatedAt: string
}

export interface CreditCardCreateRequest {
  name: string
  bank?: string
  franchise: CardFranchise
  creditLimit: number
  monthlyRate: number
  cutoffDay: number
  paymentDueDay: number
}

// Mirrors CreditCardUpdateRequest on the backend: franchise, creditLimit and currentBalance are
// deliberately excluded (fixed-at-creation identity fields / balance only moves via movements).
export interface CreditCardUpdateRequest {
  name: string
  bank?: string
  monthlyRate: number
  cutoffDay: number
  paymentDueDay: number
}
