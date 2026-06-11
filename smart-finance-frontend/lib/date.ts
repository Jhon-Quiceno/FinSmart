const DATE_INPUT_REGEX = /^\d{4}-\d{2}-\d{2}$/

export function formatDateInput(date: Date): string {
  const year = date.getFullYear()
  const month = String(date.getMonth() + 1).padStart(2, "0")
  const day = String(date.getDate()).padStart(2, "0")
  return `${year}-${month}-${day}`
}

export function getTodayDateInput(): string {
  return formatDateInput(new Date())
}

export function isValidDateInput(value: string): boolean {
  if (!DATE_INPUT_REGEX.test(value)) return false

  const [yearValue, monthValue, dayValue] = value.split("-")
  const year = Number(yearValue)
  const month = Number(monthValue)
  const day = Number(dayValue)
  const candidate = new Date(year, month - 1, day)

  return (
    candidate.getFullYear() === year &&
    candidate.getMonth() + 1 === month &&
    candidate.getDate() === day
  )
}

export function isFutureDateInput(value: string): boolean {
  return value > getTodayDateInput()
}
