export const ALL_PERIODS_VALUE = "all"

export interface PeriodOption {
  value: string
  label: string
}

function toPeriodValue(date: Date): string {
  return `${date.getFullYear()}-${String(date.getMonth() + 1).padStart(2, "0")}`
}

function toPeriodLabel(date: Date): string {
  const label = date.toLocaleDateString("es-MX", { month: "long", year: "numeric" })
  return label.charAt(0).toUpperCase() + label.slice(1)
}

export function getCurrentPeriodValue(): string {
  return toPeriodValue(new Date())
}

/** "Todos los periodos" plus the current month and the `monthsBack` months before it. */
export function getRecentPeriodOptions(monthsBack = 11): PeriodOption[] {
  const now = new Date()
  const options: PeriodOption[] = [{ value: ALL_PERIODS_VALUE, label: "Todos los periodos" }]

  for (let i = 0; i <= monthsBack; i++) {
    const date = new Date(now.getFullYear(), now.getMonth() - i, 1)
    options.push({ value: toPeriodValue(date), label: toPeriodLabel(date) })
  }

  return options
}

function formatIsoDate(date: Date): string {
  return `${date.getFullYear()}-${String(date.getMonth() + 1).padStart(2, "0")}-${String(date.getDate()).padStart(2, "0")}`
}

/** Converts a "YYYY-MM" period (or ALL_PERIODS_VALUE) into inclusive from/to ISO date bounds. */
export function periodToDateRange(period: string): { from?: string; to?: string } {
  if (period === ALL_PERIODS_VALUE || !period) return {}

  const [year, month] = period.split("-").map(Number)
  if (!year || !month) return {}

  const from = new Date(year, month - 1, 1)
  const to = new Date(year, month, 0)

  return { from: formatIsoDate(from), to: formatIsoDate(to) }
}
