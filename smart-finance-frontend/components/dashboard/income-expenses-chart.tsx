"use client"

import {
  AreaChart,
  Area,
  XAxis,
  YAxis,
  CartesianGrid,
  Tooltip,
  ResponsiveContainer,
  Legend,
} from "recharts"
import type { MonthlySeriesPoint } from "@/lib/types/analysis"

const MONTH_LABELS = [
  "Ene", "Feb", "Mar", "Abr", "May", "Jun", "Jul", "Ago", "Sep", "Oct", "Nov", "Dic",
]

interface IncomeExpensesChartProps {
  series: MonthlySeriesPoint[]
}

export function IncomeExpensesChart({ series }: IncomeExpensesChartProps) {
  const data = series.map((point) => ({
    month: MONTH_LABELS[point.month - 1] ?? String(point.month),
    ingresos: point.totalIncome,
    gastos: point.totalExpense,
  }))

  return (
    <div className="rounded-xl bg-card border border-border p-5">
      <div className="mb-4">
        <h3 className="text-lg font-semibold text-foreground">Ingresos vs Gastos</h3>
        <p className="text-sm text-muted-foreground">Comparativa de los ultimos 6 meses</p>
      </div>

      {data.length === 0 ? (
        <div className="flex h-[300px] items-center justify-center rounded-lg border border-dashed border-border text-sm text-muted-foreground">
          Todavia no hay suficientes movimientos para mostrar esta comparativa.
        </div>
      ) : (
      <div className="h-[300px]">
        <ResponsiveContainer width="100%" height="100%">
          <AreaChart data={data} margin={{ top: 10, right: 10, left: 0, bottom: 0 }}>
            <defs>
              <linearGradient id="colorIngresos" x1="0" y1="0" x2="0" y2="1">
                <stop offset="5%" stopColor="oklch(0.72 0.19 145)" stopOpacity={0.3} />
                <stop offset="95%" stopColor="oklch(0.72 0.19 145)" stopOpacity={0} />
              </linearGradient>
              <linearGradient id="colorGastos" x1="0" y1="0" x2="0" y2="1">
                <stop offset="5%" stopColor="oklch(0.65 0.22 25)" stopOpacity={0.3} />
                <stop offset="95%" stopColor="oklch(0.65 0.22 25)" stopOpacity={0} />
              </linearGradient>
            </defs>
            <CartesianGrid strokeDasharray="3 3" stroke="oklch(0.28 0.01 260)" />
            <XAxis 
              dataKey="month" 
              stroke="oklch(0.65 0.01 260)" 
              fontSize={12}
              tickLine={false}
              axisLine={false}
            />
            <YAxis 
              stroke="oklch(0.65 0.01 260)" 
              fontSize={12}
              tickLine={false}
              axisLine={false}
              tickFormatter={(value) => `$${(value / 1000)}k`}
            />
            <Tooltip
              contentStyle={{
                backgroundColor: "oklch(0.16 0.008 260)",
                border: "1px solid oklch(0.28 0.01 260)",
                borderRadius: "8px",
                color: "oklch(0.95 0.01 260)",
              }}
              labelStyle={{ color: "oklch(0.65 0.01 260)" }}
              formatter={(value: number) => [`$${value.toLocaleString('es-MX')}`, '']}
            />
            <Legend 
              wrapperStyle={{ paddingTop: '20px' }}
              formatter={(value) => <span style={{ color: 'oklch(0.88 0.01 260)' }}>{value === 'ingresos' ? 'Ingresos' : 'Gastos'}</span>}
            />
            <Area
              type="monotone"
              dataKey="ingresos"
              stroke="oklch(0.72 0.19 145)"
              strokeWidth={2}
              fillOpacity={1}
              fill="url(#colorIngresos)"
            />
            <Area
              type="monotone"
              dataKey="gastos"
              stroke="oklch(0.65 0.22 25)"
              strokeWidth={2}
              fillOpacity={1}
              fill="url(#colorGastos)"
            />
          </AreaChart>
        </ResponsiveContainer>
      </div>
      )}
    </div>
  )
}
