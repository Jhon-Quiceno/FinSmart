"use client"

import { AlertTriangle } from "lucide-react"
import { cn } from "@/lib/utils"
import type { AnalysisRecommendation } from "@/lib/types/analysis"

interface AlertsPanelProps {
  recommendations: AnalysisRecommendation[]
}

export function AlertsPanel({ recommendations }: AlertsPanelProps) {
  const alerts = recommendations.filter((item) => item.type === "ALERT")

  return (
    <div className="rounded-xl bg-card border border-border p-5">
      <div className="flex items-center gap-2 mb-4">
        <AlertTriangle className="h-5 w-5 text-warning" />
        <h3 className="text-lg font-semibold text-foreground">Alertas Proximas</h3>
      </div>

      {alerts.length === 0 ? (
        <div className="rounded-lg border border-dashed border-border p-6 text-center text-sm text-muted-foreground">
          No hay alertas activas por el momento.
        </div>
      ) : (
        <div className="space-y-3">
          {alerts.map((alert, index) => (
            <div
              key={`${alert.categoryId ?? "general"}-${index}`}
              className="flex items-center gap-3 rounded-lg border border-destructive/30 bg-destructive/5 p-3 transition-smooth"
            >
              <div className="flex h-8 w-8 items-center justify-center rounded-lg shrink-0 bg-destructive/10 text-destructive">
                <AlertTriangle className="h-4 w-4" />
              </div>
              <div className="flex-1 min-w-0">
                <p className="text-sm font-medium text-foreground">{alert.message}</p>
              </div>
              <span
                className={cn(
                  "text-xs font-medium px-2 py-1 rounded-md shrink-0",
                  "bg-destructive/10 text-destructive",
                )}
              >
                {alert.severity}
              </span>
            </div>
          ))}
        </div>
      )}
    </div>
  )
}
