import { Redirect } from 'expo-router';

import { useAuth } from '@/context/auth-context';

export default function Index() {
  const { status } = useAuth();
  if (status === 'bootstrapping') return null;
  return <Redirect href={status === 'authenticated' ? '/(tabs)' : '/login'} />;
}
