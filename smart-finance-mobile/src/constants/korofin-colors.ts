import { vars } from 'nativewind';

/**
 * Espejo en RGB (tripletas "R G B" separadas por espacio, no hex) de los tokens OKLCH de
 * docs/DESIGN.md (`:root` / `.dark` en smart-finance-frontend/app/globals.css). El formato
 * "R G B" en vez de "#rrggbb" es intencional: es el único que le permite a Tailwind construir
 * `rgb(var(--x) / <alpha-value>)` en tailwind.config.js y así soportar modificadores de
 * opacidad (`bg-primary/20`, `bg-card/50`, etc.) — con hex plano esos modificadores no
 * funcionan. Conversión hecha en build time con `culori` (formatHex(oklch(...))) y luego a
 * triplete decimal — ver DESIGN.md §2 para la fuente de verdad y la regla de conversión
 * ("nunca usar un solo valor para ambos temas").
 */
export const korofinThemes = {
  light: vars({
    '--color-background': '250 252 255',
    '--color-foreground': '20 24 31',
    '--color-card': '255 255 255',
    '--color-card-foreground': '20 24 31',
    '--color-popover': '255 255 255',
    '--color-popover-foreground': '20 24 31',
    '--color-primary': '39 153 54',
    '--color-primary-foreground': '248 254 248',
    '--color-secondary': '239 242 246',
    '--color-secondary-foreground': '37 41 48',
    '--color-muted': '239 242 246',
    '--color-muted-foreground': '94 100 108',
    '--color-accent': '231 235 242',
    '--color-accent-foreground': '27 31 39',
    '--color-destructive': '223 32 46',
    '--color-destructive-foreground': '255 249 248',
    '--color-success': '29 147 48',
    '--color-success-foreground': '248 254 248',
    '--color-warning': '212 142 0',
    '--color-warning-foreground': '35 25 10',
    '--color-border': '220 222 226',
    '--color-input': '226 229 233',
    '--color-ring': '39 153 54',
  }),
  dark: vars({
    '--color-background': '5 6 7',
    '--color-foreground': '235 239 245',
    '--color-card': '11 13 17',
    '--color-card-foreground': '235 239 245',
    '--color-popover': '8 9 12',
    '--color-popover-foreground': '235 239 245',
    '--color-primary': '87 203 96',
    '--color-primary-foreground': '6 13 6',
    '--color-secondary': '24 27 31',
    '--color-secondary-foreground': '212 216 222',
    '--color-muted': '20 22 26',
    '--color-muted-foreground': '140 143 149',
    '--color-accent': '29 34 41',
    '--color-accent-foreground': '235 239 245',
    '--color-destructive': '249 65 68',
    '--color-destructive-foreground': '255 246 245',
    '--color-success': '67 194 81',
    '--color-success-foreground': '6 13 6',
    '--color-warning': '242 166 24',
    '--color-warning-foreground': '23 16 8',
    '--color-border': '38 41 46',
    '--color-input': '20 22 26',
    '--color-ring': '87 203 96',
  }),
} as const;

export type KorofinColorScheme = keyof typeof korofinThemes;
