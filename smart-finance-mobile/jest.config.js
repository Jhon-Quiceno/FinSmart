module.exports = {
  preset: 'jest-expo',
  setupFilesAfterEnv: ['<rootDir>/jest.setup.ts'],
  // jest-expo's default transformIgnorePatterns doesn't cover nativewind/react-native-css-interop —
  // without adding them here, any test that renders a NativeWind-styled component fails at parse time.
  transformIgnorePatterns: [
    'node_modules/(?!((jest-)?react-native|@react-native(-community)?|expo(nent)?|@expo(nent)?/.*|react-navigation|@react-navigation/.*|nativewind|react-native-css-interop|@tanstack/.*|lucide-react-native))',
  ],
  // Jest doesn't read tsconfig `paths` on its own.
  moduleNameMapper: {
    '^@/(.*)$': '<rootDir>/src/$1',
    // lucide-react-native's "react-native"/"import" export condition points at an ESM .mjs
    // build; jest's default transform globs don't cover .mjs, so any test that renders a
    // component importing an icon fails at parse time ("Unexpected token 'export'") regardless
    // of transformIgnorePatterns. Force Jest to resolve the CJS build instead (Metro/the real app
    // bundle is unaffected — this mapping only applies inside Jest).
    '^lucide-react-native$': '<rootDir>/node_modules/lucide-react-native/dist/cjs/lucide-react-native.js',
  },
  collectCoverageFrom: ['src/lib/**/*.ts', 'src/context/**/*.tsx'],
};
