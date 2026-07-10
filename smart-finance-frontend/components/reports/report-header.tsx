"use client"

import { Download } from "lucide-react"
import { Button } from "@/components/ui/button"
import { Select, SelectContent, SelectItem, SelectTrigger, SelectValue } from "@/components/ui/select"
import type { PeriodOption } from "@/lib/utils/period"

interface ReportHeaderProps {
  period: string
  onPeriodChange: (period: string) => void
  periodOptions: PeriodOption[]
  onExport: () => void
  isExporting: boolean
  isLoading: boolean
}

export function ReportHeader({
  period,
  onPeriodChange,
  periodOptions,
  onExport,
  isExporting,
  isLoading,
}: ReportHeaderProps) {
  return (
    <div className="flex flex-col sm:flex-row sm:items-center sm:justify-between gap-4">
      <div>
        <h1 className="text-2xl font-bold text-foreground">Reportes</h1>
        <p className="text-sm text-muted-foreground">
          Analiza tu comportamiento financiero
        </p>
      </div>
      <div className="flex items-center gap-3">
        <Select value={period} onValueChange={onPeriodChange}>
          <SelectTrigger className="w-[200px]">
            <SelectValue placeholder="Selecciona un periodo" />
          </SelectTrigger>
          <SelectContent>
            {periodOptions.map((option) => (
              <SelectItem key={option.value} value={option.value}>
                {option.label}
              </SelectItem>
            ))}
          </SelectContent>
        </Select>
        <Button
          className="gap-2 bg-primary text-primary-foreground hover:bg-primary/90"
          onClick={onExport}
          disabled={isExporting || isLoading}
        >
          <Download className="h-4 w-4" />
          {isExporting ? "Exportando..." : "Exportar CSV"}
        </Button>
      </div>
    </div>
  )
}
