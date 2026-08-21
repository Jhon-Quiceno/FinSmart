export interface Debt {
  id: number
  name: string
  totalAmount: number
  remainingAmount: number
  interestRate: number | null
  dueDate: string | null
  createdAt: string
  updatedAt: string
}

export interface DebtCreateRequest {
  name: string
  totalAmount: number
  interestRate?: number | null
  dueDate?: string | null
}

export interface DebtUpdateRequest {
  name: string
  interestRate?: number | null
  dueDate?: string | null
}
