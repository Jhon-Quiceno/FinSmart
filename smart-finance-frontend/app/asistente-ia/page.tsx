"use client"

import { useEffect, useMemo, useRef, useState } from "react"
import { toast } from "sonner"
import { AppLayout } from "@/components/layout/app-layout"
import { AiInsightsCard } from "@/components/dashboard/ai-insights-card"
import { Skeleton } from "@/components/ui/skeleton"
import { useChatHistory, useSendMessage } from "@/hooks/use-ai"
import { getApiErrorMessage } from "@/lib/api-client"
import { cn } from "@/lib/utils"
import type { AiMessageRole, ChatMessage } from "@/lib/types/ai"
import { Bot, Send, User } from "lucide-react"

const suggestedQuestions = [
  "Como puedo reducir mis gastos mensuales?",
  "Cuanto deberia ahorrar cada mes?",
  "Cual es mi mayor gasto este mes?",
  "Como puedo pagar mis deudas mas rapido?",
]

interface DisplayMessage {
  id: string
  role: AiMessageRole
  content: string
  providerName: string | null
  model: string | null
  createdAt: string
}

function toDisplayMessage(message: ChatMessage): DisplayMessage {
  return {
    id: String(message.id),
    role: message.role,
    content: message.content,
    providerName: message.providerName,
    model: message.model,
    createdAt: message.createdAt,
  }
}

