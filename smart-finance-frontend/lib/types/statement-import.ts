import type { PaymentMethodType } from "./expense"

export const MOVEMENT_TYPES = ["INCOME", "EXPENSE"] as const

export type MovementType = (typeof MOVEMENT_TYPES)[number]

export interface ImportPreviewRow {
  date: string
  description: string
  amount: number
  movementType: MovementType
  isDuplicate: boolean
  suggestedCategoryId: number | null
  suggestedCategoryName: string | null
}

export interface StatementPreviewResponse {
  rows: ImportPreviewRow[]
  totalRows: number
  duplicateRows: number
}

export interface ImportConfirmRow {
  movementType: MovementType
  amount: number
  date: string
  description?: string
  categoryId?: number | null
  paymentMethod?: PaymentMethodType | null
}

export interface StatementConfirmRequest {
  rows: ImportConfirmRow[]
}

export interface StatementImportResultResponse {
  createdCount: number
}
