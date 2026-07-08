"use client"

import type { ReactNode } from "react"
import { motion, useReducedMotion } from "framer-motion"

const ENTRANCE_EASE = [0.22, 1, 0.36, 1] as const

interface FadeInProps {
  children: ReactNode
  delay?: number
  className?: string
}

/**
 * Fades and slides children into view (opacity 0->1, translateY 8px->0).
 * Renders children instantly with no motion when `prefers-reduced-motion` is set.
 */
export function FadeIn({ children, delay = 0, className }: FadeInProps) {
  const shouldReduceMotion = useReducedMotion()

  if (shouldReduceMotion) {
    return <div className={className}>{children}</div>
  }

  return (
    <motion.div
      className={className}
      initial={{ opacity: 0, y: 8 }}
      animate={{ opacity: 1, y: 0 }}
      transition={{ duration: 0.2, delay, ease: ENTRANCE_EASE }}
    >
      {children}
    </motion.div>
  )
}
