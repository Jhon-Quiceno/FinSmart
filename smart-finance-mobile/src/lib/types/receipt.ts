import type { CategoryType } from "@/lib/types/category"

export interface ReceiptExtraction {
  isReceipt: boolean
  description: string | null
  amount: number | null
  movementType: CategoryType | null
  categoryId: number | null
  categoryName: string | null
}
