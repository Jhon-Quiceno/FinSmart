"use client"

import { Zap } from "lucide-react"
import { Button } from "@/components/ui/button"
import { useQuickAdd } from "@/contexts/quick-add-context"

/** Floating action button, always visible on authenticated pages, that opens the quick-add dialog. */
export function QuickAddFab() {
  const { open } = useQuickAdd()

  return (
    <Button
      type="button"
      size="icon"
      className="fixed bottom-6 right-6 z-50 size-14 rounded-full shadow-lg"
      onClick={open}
      aria-label="Agregar gasto rapido"
      title="Agregar gasto rapido (Ctrl+K)"
    >
      <Zap className="size-6" />
    </Button>
  )
}
