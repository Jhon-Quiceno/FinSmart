"use client"

import { Sparkles, TrendingDown, PiggyBank, AlertCircle, Lightbulb } from "lucide-react"
import { cn } from "@/lib/utils"

interface Recommendation {
  id: number
  message: string
  type: "warning" | "tip" | "insight" | "action"
  icon: React.ReactNode
}

const recommendations: Recommendation[] = [
  {
    id: 1,
    message: "Estas gastando un 25% mas en alimentacion que el mes pasado. Considera revisar tus habitos.",
    type: "warning",
    icon: <AlertCircle className="h-4 w-4" />,
  },
  {
    id: 2,
    message: "Si reduces $500 en entretenimiento, podrias alcanzar tu meta de ahorro este mes.",
    type: "tip",
    icon: <Lightbulb className="h-4 w-4" />,
  },
  {
    id: 3,
    message: "Tu ratio de ahorro actual es del 15%. Esta por encima del promedio recomendado.",
    type: "insight",
    icon: <PiggyBank className="h-4 w-4" />,
  },
  {
    id: 4,
    message: "Detectamos un gasto recurrente de $299 en una suscripcion que no has usado en 2 meses.",
    type: "action",
    icon: <TrendingDown className="h-4 w-4" />,
  },
]

export function AIRecommendations() {
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

      <div className="space-y-3">
        {recommendations.map((rec) => (
          <div
            key={rec.id}
            className={cn(
              "flex gap-3 rounded-lg border p-4 transition-smooth hover:border-primary/30",
              rec.type === "warning" && "border-warning/20 bg-warning/5",
              rec.type === "tip" && "border-primary/20 bg-primary/5",
              rec.type === "insight" && "border-success/20 bg-success/5",
              rec.type === "action" && "border-destructive/20 bg-destructive/5"
            )}
          >
            <div
              className={cn(
                "flex h-8 w-8 items-center justify-center rounded-lg shrink-0",
                rec.type === "warning" && "bg-warning/10 text-warning",
                rec.type === "tip" && "bg-primary/10 text-primary",
                rec.type === "insight" && "bg-success/10 text-success",
                rec.type === "action" && "bg-destructive/10 text-destructive"
              )}
            >
              {rec.icon}
            </div>
            <p className="text-sm text-foreground/90 leading-relaxed">{rec.message}</p>
          </div>
        ))}
      </div>

      <button className="mt-4 w-full rounded-lg border border-primary/30 bg-primary/5 py-2.5 text-sm font-medium text-primary hover:bg-primary/10 transition-smooth">
        Ver mas recomendaciones
      </button>
    </div>
  )
}
