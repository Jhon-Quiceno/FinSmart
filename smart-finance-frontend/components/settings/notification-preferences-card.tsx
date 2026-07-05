"use client"

import { Bell } from "lucide-react"
import { toast } from "sonner"
import { Skeleton } from "@/components/ui/skeleton"
import { Switch } from "@/components/ui/switch"
import { useNotificationPreferences, useUpdateNotificationPreferences } from "@/hooks/use-notifications"
import { getApiErrorMessage } from "@/lib/api-client"
import type { NotificationPreference } from "@/lib/types/notification"

const preferenceToggles: {
  key: keyof NotificationPreference
  label: string
  description: string
}[] = [
  {
    key: "paymentReminders",
    label: "Recordatorios de pago",
    description: "Avisos de servicios y deudas que vencen en los proximos dias",
  },
  {
    key: "overspendAlerts",
    label: "Alertas de sobregasto",
    description: "Te avisamos si tu gasto del mes supera el 80% de tu ingreso",
  },
  {
    key: "weeklySummary",
    label: "Resumen semanal",
    description: "Balance, mayor categoria de gasto y recomendaciones cada semana",
  },
  {
    key: "inactivityReminders",
    label: "Recordatorios de inactividad",
    description: "Un recordatorio si llevas varios dias sin registrar movimientos",
  },
]

export function NotificationPreferencesCard() {
  const { preferences, isLoading, error } = useNotificationPreferences()
  const { updatePreferences, isLoading: isUpdating } = useUpdateNotificationPreferences()

  const handleToggle = async (key: keyof NotificationPreference, checked: boolean) => {
    if (!preferences) return

    try {
      await updatePreferences({ ...preferences, [key]: checked })
      toast.success("Preferencias actualizadas")
    } catch (err) {
      toast.error(getApiErrorMessage(err, "No fue posible actualizar tus preferencias"))
    }
  }

  return (
    <div className="rounded-xl bg-card border border-border p-6">
      <div className="flex items-center gap-3 mb-6">
        <div className="flex h-10 w-10 items-center justify-center rounded-lg bg-warning/10">
          <Bell className="h-5 w-5 text-warning" />
        </div>
        <div>
          <h2 className="text-lg font-semibold text-foreground">Notificaciones</h2>
          <p className="text-sm text-muted-foreground">Configura tus alertas</p>
        </div>
      </div>

      {isLoading ? (
        <div className="space-y-3">
          <Skeleton className="h-16 w-full" />
          <Skeleton className="h-16 w-full" />
          <Skeleton className="h-16 w-full" />
        </div>
      ) : error || !preferences ? (
        <p className="text-sm text-muted-foreground">No fue posible cargar tus preferencias de notificacion.</p>
      ) : (
        <div className="space-y-4">
          {preferenceToggles.map((toggle) => (
            <div key={toggle.key} className="flex items-center justify-between p-4 rounded-lg bg-secondary/50">
              <div>
                <p className="text-sm font-medium text-foreground">{toggle.label}</p>
                <p className="text-xs text-muted-foreground">{toggle.description}</p>
              </div>
              <Switch
                checked={preferences[toggle.key]}
                disabled={isUpdating}
                onCheckedChange={(checked) => void handleToggle(toggle.key, checked)}
              />
            </div>
          ))}

          <div className="flex items-center justify-between p-4 rounded-lg bg-secondary/50">
            <div>
              <p className="text-sm font-medium text-foreground">Notificaciones por correo</p>
              <p className="text-xs text-muted-foreground">
                Ademas de las notificaciones dentro de la app, envianos una copia por correo.
              </p>
              <p className="text-xs text-muted-foreground mt-1">
                Requiere que el servidor tenga configurado el envio de correos; si no esta disponible, seguiras
                recibiendo solo notificaciones dentro de la app.
              </p>
            </div>
            <Switch
              checked={preferences.emailEnabled}
              disabled={isUpdating}
              onCheckedChange={(checked) => void handleToggle("emailEnabled", checked)}
            />
          </div>
        </div>
      )}
    </div>
  )
}
