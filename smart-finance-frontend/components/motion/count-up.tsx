"use client"

import { useEffect } from "react"
import { animate, motion, useMotionValue, useReducedMotion, useTransform } from "framer-motion"

const ENTRANCE_EASE = [0.22, 1, 0.36, 1] as const

const defaultFormat = (n: number) => Math.round(n).toLocaleString()

interface CountUpProps {
  value: number
  format?: (n: number) => string
  className?: string
  duration?: number
}

/**
 * Animates the display of an already-computed number from 0 to `value`.
 * Does not compute, fetch, or own the number itself — purely presentational.
 * Renders the formatted value instantly with no animation when
 * `prefers-reduced-motion` is set.
 */
export function CountUp({ value, format = defaultFormat, className, duration = 0.6 }: CountUpProps) {
  const shouldReduceMotion = useReducedMotion()
  const count = useMotionValue(0)
  const display = useTransform(count, (latest) => format(latest))

  useEffect(() => {
    if (shouldReduceMotion) {
      count.set(value)
      return
    }

    const controls = animate(count, value, {
      duration,
      ease: ENTRANCE_EASE,
    })

    return () => controls.stop()
  }, [value, duration, shouldReduceMotion, count])

  if (shouldReduceMotion) {
    return <span className={className}>{format(value)}</span>
  }

  return <motion.span className={className}>{display}</motion.span>
}
