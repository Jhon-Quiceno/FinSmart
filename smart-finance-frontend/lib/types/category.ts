export const CATEGORY_TYPES = ["INCOME", "EXPENSE"] as const

export type CategoryType = (typeof CATEGORY_TYPES)[number]

export interface Category {
  id: number
  name: string
  type: CategoryType
  icon: string | null
  color: string | null
}

export type CategoryResponse = Category

export interface CategoryRequest {
  name: string
  type: CategoryType
  icon?: string | null
  color?: string | null
}
