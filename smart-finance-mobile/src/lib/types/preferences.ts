import type { ThemePreference } from "@/components/korofin-theme-provider"

export const CURRENCIES = ["COP", "USD", "MXN", "ARS", "EUR"] as const
export type CurrencyCode = (typeof CURRENCIES)[number]

export const LANGUAGES = ["ES", "EN"] as const
export type LanguageCode = (typeof LANGUAGES)[number]

export const BACKEND_THEMES = ["SYSTEM", "LIGHT", "DARK"] as const
export type BackendTheme = (typeof BACKEND_THEMES)[number]

export interface UserPreferences {
  theme: BackendTheme
  currency: CurrencyCode
  language: LanguageCode
}

/** korofin-theme-provider usa minusculas ('system'/'light'/'dark'), el backend usa mayusculas. */
export function toBackendTheme(preference: ThemePreference): BackendTheme {
  return preference.toUpperCase() as BackendTheme
}

export function toLocalTheme(theme: BackendTheme): ThemePreference {
  return theme.toLowerCase() as ThemePreference
}
