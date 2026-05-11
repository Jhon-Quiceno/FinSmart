"use client"

import { BarChart, Bar, XAxis, YAxis, CartesianGrid, Tooltip, ResponsiveContainer, LineChart, Line, PieChart, Pie, Cell } from "recharts"
import { AppLayout } from "@/components/layout/app-layout"
import { Download, Calendar, TrendingUp, TrendingDown, PiggyBank } from "lucide-react"
import { Button } from "@/components/ui/button"

const monthlyData = [
  { month: "Ene", ingresos: 25000, gastos: 18000, ahorro: 7000 },
  { month: "Feb", ingresos: 27000, gastos: 19500, ahorro: 7500 },
  { month: "Mar", ingresos: 26500, gastos: 21000, ahorro: 5500 },
  { month: "Abr", ingresos: 28000, gastos: 17800, ahorro: 10200 },
  { month: "May", ingresos: 30000, gastos: 22500, ahorro: 7500 },
  { month: "Jun", ingresos: 32000, gastos: 20000, ahorro: 12000 },
]

const categoryData = [
  { name: "Alimentacion", value: 4500, color: "oklch(0.72 0.19 145)" },
  { name: "Transporte", value: 2800, color: "oklch(0.65 0.22 25)" },
  { name: "Entretenimiento", value: 2200, color: "oklch(0.78 0.16 75)" },
  { name: "Servicios", value: 3500, color: "oklch(0.65 0.18 260)" },
  { name: "Otros", value: 1500, color: "oklch(0.70 0.15 200)" },
]

const savingsData = [
  { month: "Ene", meta: 8000, real: 7000 },
  { month: "Feb", meta: 8000, real: 7500 },
  { month: "Mar", meta: 8000, real: 5500 },
  { month: "Abr", meta: 8000, real: 10200 },
  { month: "May", meta: 8000, real: 7500 },
  { month: "Jun", meta: 8000, real: 12000 },
]

