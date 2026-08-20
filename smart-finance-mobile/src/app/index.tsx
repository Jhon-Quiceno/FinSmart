import { Link } from 'expo-router';
import { Text, View } from 'react-native';
import { SafeAreaView } from 'react-native-safe-area-context';

export default function HomeScreen() {
  return (
    <SafeAreaView className="flex-1 items-center justify-center gap-4 bg-background px-6">
      <Text className="text-center text-2xl font-bold text-foreground">KoroFin</Text>
      <View className="rounded-full bg-primary px-4 py-1.5">
        <Text className="text-sm font-semibold text-primary-foreground">
          NativeWind + SDK 54 OK
        </Text>
      </View>
      <Link
        href="/login"
        className="mt-2 rounded-lg bg-primary px-5 py-3 text-base font-medium text-primary-foreground"
      >
        Ver pantalla de login
      </Link>
    </SafeAreaView>
  );
}
