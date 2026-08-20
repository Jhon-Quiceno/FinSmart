// react-native-css-interop@0.2.6 (la versión instalada, la más nueva que existe) tiene un bug
// en su parseBoxShadow: solo setea shadowColor/shadowRadius, nunca shadowOpacity (default 0,
// invisible en iOS) ni elevation (obligatorio en Android) — la clase `shadow-sm` de Tailwind
// nunca dibuja nada. Se usa este objeto vía `style` en su lugar, el único camino que funciona.
// Sin tipar como ViewStyle/TextStyle a propósito: son estructuralmente incompatibles en RN
// (userSelect difiere entre ambos) y este objeto se pasa tanto a `View` como a `TextInput`.
export const CARD_SHADOW = {
  shadowColor: '#000',
  shadowOffset: { width: 0, height: 1 },
  shadowOpacity: 0.06,
  shadowRadius: 2,
  elevation: 2,
} as const;
