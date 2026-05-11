"use client"

import { PieChart, Pie, Cell, ResponsiveContainer, Tooltip, Legend } from "recharts"

const data = [
  { name: "Alimentacion", value: 4500, color: "oklch(0.72 0.19 145)" },
  { name: "Transporte", value: 2800, color: "oklch(0.65 0.22 25)" },
  { name: "Entretenimiento", value: 2200, color: "oklch(0.78 0.16 75)" },
  { name: "Servicios", value: 3500, color: "oklch(0.65 0.18 260)" },
  { name: "Otros", value: 1500, color: "oklch(0.70 0.15 200)" },
]

export function ExpensesByCategoryChart() {
  const total = data.reduce((sum, item) => sum + item.value, 0)

  return (
    <div className="rounded-xl bg-card border border-border p-5">
      <div className="mb-4">
        <h3 className="text-lg font-semibold text-foreground">Gastos por Categoria</h3>
        <p className="text-sm text-muted-foreground">Distribucion del mes actual</p>
      </div>

      <div className="h-[280px]">
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
            >
              {data.map((entry, index) => (
                <Cell key={`cell-${index}`} fill={entry.color} />
              ))}
            </Pie>
            <Tooltip
              contentStyle={{
                backgroundColor: "oklch(0.16 0.008 260)",
                border: "1px solid oklch(0.28 0.01 260)",
                borderRadius: "8px",
                color: "oklch(0.95 0.01 260)",
              }}
              formatter={(value: number) => [`$${value.toLocaleString('es-MX')}`, '']}
            />
          </PieChart>
        </ResponsiveContainer>
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
              {Math.round((item.value / total) * 100)}%
            </span>
          </div>
        ))}
      </div>
    </div>
  )
}
