"use client"

import { Bot } from "lucide-react"

const suggestedQuestions = [
  "Como puedo reducir mis gastos mensuales?",
  "Cuanto deberia ahorrar cada mes?",
  "Cual es mi mayor gasto este mes?",
  "Como puedo pagar mis deudas mas rapido?",
]

interface ChatEmptyStateProps {
  onSelectQuestion: (question: string) => void
}

export function ChatEmptyState({ onSelectQuestion }: ChatEmptyStateProps) {
  return (
    <div className="flex h-full flex-col items-center justify-center gap-3 text-center">
      <div className="flex h-12 w-12 items-center justify-center rounded-full bg-gradient-to-br from-primary to-primary/60">
        <Bot className="h-6 w-6 text-primary-foreground" />
      </div>
      <p className="max-w-sm text-sm text-muted-foreground">
        Hola! Soy tu asistente financiero. Conozco tus ingresos, gastos, deudas y servicios
        recurrentes reales, asi que podes preguntarme lo que necesites sobre tus finanzas.
      </p>
      <div className="flex flex-wrap justify-center gap-2 pt-2">
        {suggestedQuestions.map((question) => (
          <button
            key={question}
            onClick={() => onSelectQuestion(question)}
            className="text-xs bg-muted/50 hover:bg-muted text-foreground px-3 py-1.5 rounded-full transition-colors border border-border hover:border-primary/30"
          >
            {question}
          </button>
        ))}
      </div>
    </div>
  )
}
