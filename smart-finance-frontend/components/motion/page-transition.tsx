"use client"

import type { ReactNode } from "react"
import { usePathname } from "next/navigation"
import { AnimatePresence, motion, useReducedMotion } from "framer-motion"

const ENTRANCE_EASE = [0.22, 1, 0.36, 1] as const

interface PageTransitionProps {
  children: ReactNode
}

/**
 * Cross-fades route content on navigation, keyed by the current pathname.
 * Renders children directly with no animation when `prefers-reduced-motion` is set.
 */
export function PageTransition({ children }: PageTransitionProps) {
  const pathname = usePathname()
  const shouldReduceMotion = useReducedMotion()

  if (shouldReduceMotion) {
    return <>{children}</>
  }

  return (
    <AnimatePresence mode="wait">
      <motion.div
        key={pathname}
        initial={{ opacity: 0, y: 8 }}
        animate={{ opacity: 1, y: 0 }}
        exit={{ opacity: 0, y: -8 }}
        transition={{ duration: 0.2, ease: ENTRANCE_EASE }}
      >
        {children}
      </motion.div>
    </AnimatePresence>
  )
}
