export const TRANSACTION_TYPES = ["INCOME", "EXPENSE"] as const

export type TransactionType = (typeof TRANSACTION_TYPES)[number]

export const RECOMMENDATION_TYPES = ["ALERT", "RECOMMENDATION", "INFO"] as const

export type RecommendationType = (typeof RECOMMENDATION_TYPES)[number]

export const RECOMMENDATION_SEVERITIES = ["HIGH", "MEDIUM", "LOW"] as const

export type RecommendationSeverity = (typeof RECOMMENDATION_SEVERITIES)[number]

export interface TopCategory {
  categoryId: number | null
  categoryName: string
  totalAmount: number
  percentage: number
}

export interface MonthlySeriesPoint {
  year: number
  month: number
  totalIncome: number
  totalExpense: number
}

export interface RecentTransaction {
  id: number
  type: TransactionType
  amount: number
  description: string
  date: string
  categoryName: string | null
}

export interface AnalysisSummary {
  periodYear: number
  periodMonth: number
  totalIncome: number
  totalExpense: number
  savings: number
  expenseRatio: number
  expenseAlert: boolean
  totalDebt: number
  debtRatio: number
  annualSavingsProjection: number
  topCategories: TopCategory[]
  monthlySeries: MonthlySeriesPoint[]
  recentTransactions: RecentTransaction[]
}

export interface AnalysisRecommendation {
  type: RecommendationType
  severity: RecommendationSeverity
  categoryId: number | null
  message: string
}

export interface Prediction {
  projectedExpense: number
  projectedBalance: number
  avgDailySpend: number
  daysRemaining: number
  recommendedDailyMax: number
  alert: boolean
}
