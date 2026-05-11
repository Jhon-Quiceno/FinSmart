"use client"

import { AlertTriangle, Calendar, CreditCard, Zap } from "lucide-react"
import { cn } from "@/lib/utils"

interface Alert {
  id: number
  title: string
  description: string
  type: "warning" | "danger" | "info"
  dueDate?: string
  icon: React.ReactNode
}

const alerts: Alert[] = [
  {
    id: 1,
    title: "Pago de Netflix",
    description: "Vence en 3 dias",
    type: "warning",
    dueDate: "25 Mar",
    icon: <Calendar className="h-4 w-4" />,
  },
  {
    id: 2,
    title: "Tarjeta de Credito",
    description: "Pago minimo pendiente",
    type: "danger",
    dueDate: "20 Mar",
    icon: <CreditCard className="h-4 w-4" />,
  },
  {
    id: 3,
    title: "Servicio de Luz",
    description: "Vence en 5 dias",
    type: "warning",
    dueDate: "28 Mar",
    icon: <Zap className="h-4 w-4" />,
  },
  {
    id: 4,
    title: "Prestamo Personal",
    description: "Cuota mensual proxima",
    type: "info",
    dueDate: "01 Abr",
    icon: <CreditCard className="h-4 w-4" />,
  },
]

export function AlertsPanel() {
  return (
    <div className="rounded-xl bg-card border border-border p-5">
      <div className="flex items-center gap-2 mb-4">
        <AlertTriangle className="h-5 w-5 text-warning" />
        <h3 className="text-lg font-semibold text-foreground">Alertas Proximas</h3>
      </div>

      <div className="space-y-3">
        {alerts.map((alert) => (
          <div
            key={alert.id}
            className={cn(
              "flex items-center gap-3 rounded-lg border p-3 transition-smooth",
              alert.type === "danger" && "border-destructive/30 bg-destructive/5",
              alert.type === "warning" && "border-warning/30 bg-warning/5",
              alert.type === "info" && "border-primary/30 bg-primary/5"
            )}
          >
            <div
              className={cn(
                "flex h-8 w-8 items-center justify-center rounded-lg shrink-0",
                alert.type === "danger" && "bg-destructive/10 text-destructive",
                alert.type === "warning" && "bg-warning/10 text-warning",
                alert.type === "info" && "bg-primary/10 text-primary"
              )}
            >
              {alert.icon}
            </div>
            <div className="flex-1 min-w-0">
              <p className="text-sm font-medium text-foreground truncate">{alert.title}</p>
              <p className="text-xs text-muted-foreground">{alert.description}</p>
            </div>
            {alert.dueDate && (
              <span
                className={cn(
                  "text-xs font-medium px-2 py-1 rounded-md shrink-0",
                  alert.type === "danger" && "bg-destructive/10 text-destructive",
                  alert.type === "warning" && "bg-warning/10 text-warning",
                  alert.type === "info" && "bg-primary/10 text-primary"
                )}
              >
                {alert.dueDate}
              </span>
            )}
          </div>
        ))}
      </div>
    </div>
  )
}
