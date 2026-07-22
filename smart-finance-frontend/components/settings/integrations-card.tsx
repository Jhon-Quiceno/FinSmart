"use client"

import { useEffect, useState } from "react"
import { CheckCircle2, Send } from "lucide-react"
import { Badge } from "@/components/ui/badge"
import { Button } from "@/components/ui/button"
import { Skeleton } from "@/components/ui/skeleton"
import { toastApiError } from "@/lib/api-client"
import { generateTelegramLinkCode, getTelegramLinkStatus } from "@/lib/services/telegram-integration.service"

export function IntegrationsCard() {
  const [isCheckingStatus, setIsCheckingStatus] = useState(true)
  const [isLinked, setIsLinked] = useState(false)
  const [isGeneratingCode, setIsGeneratingCode] = useState(false)
  const [code, setCode] = useState<string | null>(null)

  useEffect(() => {
    let cancelled = false

    getTelegramLinkStatus()
      .then((status) => {
        if (!cancelled) setIsLinked(status.linked)
      })
      .catch((error) => {
        if (!cancelled) toastApiError(error, "No fue posible consultar el estado de Telegram")
      })
      .finally(() => {
        if (!cancelled) setIsCheckingStatus(false)
      })

    return () => {
      cancelled = true
    }
  }, [])

  const handleGenerateCode = async () => {
    setIsGeneratingCode(true)
    try {
      const response = await generateTelegramLinkCode()
      setCode(response.code)
    } catch (error) {
      toastApiError(error, "No fue posible generar el codigo de vinculacion")
    } finally {
      setIsGeneratingCode(false)
    }
  }

  return (
    <div className="rounded-xl bg-card border border-border p-6">
      <div className="flex items-center gap-3 mb-6">
        <div className="flex h-10 w-10 items-center justify-center rounded-lg bg-primary/10">
          <Send className="h-5 w-5 text-primary" />
        </div>
        <div>
          <h2 className="text-lg font-semibold text-foreground">Integraciones</h2>
          <p className="text-sm text-muted-foreground">Conecta KoroFin con otras aplicaciones</p>
        </div>
      </div>

      <div className="space-y-3">
        <div className="flex items-center gap-2">
          <h3 className="text-sm font-medium text-foreground">Telegram</h3>
          {!isCheckingStatus && isLinked && (
            <Badge variant="secondary" className="gap-1">
              <CheckCircle2 className="h-3 w-3" />
              Conectado
            </Badge>
          )}
        </div>
        <p className="text-sm text-muted-foreground">
          Registra gastos e ingresos mandandole un mensaje al bot (ej. &quot;Uber 15000&quot;) o una
          foto del recibo. Tambien podes preguntarle cosas como &quot;cuanto gaste en comida este
          mes&quot;.
        </p>

        {isCheckingStatus ? (
          <Skeleton className="h-9 w-40" />
        ) : isLinked ? (
          <p className="text-sm text-muted-foreground">
            Tu cuenta ya esta vinculada a un chat de Telegram. Si necesitas vincular un chat distinto,
            escribinos desde ese chat cualquier mensaje y seguí las instrucciones que te da el bot.
          </p>
        ) : (
          <>
            {code && (
              <div className="rounded-lg border border-border bg-secondary p-4 space-y-2">
                <p className="text-center font-mono text-2xl tracking-widest text-foreground">{code}</p>
                <p className="text-center text-xs text-muted-foreground">
                  El codigo vence en 10 minutos y es de un solo uso.
                </p>
                <p className="text-sm text-foreground">
                  Envia /start {code} al bot de KoroFin en Telegram.
                </p>
              </div>
            )}

            <Button variant="outline" disabled={isGeneratingCode} onClick={() => void handleGenerateCode()}>
              {isGeneratingCode ? "Generando..." : code ? "Generar nuevo codigo" : "Vincular Telegram"}
            </Button>
          </>
        )}
      </div>
    </div>
  )
}
