"use client"

import { useState } from "react"
import axios from "axios"
import { formatDistanceToNow } from "date-fns"
import { es } from "date-fns/locale"
import { ArrowRight, Sparkles } from "lucide-react"
import Link from "next/link"
import { toast } from "sonner"
import { Button } from "@/components/ui/button"
import { Skeleton } from "@/components/ui/skeleton"
import { getApiErrorMessage } from "@/lib/api-client"
import { useGenerateInsight, useInsight } from "@/hooks/use-ai"

function formatRelativeTime(isoDate: string): string {
  try {
    return formatDistanceToNow(new Date(isoDate), { addSuffix: true, locale: es })
  } catch {
    return ""
  }
}

function formatInsightContent(content: string): string[] {
  return content
    .split("\n")
    .map((line) => line.trim())
    .filter((line) => line.length > 0)
    .map((line) => line.replace(/^[-*]\s*/, ""))
}

export function AiInsightsCard() {
  const { insight, isLoading, error } = useInsight()
  const { generateInsight, isLoading: isGenerating } = useGenerateInsight()
  const [needsProvider, setNeedsProvider] = useState(false)

  const handleGenerate = async () => {
    setNeedsProvider(false)
    try {
      await generateInsight()
      toast.success("Insight generado correctamente")
    } catch (err) {
      if (axios.isAxiosError(err) && err.response?.status === 422) {
        setNeedsProvider(true)
      }
      toast.error(getApiErrorMessage(err, "No fue posible generar el insight"))
    }
  }

  return (
    <div className="rounded-xl bg-card border border-border p-5">
      <div className="flex items-center justify-between mb-4">
        <div className="flex items-center gap-2">
          <div className="flex h-8 w-8 items-center justify-center rounded-lg bg-primary/10">
            <Sparkles className="h-5 w-5 text-primary" />
          </div>
          <div>
            <h3 className="text-lg font-semibold text-foreground">Insights financieros IA</h3>
            <p className="text-xs text-muted-foreground">Recomendaciones personalizadas del asistente</p>
          </div>
        </div>
        <Button size="sm" variant="outline" onClick={() => void handleGenerate()} disabled={isGenerating}>
          {isGenerating ? "Generando..." : "Generar insights"}
        </Button>
      </div>

      {isLoading ? (
        <div className="space-y-2">
          <Skeleton className="h-4 w-full" />
          <Skeleton className="h-4 w-5/6" />
          <Skeleton className="h-4 w-2/3" />
        </div>
      ) : error ? (
        <div className="rounded-lg border border-dashed border-border p-6 text-center text-sm text-muted-foreground">
          No fue posible cargar el ultimo insight.
        </div>
      ) : !insight ? (
        <div className="rounded-lg border border-dashed border-border p-6 text-center text-sm text-muted-foreground">
          Todavia no generaste ningun insight. Toca &quot;Generar insights&quot; para obtener recomendaciones
          basadas en tus finanzas reales.
        </div>
      ) : (
        <div className="space-y-3">
          <ul className="space-y-2">
            {formatInsightContent(insight.content).map((line, index) => (
              <li key={`insight-line-${index}`} className="flex gap-2 text-sm text-foreground/90 leading-relaxed">
                <span className="text-primary">•</span>
                <span>{line}</span>
              </li>
            ))}
          </ul>
          <p className="text-xs text-muted-foreground">
            Generado {formatRelativeTime(insight.createdAt)} con {insight.providerName} ({insight.model})
          </p>
        </div>
      )}

      {needsProvider && (
        <p className="mt-3 text-xs text-muted-foreground">
          Necesitas configurar un proveedor de IA para generar insights.{" "}
          <Link href="/configuracion" className="text-primary underline underline-offset-2">
            Configuralo aca
          </Link>
          .
        </p>
      )}

      <div className="mt-4 border-t border-border pt-3">
        <Link
          href="/asistente-ia"
          className="inline-flex items-center gap-1 text-xs font-medium text-primary hover:underline"
        >
          Ir al asistente IA
          <ArrowRight className="h-3 w-3" />
        </Link>
      </div>
    </div>
  )
}
