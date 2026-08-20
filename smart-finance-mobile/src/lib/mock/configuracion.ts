// Datos mock para Configuración — reemplazar por GET /api/users/me y GET /api/ai/providers
// (TODO Fase 0 backend).

export const mockUser = {
  name: 'Jhon Quiceno',
  email: 'jpquiceno262@gmail.com',
};

export const mockNotificationPreferences = {
  paymentReminders: true,
  overspendAlerts: true,
  weeklySummary: false,
  inactivityReminders: true,
};

export interface MockAiProvider {
  name: string;
  active: boolean;
}

export const mockAiProviders: MockAiProvider[] = [
  { name: 'OpenAI GPT-4o mini', active: true },
  { name: 'Anthropic Claude', active: false },
];
