"use client"

import type { ReactNode } from "react"
import { motion, useReducedMotion, type Variants } from "framer-motion"

const ENTRANCE_EASE = [0.22, 1, 0.36, 1] as const

const itemVariants: Variants = {
  hidden: { opacity: 0, y: 8 },
  show: { opacity: 1, y: 0, transition: { duration: 0.2, ease: ENTRANCE_EASE } },
}

interface StaggerContainerProps {
  children: ReactNode
  className?: string
  /**
   * Delay in seconds between each child's entrance. Keep lists short
   * (~8-10 items) so the total stagger delay stays under ~400ms.
   */
  staggerDelay?: number
}

/**
 * Orchestrates staggered entrance animations for `StaggerItem` children.
 * Falls back to a plain div (no stagger, no motion) when `prefers-reduced-motion` is set.
 */
export function StaggerContainer({ children, className, staggerDelay = 0.05 }: StaggerContainerProps) {
  const shouldReduceMotion = useReducedMotion()

  if (shouldReduceMotion) {
    return <div className={className}>{children}</div>
  }

  return (
    <motion.div
      className={className}
      initial="hidden"
      animate="show"
      variants={{
        hidden: {},
        show: {
          transition: {
            staggerChildren: staggerDelay,
          },
        },
      }}
    >
      {children}
    </motion.div>
  )
}

interface StaggerItemProps {
  children: ReactNode
  className?: string
}

/**
 * A single staggered entrance item. Must be rendered as a direct (or nested)
 * child of `StaggerContainer` to inherit its `hidden`/`show` variant timing.
 */
export function StaggerItem({ children, className }: StaggerItemProps) {
  const shouldReduceMotion = useReducedMotion()

  if (shouldReduceMotion) {
    return <div className={className}>{children}</div>
  }

  return (
    <motion.div className={className} variants={itemVariants}>
      {children}
    </motion.div>
  )
}
