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
  },
  collectCoverageFrom: ['src/lib/**/*.ts', 'src/context/**/*.tsx'],
};
