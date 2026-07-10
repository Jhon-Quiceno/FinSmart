"use client"

import { Bot, User } from "lucide-react"
import { cn } from "@/lib/utils"
import type { DisplayMessage } from "@/components/ai/chat-message"

interface ChatMessageBubbleProps {
  message: DisplayMessage
}

export function ChatMessageBubble({ message }: ChatMessageBubbleProps) {
  return (
    <div className={cn("flex gap-3", message.role === "USER" && "flex-row-reverse")}>
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
  )
}
