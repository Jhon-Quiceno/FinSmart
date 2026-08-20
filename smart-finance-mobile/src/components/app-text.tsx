import { Text, TextInput, type TextInputProps, type TextProps } from 'react-native';

// `Text.defaultProps`/`TextInput.defaultProps` no funcionan en esta versión de RN — `Text` es
// un function component (ver node_modules/react-native/Libraries/Text/Text.js) y React no lee
// defaultProps de function components. Tampoco hay herencia de `font-family` vía NativeWind en
// runtime nativo (react-native-css-interop solo implementa esa herencia en su build web). Por
// eso la fuente global se aplica acá, mergeando el `style` explícito en cada `<Text>`/
// `<TextInput>` de la app — ver el retrofit de imports en cada pantalla (`AppText as Text`).
export function AppText({ style, ...rest }: TextProps) {
  return <Text style={[{ fontFamily: 'Inter' }, style]} {...rest} />;
}

export function AppTextInput({ style, ...rest }: TextInputProps) {
  return <TextInput style={[{ fontFamily: 'Inter' }, style]} {...rest} />;
}
