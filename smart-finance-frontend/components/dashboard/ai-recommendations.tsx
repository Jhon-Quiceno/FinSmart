"use client"

import { Sparkles, Lightbulb, Info } from "lucide-react"
import { cn } from "@/lib/utils"
import type { AnalysisRecommendation } from "@/lib/types/analysis"

interface AIRecommendationsProps {
  recommendations: AnalysisRecommendation[]
}

export function AIRecommendations({ recommendations }: AIRecommendationsProps) {
  const items = recommendations.filter((item) => item.type === "RECOMMENDATION" || item.type === "INFO")

  return (
    <div className="rounded-xl bg-card border border-border p-5">
      <div className="flex items-center gap-2 mb-4">
        <div className="flex h-8 w-8 items-center justify-center rounded-lg bg-primary/10">
          <Sparkles className="h-5 w-5 text-primary" />
        </div>
        <div>
          <h3 className="text-lg font-semibold text-foreground">Recomendaciones IA</h3>
          <p className="text-xs text-muted-foreground">Basadas en tu comportamiento financiero</p>
        </div>
      </div>

      {items.length === 0 ? (
        <div className="rounded-lg border border-dashed border-border p-6 text-center text-sm text-muted-foreground">
          Todavia no hay recomendaciones para este periodo.
        </div>
      ) : (
        <div className="space-y-3">
          {items.map((rec, index) => (
            <div
              key={`${rec.categoryId ?? "general"}-${index}`}
              className={cn(
                "flex gap-3 rounded-lg border p-4 transition-smooth hover:border-primary/30",
                rec.type === "RECOMMENDATION" && "border-primary/20 bg-primary/5",
                rec.type === "INFO" && "border-success/20 bg-success/5",
              )}
            >
              <div
                className={cn(
                  "flex h-8 w-8 items-center justify-center rounded-lg shrink-0",
                  rec.type === "RECOMMENDATION" && "bg-primary/10 text-primary",
                  rec.type === "INFO" && "bg-success/10 text-success",
                )}
              >
                {rec.type === "RECOMMENDATION" ? <Lightbulb className="h-4 w-4" /> : <Info className="h-4 w-4" />}
              </div>
              <p className="text-sm text-foreground/90 leading-relaxed">{rec.message}</p>
            </div>
          ))}
        </div>
      )}
    </div>
  )
}
