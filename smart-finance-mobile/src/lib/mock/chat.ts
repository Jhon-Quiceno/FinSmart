// Datos mock para el Asistente IA — reemplazar por POST /api/ai/chat (TODO Fase 0 backend).
// Forma replica ChatMessage de smart-finance-frontend/lib/types/ai.ts.

export interface MockChatMessage {
  id: number;
  role: 'USER' | 'ASSISTANT';
  content: string;
}

export const mockChatMessages: MockChatMessage[] = [
  { id: 1, role: 'ASSISTANT', content: '¡Hola! Soy tu asistente financiero de KoroFin. ¿En qué te ayudo hoy?' },
  { id: 2, role: 'USER', content: '¿Cuánto gasté en mercado este mes?' },
  { id: 3, role: 'ASSISTANT', content: 'Llevás $820.000,00 en la categoría Mercado durante agosto — un 26% de tu gasto total del mes.' },
  { id: 4, role: 'USER', content: '¿Voy bien con mi meta de ahorro?' },
  { id: 5, role: 'ASSISTANT', content: 'Sí — tu proyección de ahorro anual mejoró 12% respecto al mes pasado. Vas por buen camino.' },
];