export default function AsistenteIAPage() {
  const { history, isLoading: isLoadingHistory } = useChatHistory({ size: 50 })
  const { sendMessage, isLoading: isSending } = useSendMessage()

  const [inputValue, setInputValue] = useState("")
  const [pendingMessage, setPendingMessage] = useState<string | null>(null)
  const messagesEndRef = useRef<HTMLDivElement>(null)

  // The backend returns history most-recent-first (same convention as notifications);
  // the chat needs chronological order, oldest on top.
  const chronologicalMessages = useMemo(
    () => [...history.content].reverse().map(toDisplayMessage),
    [history.content],
  )

  const displayMessages: DisplayMessage[] = useMemo(() => {
    if (!pendingMessage) return chronologicalMessages

    return [
      ...chronologicalMessages,
      {
        id: "pending-user-message",
        role: "USER" as const,
        content: pendingMessage,
        providerName: null,
        model: null,
        createdAt: new Date().toISOString(),
      },
    ]
  }, [chronologicalMessages, pendingMessage])

  useEffect(() => {
    messagesEndRef.current?.scrollIntoView({ behavior: "smooth" })
  }, [displayMessages.length, isSending])

  const handleSendMessage = async (rawContent: string) => {
    const content = rawContent.trim()
    if (!content || isSending) return

    setPendingMessage(content)
    setInputValue("")

    try {
      await sendMessage(content)
      setPendingMessage(null)
    } catch (error) {
      setPendingMessage(null)
      // Keep the user's text in the input so they can retry without retyping.
      setInputValue(content)
      toast.error(getApiErrorMessage(error, "No fue posible enviar tu mensaje. Intenta de nuevo mas tarde."))
    }
  }

  const handleKeyDown = (event: React.KeyboardEvent<HTMLInputElement>) => {
    if (event.key === "Enter" && !event.shiftKey) {
      event.preventDefault()
      void handleSendMessage(inputValue)
    }
  }

  return (
    <AppLayout>
      <div className="space-y-6">
        {/* Header */}
        <div>
          <h1 className="text-2xl font-bold text-foreground">Asistente IA</h1>
          <p className="text-muted-foreground mt-1">
            Insights inteligentes y asistencia financiera personalizada
          </p>
        </div>

        <div className="grid gap-6 lg:grid-cols-5">
          {/* Insights Section */}
          <div className="lg:col-span-2 space-y-4">
            <AiInsightsCard />
          </div>

          {/* Chat Section */}
          <div className="lg:col-span-3 flex flex-col rounded-xl bg-card border border-border overflow-hidden h-[calc(100vh-200px)]">
            {/* Chat Header */}
            <div className="flex items-center gap-3 p-4 border-b border-border bg-muted/30">
              <div className="flex h-10 w-10 items-center justify-center rounded-full bg-gradient-to-br from-primary to-primary/60">
                <Bot className="h-5 w-5 text-primary-foreground" />
              </div>
              <div>
                <h3 className="font-semibold text-foreground">Asistente Financiero</h3>
                <div className="flex items-center gap-1.5">
                  <span className="h-2 w-2 rounded-full bg-success animate-pulse" />
                  <span className="text-xs text-muted-foreground">En linea - Listo para ayudarte</span>
                </div>
              </div>
            </div>

            {/* Messages */}
            <div className="flex-1 overflow-y-auto p-4 space-y-4">
              {isLoadingHistory ? (
                <div className="space-y-4">
                  <Skeleton className="h-16 w-2/3" />
                  <Skeleton className="ml-auto h-12 w-1/2" />
                  <Skeleton className="h-16 w-3/4" />
                </div>
              ) : displayMessages.length === 0 ? (
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
                        onClick={() => setInputValue(question)}
                        className="text-xs bg-muted/50 hover:bg-muted text-foreground px-3 py-1.5 rounded-full transition-colors border border-border hover:border-primary/30"
                      >
                        {question}
                      </button>
                    ))}
                  </div>
                </div>
              ) : (
                displayMessages.map((message) => (
                  <div
                    key={message.id}
                    className={cn("flex gap-3", message.role === "USER" && "flex-row-reverse")}
                  >
                    <div
                      className={cn(
                        "flex h-8 w-8 items-center justify-center rounded-full shrink-0",
                        message.role === "ASSISTANT" ? "bg-gradient-to-br from-primary to-primary/60" : "bg-muted",
                      )}
                    >
                      {message.role === "ASSISTANT" ? (
                        <Bot className="h-4 w-4 text-primary-foreground" />
                      ) : (
                        <User className="h-4 w-4 text-muted-foreground" />
                      )}
                    </div>
                    <div
                      className={cn(
                        "max-w-[80%] rounded-2xl px-4 py-3",
                        message.role === "ASSISTANT"
                          ? "bg-muted/50 rounded-tl-sm"
                          : "bg-primary text-primary-foreground rounded-tr-sm",
                      )}
                    >
                      <p
                        className={cn(
                          "text-sm whitespace-pre-line leading-relaxed",
                          message.role === "ASSISTANT" ? "text-foreground" : "text-primary-foreground",
                        )}
                      >
                        {message.content}
                      </p>
                      {message.role === "ASSISTANT" && message.providerName && (
                        <p className="text-[10px] mt-1 text-muted-foreground">
                          {message.providerName} · {message.model}
                        </p>
                      )}
                    </div>
                  </div>
                ))
              )}

              {isSending && (
                <div className="flex gap-3">
                  <div className="flex h-8 w-8 items-center justify-center rounded-full bg-gradient-to-br from-primary to-primary/60 shrink-0">
                    <Bot className="h-4 w-4 text-primary-foreground" />
                  </div>
                  <div className="bg-muted/50 rounded-2xl rounded-tl-sm px-4 py-3">
                    <div className="flex gap-1">
                      <span
                        className="h-2 w-2 rounded-full bg-muted-foreground/50 animate-bounce"
                        style={{ animationDelay: "0ms" }}
                      />
                      <span
                        className="h-2 w-2 rounded-full bg-muted-foreground/50 animate-bounce"
                        style={{ animationDelay: "150ms" }}
                      />
                      <span
                        className="h-2 w-2 rounded-full bg-muted-foreground/50 animate-bounce"
                        style={{ animationDelay: "300ms" }}
                      />
                    </div>
                  </div>
                </div>
              )}

              <div ref={messagesEndRef} />
            </div>

            {/* Input */}
            <div className="p-4 border-t border-border bg-muted/20">
              <div className="flex items-center gap-2">
                <input
                  type="text"
                  value={inputValue}
                  onChange={(event) => setInputValue(event.target.value)}
                  onKeyDown={handleKeyDown}
                  placeholder="Escribe tu pregunta financiera..."
                  className="flex-1 bg-background border border-border rounded-xl px-4 py-3 text-sm text-foreground placeholder:text-muted-foreground focus:outline-none focus:ring-2 focus:ring-primary/50 focus:border-primary transition-all"
                  disabled={isSending}
                />
                <button
                  onClick={() => void handleSendMessage(inputValue)}
                  disabled={!inputValue.trim() || isSending}
                  className="flex h-11 w-11 items-center justify-center rounded-xl bg-primary text-primary-foreground hover:bg-primary/90 disabled:opacity-50 disabled:cursor-not-allowed transition-all"
                >
                  <Send className="h-5 w-5" />
                </button>
              </div>
              <p className="text-[10px] text-muted-foreground text-center mt-2">
                El asistente utiliza IA para darte consejos basados en tus datos financieros
              </p>
            </div>
          </div>
        </div>
      </div>
    </AppLayout>
  )
}
