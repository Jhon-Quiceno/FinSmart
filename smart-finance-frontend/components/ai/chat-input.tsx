"use client"

import { format } from "date-fns"
import { es } from "date-fns/locale"
import { Send } from "lucide-react"
import type { AiUsage } from "@/lib/types/ai"

function formatResetDate(isoDate: string): string {
  try {
    return format(new Date(isoDate), "d 'de' MMMM 'de' yyyy", { locale: es })
  } catch {
    return ""
  }
}

interface ChatInputProps {
  value: string
  onChange: (value: string) => void
  onKeyDown: (event: React.KeyboardEvent<HTMLInputElement>) => void
  onSend: () => void
  isSending: boolean
  quotaExhausted: boolean
  quotaMessage: string | null
  usage: AiUsage | null
}

export function ChatInput({
  value,
  onChange,
  onKeyDown,
  onSend,
  isSending,
  quotaExhausted,
  quotaMessage,
  usage,
}: ChatInputProps) {
  const quotaNotice =
    quotaMessage ??
    (quotaExhausted && usage
      ? `Alcanzaste el límite de ${usage.limit} mensajes de IA este mes. Tu límite se reinicia el ${formatResetDate(usage.resetsAt)}.`
      : null)

  return (
    <div className="p-4 border-t border-border bg-muted/20">
      {quotaNotice && (
        <div className="mb-3 rounded-lg border border-warning/30 bg-warning/10 px-3 py-2 text-xs text-warning">
          {quotaNotice}
        </div>
      )}
      <div className="flex items-center gap-2">
        <input
          type="text"
          value={value}
          onChange={(event) => onChange(event.target.value)}
          onKeyDown={onKeyDown}
          placeholder={
            quotaExhausted ? "Alcanzaste tu límite mensual de mensajes" : "Escribe tu pregunta financiera..."
          }
          className="flex-1 bg-background border border-border rounded-xl px-4 py-3 text-sm text-foreground placeholder:text-muted-foreground focus:outline-none focus:ring-2 focus:ring-primary/50 focus:border-primary transition-all disabled:opacity-60"
          disabled={isSending || quotaExhausted}
        />
        <button
          onClick={onSend}
          disabled={!value.trim() || isSending || quotaExhausted}
          className="flex h-11 w-11 items-center justify-center rounded-xl bg-primary text-primary-foreground hover:bg-primary/90 disabled:opacity-50 disabled:cursor-not-allowed transition-all"
        >
          <Send className="h-5 w-5" />
        </button>
      </div>
      <div className="flex items-center justify-between gap-2 mt-2">
        <p className="text-[10px] text-muted-foreground">
          El asistente utiliza IA para darte consejos basados en tus datos financieros
        </p>
        {usage && (
          <p className="text-[10px] text-muted-foreground shrink-0">
            {usage.used}/{usage.limit} mensajes este mes
          </p>
        )}
      </div>
    </div>
  )
}
