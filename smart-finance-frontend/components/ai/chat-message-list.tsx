"use client"

import type { RefObject } from "react"
import { Bot } from "lucide-react"
import { Skeleton } from "@/components/ui/skeleton"
import { ChatEmptyState } from "@/components/ai/chat-empty-state"
import { ChatMessageBubble } from "@/components/ai/chat-message-bubble"
import type { DisplayMessage } from "@/components/ai/chat-message"

interface ChatMessageListProps {
  isLoadingHistory: boolean
  messages: DisplayMessage[]
  isSending: boolean
  onSelectQuestion: (question: string) => void
  messagesEndRef: RefObject<HTMLDivElement | null>
}

export function ChatMessageList({
  isLoadingHistory,
  messages,
  isSending,
  onSelectQuestion,
  messagesEndRef,
}: ChatMessageListProps) {
  return (
    <div className="flex-1 overflow-y-auto p-4 space-y-4">
      {isLoadingHistory ? (
        <div className="space-y-4">
          <Skeleton className="h-16 w-2/3" />
          <Skeleton className="ml-auto h-12 w-1/2" />
          <Skeleton className="h-16 w-3/4" />
        </div>
      ) : messages.length === 0 ? (
        <ChatEmptyState onSelectQuestion={onSelectQuestion} />
      ) : (
        messages.map((message) => <ChatMessageBubble key={message.id} message={message} />)
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
  )
}
