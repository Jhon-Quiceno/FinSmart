"use client"

import { Bot } from "lucide-react"

export function ChatHeader() {
  return (
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
  )
}
