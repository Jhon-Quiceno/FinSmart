import { Tabs } from 'expo-router';
import { Bot, LayoutGrid, Receipt, Wallet } from 'lucide-react-native';
import { useColorScheme } from 'react-native';

const TAB_THEME = {
  light: { bar: 'rgb(255 255 255)', border: 'rgb(220 222 226)', active: 'rgb(39 153 54)', inactive: 'rgb(94 100 108)' },
  dark: { bar: 'rgb(11 13 17)', border: 'rgb(38 41 46)', active: 'rgb(87 203 96)', inactive: 'rgb(140 143 149)' },
};

export default function TabsLayout() {
  const colorScheme = useColorScheme();
  const theme = TAB_THEME[colorScheme === 'dark' ? 'dark' : 'light'];

  return (
    <Tabs
      screenOptions={{
        headerShown: false,
        tabBarActiveTintColor: theme.active,
        tabBarInactiveTintColor: theme.inactive,
        tabBarStyle: { backgroundColor: theme.bar, borderTopColor: theme.border },
      }}
    >
      <Tabs.Screen
        name="index"
        options={{ title: 'Inicio', tabBarIcon: ({ color, size }) => <LayoutGrid color={color} size={size} /> }}
      />
      <Tabs.Screen
        name="movimientos"
        options={{ title: 'Movimientos', tabBarIcon: ({ color, size }) => <Receipt color={color} size={size} /> }}
      />
      <Tabs.Screen
        name="deudas"
        options={{ title: 'Deudas', tabBarIcon: ({ color, size }) => <Wallet color={color} size={size} /> }}
      />
      <Tabs.Screen
        name="asistente"
        options={{ title: 'Asistente', tabBarIcon: ({ color, size }) => <Bot color={color} size={size} /> }}
      />
    </Tabs>
  );
}
