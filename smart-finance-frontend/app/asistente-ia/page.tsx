"use client"

import { useEffect, useMemo, useRef, useState } from "react"
import axios from "axios"
import { toast } from "sonner"
import { AppLayout } from "@/components/layout/app-layout"
import { AiInsightsCard } from "@/components/dashboard/ai-insights-card"
import { ChatHeader } from "@/components/ai/chat-header"
import { ChatInput } from "@/components/ai/chat-input"
import { ChatMessageList } from "@/components/ai/chat-message-list"
import { toDisplayMessage } from "@/components/ai/chat-message"
import type { DisplayMessage } from "@/components/ai/chat-message"
import { invalidateUsageCache, useAiUsage, useChatHistory, useSendMessage } from "@/hooks/use-ai"
import { getApiErrorMessage } from "@/lib/api-client"

export default function AsistenteIAPage() {
  const { history, isLoading: isLoadingHistory } = useChatHistory({ size: 50 })
  const { sendMessage, isLoading: isSending } = useSendMessage()
  const { usage } = useAiUsage()

  const [inputValue, setInputValue] = useState("")
  const [pendingMessage, setPendingMessage] = useState<string | null>(null)
  const [quotaMessage, setQuotaMessage] = useState<string | null>(null)
  const messagesEndRef = useRef<HTMLDivElement>(null)

  const quotaExhausted = usage !== null && usage.remaining <= 0

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
    if (!content || isSending || quotaExhausted) return

    setQuotaMessage(null)
    setPendingMessage(content)
    setInputValue("")

    try {
      await sendMessage(content)
      setPendingMessage(null)
    } catch (error) {
      setPendingMessage(null)
      // Keep the user's text in the input so they can retry without retyping.
      setInputValue(content)

      if (axios.isAxiosError(error) && error.response?.status === 429) {
        setQuotaMessage(
          getApiErrorMessage(error, "Alcanzaste el límite de mensajes de IA este mes. Intenta de nuevo el próximo mes."),
        )
        // Refresh the usage cache from the server so `quotaExhausted` (and therefore the
        // disabled input) reflects the real remaining count immediately, instead of waiting
        // for the next unrelated refetch.
        invalidateUsageCache()
        return
      }

      toast.error(getApiErrorMessage(error, "No fue posible enviar tu mensaje. Intenta de nuevo más tarde."))
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
            <ChatHeader />

            <ChatMessageList
              isLoadingHistory={isLoadingHistory}
              messages={displayMessages}
              isSending={isSending}
              onSelectQuestion={setInputValue}
              messagesEndRef={messagesEndRef}
            />

            <ChatInput
              value={inputValue}
              onChange={setInputValue}
              onKeyDown={handleKeyDown}
              onSend={() => void handleSendMessage(inputValue)}
              isSending={isSending}
              quotaExhausted={quotaExhausted}
              quotaMessage={quotaMessage}
              usage={usage}
            />
          </div>
        </div>
      </div>
    </AppLayout>
  )
}
