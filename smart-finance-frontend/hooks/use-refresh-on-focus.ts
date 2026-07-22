"use client"

import { useEffect, useRef } from "react"

// Fallback poll while the tab stays visible, in case focus/visibility events are missed
// (e.g. the user switches to Telegram inside the same OS window manager without a focus blur).
const BACKGROUND_REFRESH_INTERVAL_MS = 30_000

/**
 * Runs `onRefresh` whenever the browser tab regains visibility or focus, plus on a short interval
 * while it stays visible. Use it in data-fetching hooks so views pick up changes made from an
 * external channel (e.g. registering an expense via the Telegram bot) without a manual reload.
 *
 * `onRefresh` is read through a ref so callers can pass an inline, non-memoized callback without
 * causing the listeners below to be torn down and re-attached on every render.
 */
export function useRefreshOnFocus(onRefresh: () => void) {
  const onRefreshRef = useRef(onRefresh)

  useEffect(() => {
    onRefreshRef.current = onRefresh
  }, [onRefresh])

  useEffect(() => {
    const handleVisibilityChange = () => {
      if (document.visibilityState === "visible") {
        onRefreshRef.current()
      }
    }

    const handleFocus = () => {
      onRefreshRef.current()
    }

    document.addEventListener("visibilitychange", handleVisibilityChange)
    window.addEventListener("focus", handleFocus)

    const intervalId = setInterval(() => {
      if (document.visibilityState === "visible") {
        onRefreshRef.current()
      }
    }, BACKGROUND_REFRESH_INTERVAL_MS)

    return () => {
      document.removeEventListener("visibilitychange", handleVisibilityChange)
      window.removeEventListener("focus", handleFocus)
      clearInterval(intervalId)
    }
  }, [])
}