export default function ReportesPage() {
  const totalIncome = monthlyData.reduce((sum, m) => sum + m.ingresos, 0)
  const totalExpenses = monthlyData.reduce((sum, m) => sum + m.gastos, 0)
  const totalSavings = monthlyData.reduce((sum, m) => sum + m.ahorro, 0)

  return (
    <AppLayout>
      <div className="space-y-6">
        {/* Header */}
        <div className="flex flex-col sm:flex-row sm:items-center sm:justify-between gap-4">
          <div>
            <h1 className="text-2xl font-bold text-foreground">Reportes</h1>
            <p className="text-sm text-muted-foreground">
              Analiza tu comportamiento financiero
            </p>
          </div>
          <div className="flex items-center gap-3">
            <Button variant="outline" className="gap-2">
              <Calendar className="h-4 w-4" />
              Ultimos 6 meses
            </Button>
            <Button className="gap-2 bg-primary text-primary-foreground hover:bg-primary/90">
              <Download className="h-4 w-4" />
              Exportar
            </Button>
          </div>
        </div>

        {/* Summary Cards */}
        <div className="grid grid-cols-1 md:grid-cols-3 gap-4">
          <div className="rounded-xl bg-card border border-border p-5">
            <div className="flex items-center gap-3">
              <div className="flex h-10 w-10 items-center justify-center rounded-lg bg-success/10">
                <TrendingUp className="h-5 w-5 text-success" />
              </div>
              <div>
                <p className="text-sm text-muted-foreground">Total Ingresos</p>
                <p className="text-xl font-bold text-success">
                  ${totalIncome.toLocaleString("es-MX")}
                </p>
              </div>
            </div>
          </div>

          <div className="rounded-xl bg-card border border-border p-5">
            <div className="flex items-center gap-3">
              <div className="flex h-10 w-10 items-center justify-center rounded-lg bg-destructive/10">
                <TrendingDown className="h-5 w-5 text-destructive" />
              </div>
              <div>
                <p className="text-sm text-muted-foreground">Total Gastos</p>
                <p className="text-xl font-bold text-destructive">
                  ${totalExpenses.toLocaleString("es-MX")}
                </p>
              </div>
            </div>
          </div>

          <div className="rounded-xl bg-card border border-border p-5">
            <div className="flex items-center gap-3">
              <div className="flex h-10 w-10 items-center justify-center rounded-lg bg-primary/10">
                <PiggyBank className="h-5 w-5 text-primary" />
              </div>
              <div>
                <p className="text-sm text-muted-foreground">Total Ahorro</p>
                <p className="text-xl font-bold text-primary">
                  ${totalSavings.toLocaleString("es-MX")}
                </p>
              </div>
            </div>
          </div>
        </div>

        {/* Charts Row */}
        <div className="grid grid-cols-1 lg:grid-cols-2 gap-6">
          {/* Monthly Income vs Expenses */}
          <div className="rounded-xl bg-card border border-border p-5">
            <h3 className="text-lg font-semibold text-foreground mb-4">Ingresos vs Gastos Mensuales</h3>
            <div className="h-[300px]">
              <ResponsiveContainer width="100%" height="100%">
                <BarChart data={monthlyData}>
                  <CartesianGrid strokeDasharray="3 3" stroke="oklch(0.28 0.01 260)" />
                  <XAxis dataKey="month" stroke="oklch(0.65 0.01 260)" fontSize={12} />
                  <YAxis stroke="oklch(0.65 0.01 260)" fontSize={12} tickFormatter={(v) => `$${v/1000}k`} />
                  <Tooltip
                    contentStyle={{
                      backgroundColor: "oklch(0.16 0.008 260)",
                      border: "1px solid oklch(0.28 0.01 260)",
                      borderRadius: "8px",
                      color: "oklch(0.95 0.01 260)",
                    }}
                    formatter={(value: number) => [`$${value.toLocaleString("es-MX")}`, ""]}
                  />
                  <Bar dataKey="ingresos" fill="oklch(0.72 0.19 145)" radius={[4, 4, 0, 0]} name="Ingresos" />
                  <Bar dataKey="gastos" fill="oklch(0.65 0.22 25)" radius={[4, 4, 0, 0]} name="Gastos" />
                </BarChart>
              </ResponsiveContainer>
            </div>
          </div>

          {/* Savings Progress */}
          <div className="rounded-xl bg-card border border-border p-5">
            <h3 className="text-lg font-semibold text-foreground mb-4">Progreso de Ahorro</h3>
            <div className="h-[300px]">
              <ResponsiveContainer width="100%" height="100%">
                <LineChart data={savingsData}>
                  <CartesianGrid strokeDasharray="3 3" stroke="oklch(0.28 0.01 260)" />
                  <XAxis dataKey="month" stroke="oklch(0.65 0.01 260)" fontSize={12} />
                  <YAxis stroke="oklch(0.65 0.01 260)" fontSize={12} tickFormatter={(v) => `$${v/1000}k`} />
                  <Tooltip
                    contentStyle={{
                      backgroundColor: "oklch(0.16 0.008 260)",
                      border: "1px solid oklch(0.28 0.01 260)",
                      borderRadius: "8px",
                      color: "oklch(0.95 0.01 260)",
                    }}
                    formatter={(value: number) => [`$${value.toLocaleString("es-MX")}`, ""]}
                  />
                  <Line type="monotone" dataKey="meta" stroke="oklch(0.65 0.01 260)" strokeDasharray="5 5" strokeWidth={2} name="Meta" dot={false} />
                  <Line type="monotone" dataKey="real" stroke="oklch(0.75 0.18 145)" strokeWidth={2} name="Real" />
                </LineChart>
              </ResponsiveContainer>
            </div>
          </div>
        </div>

        {/* Expenses by Category */}
        <div className="grid grid-cols-1 lg:grid-cols-2 gap-6">
          <div className="rounded-xl bg-card border border-border p-5">
            <h3 className="text-lg font-semibold text-foreground mb-4">Gastos por Categoria</h3>
            <div className="h-[300px]">
              <ResponsiveContainer width="100%" height="100%">
                <PieChart>
                  <Pie
                    data={categoryData}
                    cx="50%"
                    cy="50%"
                    innerRadius={60}
                    outerRadius={100}
                    paddingAngle={3}
                    dataKey="value"
                  >
                    {categoryData.map((entry, index) => (
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
                    formatter={(value: number) => [`$${value.toLocaleString("es-MX")}`, ""]}
                  />
                </PieChart>
              </ResponsiveContainer>
            </div>
            <div className="grid grid-cols-2 gap-2 mt-4">
              {categoryData.map((item) => (
                <div key={item.name} className="flex items-center gap-2">
                  <div className="h-3 w-3 rounded-full shrink-0" style={{ backgroundColor: item.color }} />
                  <span className="text-xs text-muted-foreground">{item.name}</span>
                  <span className="text-xs font-medium text-foreground ml-auto">${item.value.toLocaleString("es-MX")}</span>
                </div>
              ))}
            </div>
          </div>

          {/* Monthly Savings Breakdown */}
          <div className="rounded-xl bg-card border border-border p-5">
            <h3 className="text-lg font-semibold text-foreground mb-4">Ahorro Mensual</h3>
            <div className="h-[300px]">
              <ResponsiveContainer width="100%" height="100%">
                <BarChart data={monthlyData} layout="vertical">
                  <CartesianGrid strokeDasharray="3 3" stroke="oklch(0.28 0.01 260)" />
                  <XAxis type="number" stroke="oklch(0.65 0.01 260)" fontSize={12} tickFormatter={(v) => `$${v/1000}k`} />
                  <YAxis type="category" dataKey="month" stroke="oklch(0.65 0.01 260)" fontSize={12} />
                  <Tooltip
                    contentStyle={{
                      backgroundColor: "oklch(0.16 0.008 260)",
                      border: "1px solid oklch(0.28 0.01 260)",
                      borderRadius: "8px",
                      color: "oklch(0.95 0.01 260)",
                    }}
                    formatter={(value: number) => [`$${value.toLocaleString("es-MX")}`, ""]}
                  />
                  <Bar dataKey="ahorro" fill="oklch(0.75 0.18 145)" radius={[0, 4, 4, 0]} name="Ahorro" />
                </BarChart>
              </ResponsiveContainer>
            </div>
          </div>
        </div>
      </div>
    </AppLayout>
  )
}
