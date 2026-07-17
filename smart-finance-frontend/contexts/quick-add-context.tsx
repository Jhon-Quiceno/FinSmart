"use client"

import { createContext, ReactNode, useCallback, useContext, useEffect, useState } from "react"

interface QuickAddContextType {
  isOpen: boolean
  open: () => void
  close: () => void
  toggle: () => void
}

const QuickAddContext = createContext<QuickAddContextType | undefined>(undefined)

/**
 * Global state for the quick-add dialog: tracks open/closed and wires the `Ctrl+K` / `Cmd+K`
 * shortcut at the document level so it works from any authenticated page, regardless of which
 * element currently has focus.
 */
export function QuickAddProvider({ children }: { children: ReactNode }) {
  const [isOpen, setIsOpen] = useState(false)

  const open = useCallback(() => setIsOpen(true), [])
  const close = useCallback(() => setIsOpen(false), [])
  const toggle = useCallback(() => setIsOpen((value) => !value), [])

  useEffect(() => {
    const handleKeyDown = (event: KeyboardEvent) => {
      const isShortcut = (event.metaKey || event.ctrlKey) && event.key.toLowerCase() === "k"
      if (!isShortcut) return

      event.preventDefault()
      setIsOpen(true)
    }

    document.addEventListener("keydown", handleKeyDown)
    return () => {
      document.removeEventListener("keydown", handleKeyDown)
    }
  }, [])

  return (
    <QuickAddContext.Provider value={{ isOpen, open, close, toggle }}>
      {children}
    </QuickAddContext.Provider>
  )
}

export function useQuickAdd() {
  const context = useContext(QuickAddContext)
  if (context === undefined) {
    throw new Error("useQuickAdd must be used within a QuickAddProvider")
  }

  return context
}
