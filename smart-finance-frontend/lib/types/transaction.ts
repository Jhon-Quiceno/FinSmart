export interface PaginatedResponse<T> {
  content: T[]
  number: number
  size: number
  totalElements: number
  totalPages: number
}

export type TransactionType = "INCOME" | "EXPENSE" | "TRANSFER"

export type PaymentMethodType =
  | "CASH"
  | "DEBIT_CARD"
  | "CREDIT_CARD"
  | "TRANSFER"
  | "DIGITAL_WALLET"

export interface Transaction {
  id: number
  accountId: number | null
  accountName: string | null
  categoryId: number | null
  categoryName: string | null
  type: TransactionType
  amount: number
  description: string | null
  transactionDate: string
  paymentMethod: PaymentMethodType | null
  notes: string | null
  createdAt: string
  updatedAt: string
}

export type TransactionResponse = Transaction

export interface TransactionRequest {
  accountId?: number | null
  categoryId?: number | null
  type: TransactionType
  amount: number
  description?: string
  transactionDate: string
  paymentMethod?: PaymentMethodType | null
  notes?: string | null
}

export interface TransactionFilters {
  page?: number
  size?: number
  type?: TransactionType
  month?: number
  year?: number
  category?: number
  account?: number
  from?: string
  to?: string
}
