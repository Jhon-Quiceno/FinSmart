"use client"

import { useState } from "react"
import { PieChart, Pie, Cell, ResponsiveContainer } from "recharts"
import type { TopCategory } from "@/lib/types/analysis"

const CATEGORY_COLORS = [
  "oklch(0.72 0.19 145)",
  "oklch(0.65 0.22 25)",
  "oklch(0.78 0.16 75)",
  "oklch(0.65 0.18 260)",
  "oklch(0.70 0.15 200)",
]

interface ExpensesByCategoryChartProps {
  topCategories: TopCategory[]
}

export function ExpensesByCategoryChart({ topCategories }: ExpensesByCategoryChartProps) {
  const [activeIndex, setActiveIndex] = useState<number | null>(null)

  const data = topCategories.map((category, index) => ({
    name: category.categoryName,
    value: category.totalAmount,
    percentage: category.percentage * 100,
    color: CATEGORY_COLORS[index % CATEGORY_COLORS.length],
  }))

  const activeEntry = activeIndex !== null ? data[activeIndex] : null

  return (
    <div className="rounded-xl bg-card border border-border p-5">
      <div className="mb-4">
        <h3 className="text-lg font-semibold text-foreground">Gastos por Categoria</h3>
        <p className="text-sm text-muted-foreground">Distribucion del mes actual</p>
      </div>

      {data.length === 0 ? (
        <div className="flex h-[280px] items-center justify-center rounded-lg border border-dashed border-border text-sm text-muted-foreground">
          Todavia no registraste gastos en este periodo.
        </div>
      ) : (
        <>
          {/*
            Recharts' built-in <Tooltip> never activates on Pie hover under
            React 19 (confirmed via manual repro: the pointer lands on the
            correct <path>, but the tooltip stays visibility:hidden with an
            empty payload). Driving hover state manually via Pie's own
            onMouseEnter/onMouseLeave and rendering the value in the donut's
            empty center avoids depending on that broken auto-tooltip.
          */}
          <div className="relative h-[280px]">
            <ResponsiveContainer width="100%" height="100%">
              <PieChart>
                <Pie
                  data={data}
                  cx="50%"
                  cy="50%"
                  innerRadius={60}
                  outerRadius={90}
                  paddingAngle={3}
                  dataKey="value"
                  onMouseEnter={(_, index) => setActiveIndex(index)}
                  onMouseLeave={() => setActiveIndex(null)}
                >
                  {data.map((entry, index) => (
                    <Cell
                      key={`cell-${index}`}
                      fill={entry.color}
                      opacity={activeIndex === null || activeIndex === index ? 1 : 0.4}
                      style={{ transition: "opacity 150ms ease" }}
                    />
                  ))}
                </Pie>
              </PieChart>
            </ResponsiveContainer>
            <div className="pointer-events-none absolute inset-0 flex flex-col items-center justify-center text-center">
              {activeEntry ? (
                <>
                  <span className="text-xs text-muted-foreground truncate max-w-[100px]">{activeEntry.name}</span>
                  <span className="text-sm font-semibold text-foreground">
                    ${activeEntry.value.toLocaleString('es-MX')}
                  </span>
                  <span className="text-xs text-muted-foreground">{Math.round(activeEntry.percentage)}%</span>
                </>
              ) : (
                <span className="text-xs text-muted-foreground">Pasa el cursor</span>
              )}
            </div>
          </div>

          {/* Legend below chart */}
          <div className="grid grid-cols-2 gap-2 mt-2">
            {data.map((item) => (
              <div key={item.name} className="flex items-center gap-2">
                <div
                  className="h-3 w-3 rounded-full shrink-0"
                  style={{ backgroundColor: item.color }}
                />
                <span className="text-xs text-muted-foreground truncate">{item.name}</span>
                <span className="text-xs font-medium text-foreground ml-auto">
                  {Math.round(item.percentage)}%
                </span>
              </div>
            ))}
          </div>
        </>
      )}
    </div>
  )
}
