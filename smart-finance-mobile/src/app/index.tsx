import { Redirect } from 'expo-router';

// TODO(Fase 0 backend): una vez exista sesión real, decidir acá entre /login y /(tabs)
// según haya o no un refreshToken válido en expo-secure-store.
export default function Index() {
  return <Redirect href="/login" />;
}
