"use client"

import type { ReactNode } from "react"
import { motion, useReducedMotion } from "framer-motion"

const INTERACTION_EASE = [0.4, 0, 0.2, 1] as const

interface TapScaleProps {
  children: ReactNode
  className?: string
  scale?: number
}

/**
 * Adds a subtle hover lift and tap scale-down micro-interaction.
 * Renders a plain div with no hover/tap animation when
 * `prefers-reduced-motion` is set.
 */
export function TapScale({ children, className, scale = 0.98 }: TapScaleProps) {
  const shouldReduceMotion = useReducedMotion()

  if (shouldReduceMotion) {
    return <div className={className}>{children}</div>
  }

  return (
    <motion.div
      className={className}
      whileHover={{ y: -2 }}
      whileTap={{ scale }}
      transition={{ duration: 0.15, ease: INTERACTION_EASE }}
    >
      {children}
    </motion.div>
  )
}
