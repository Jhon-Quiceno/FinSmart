"use client"

import { Tv, Zap, Droplets, Wifi, Phone, Music, Cloud, MoreHorizontal, Calendar } from "lucide-react"
import { cn } from "@/lib/utils"

export interface Service {
  id: number
  name: string
  amount: number
  paymentDate: string
  daysUntilDue: number
  category: string
  status: "paid" | "pending" | "upcoming"
}

interface ServiceCardProps {
  service: Service
}

const categoryIcons: Record<string, React.ReactNode> = {
  Streaming: <Tv className="h-5 w-5" />,
  Luz: <Zap className="h-5 w-5" />,
  Agua: <Droplets className="h-5 w-5" />,
  Internet: <Wifi className="h-5 w-5" />,
  Telefono: <Phone className="h-5 w-5" />,
  Musica: <Music className="h-5 w-5" />,
  Cloud: <Cloud className="h-5 w-5" />,
  Otros: <MoreHorizontal className="h-5 w-5" />,
}

export function ServiceCard({ service }: ServiceCardProps) {
  const getStatusStyle = () => {
    if (service.status === "paid") {
      return {
        bg: "bg-success/10",
        text: "text-success",
        border: "border-success/30",
        label: "Pagado",
      }
    }
    if (service.daysUntilDue <= 3) {
      return {
        bg: "bg-destructive/10",
        text: "text-destructive",
        border: "border-destructive/30",
        label: "Urgente",
      }
    }
    if (service.daysUntilDue <= 7) {
      return {
        bg: "bg-warning/10",
        text: "text-warning",
        border: "border-warning/30",
        label: "Proximo",
      }
    }
    return {
      bg: "bg-primary/10",
      text: "text-primary",
      border: "border-primary/30",
      label: "Pendiente",
    }
  }

  const statusStyle = getStatusStyle()

  return (
    <div className={cn(
      "rounded-xl bg-card border p-5 transition-smooth hover:border-primary/30",
      service.status !== "paid" && service.daysUntilDue <= 3 && "border-destructive/30",
      service.status !== "paid" && service.daysUntilDue <= 7 && service.daysUntilDue > 3 && "border-warning/30",
      service.status === "paid" && "border-success/30",
      service.status !== "paid" && service.daysUntilDue > 7 && "border-border"
    )}>
      <div className="flex items-start justify-between mb-4">
        <div className="flex items-center gap-3">
          <div className={cn("flex h-12 w-12 items-center justify-center rounded-xl", statusStyle.bg, statusStyle.text)}>
            {categoryIcons[service.category] || categoryIcons.Otros}
          </div>
          <div>
            <h3 className="text-base font-semibold text-foreground">{service.name}</h3>
            <p className="text-xs text-muted-foreground">{service.category}</p>
          </div>
        </div>
        <span className={cn(
          "text-xs font-medium px-2.5 py-1 rounded-full border",
          statusStyle.bg,
          statusStyle.text,
          statusStyle.border
        )}>
          {statusStyle.label}
        </span>
      </div>

      <div className="flex items-center justify-between">
        <div>
          <p className="text-xs text-muted-foreground">Monto</p>
          <p className="text-lg font-bold text-foreground">
            ${service.amount.toLocaleString("es-MX", { minimumFractionDigits: 2 })}
          </p>
        </div>
        <div className="text-right">
          <p className="text-xs text-muted-foreground">Fecha de pago</p>
          <div className="flex items-center gap-1.5">
            <Calendar className="h-3.5 w-3.5 text-muted-foreground" />
            <p className="text-sm font-medium text-foreground">{service.paymentDate}</p>
          </div>
        </div>
      </div>

      {service.status !== "paid" && (
        <div className="mt-4 pt-4 border-t border-border">
          <div className="flex items-center justify-between">
            <span className={cn("text-sm font-medium", statusStyle.text)}>
              {service.daysUntilDue === 0 
                ? "Vence hoy" 
                : service.daysUntilDue === 1 
                  ? "Vence manana" 
                  : `Vence en ${service.daysUntilDue} dias`}
            </span>
            <button className="text-xs font-medium text-primary hover:text-primary/80 transition-smooth">
              Pagar ahora
            </button>
          </div>
        </div>
      )}
    </div>
  )
}
