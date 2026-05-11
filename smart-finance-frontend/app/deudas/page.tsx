"use client"

import { useState } from "react"
import { Plus, CreditCard, TrendingDown, AlertCircle } from "lucide-react"
import { AppLayout } from "@/components/layout/app-layout"
import { Button } from "@/components/ui/button"
import { DebtCard, type Debt } from "@/components/debts/debt-card"

const initialDebts: Debt[] = [
  {
    id: 1,
    name: "Tarjeta BBVA",
    totalAmount: 25000,
    paidAmount: 18000,
    interestRate: 28.5,
    dueDate: "25 Mar 2026",
    minimumPayment: 2500,
    priority: "high",
  },
  {
    id: 2,
    name: "Prestamo Auto",
    totalAmount: 150000,
    paidAmount: 95000,
    interestRate: 12.5,
    dueDate: "15 Abr 2026",
    minimumPayment: 4500,
    priority: "medium",
  },
  {
    id: 3,
    name: "Tarjeta Banamex",
    totalAmount: 15000,
    paidAmount: 12500,
    interestRate: 32.0,
    dueDate: "20 Mar 2026",
    minimumPayment: 1500,
    priority: "low",
  },
  {
    id: 4,
    name: "Credito Hipotecario",
    totalAmount: 850000,
    paidAmount: 280000,
    interestRate: 9.5,
    dueDate: "01 Abr 2026",
    minimumPayment: 12000,
    priority: "medium",
  },
  {
    id: 5,
    name: "Prestamo Personal",
    totalAmount: 35000,
    paidAmount: 8000,
    interestRate: 18.0,
    dueDate: "10 Mar 2026",
    minimumPayment: 3000,
    priority: "high",
  },
]

export default function DeudasPage() {
  const [debts] = useState<Debt[]>(initialDebts)

  const totalDebt = debts.reduce((sum, d) => sum + d.totalAmount, 0)
  const totalPaid = debts.reduce((sum, d) => sum + d.paidAmount, 0)
  const totalRemaining = totalDebt - totalPaid
  const highPriorityCount = debts.filter(d => d.priority === "high").length

  return (
    <AppLayout>
      <div className="space-y-6">
        {/* Header */}
        <div className="flex flex-col sm:flex-row sm:items-center sm:justify-between gap-4">
          <div>
            <h1 className="text-2xl font-bold text-foreground">Deudas</h1>
            <p className="text-sm text-muted-foreground">
              Gestiona y monitorea todas tus deudas
            </p>
          </div>
          <Button className="bg-warning text-warning-foreground hover:bg-warning/90">
            <Plus className="h-4 w-4 mr-2" />
            Agregar Deuda
          </Button>
        </div>

        {/* Summary Cards */}
        <div className="grid grid-cols-1 md:grid-cols-4 gap-4">
          <div className="rounded-xl bg-card border border-border p-5">
            <div className="flex items-center gap-3">
              <div className="flex h-10 w-10 items-center justify-center rounded-lg bg-warning/10">
                <CreditCard className="h-5 w-5 text-warning" />
              </div>
              <div>
                <p className="text-sm text-muted-foreground">Deuda Total</p>
                <p className="text-xl font-bold text-foreground">
                  ${totalDebt.toLocaleString("es-MX")}
                </p>
              </div>
            </div>
          </div>

          <div className="rounded-xl bg-card border border-border p-5">
            <div className="flex items-center gap-3">
              <div className="flex h-10 w-10 items-center justify-center rounded-lg bg-success/10">
                <TrendingDown className="h-5 w-5 text-success" />
              </div>
              <div>
                <p className="text-sm text-muted-foreground">Total Pagado</p>
                <p className="text-xl font-bold text-success">
                  ${totalPaid.toLocaleString("es-MX")}
                </p>
              </div>
            </div>
          </div>

          <div className="rounded-xl bg-card border border-border p-5">
            <div className="flex items-center gap-3">
              <div className="flex h-10 w-10 items-center justify-center rounded-lg bg-destructive/10">
                <AlertCircle className="h-5 w-5 text-destructive" />
              </div>
              <div>
                <p className="text-sm text-muted-foreground">Por Pagar</p>
                <p className="text-xl font-bold text-destructive">
                  ${totalRemaining.toLocaleString("es-MX")}
                </p>
              </div>
            </div>
          </div>

          <div className="rounded-xl bg-card border border-border p-5">
            <div className="flex items-center gap-3">
              <div className="flex h-10 w-10 items-center justify-center rounded-lg bg-destructive/10">
                <AlertCircle className="h-5 w-5 text-destructive" />
              </div>
              <div>
                <p className="text-sm text-muted-foreground">Alta Prioridad</p>
                <p className="text-xl font-bold text-foreground">{highPriorityCount}</p>
              </div>
            </div>
          </div>
        </div>

        {/* Overall Progress */}
        <div className="rounded-xl bg-card border border-border p-5">
          <div className="flex items-center justify-between mb-3">
            <h3 className="text-sm font-medium text-foreground">Progreso General de Pago</h3>
            <span className="text-sm font-semibold text-primary">
              {((totalPaid / totalDebt) * 100).toFixed(1)}%
            </span>
          </div>
          <div className="h-3 rounded-full bg-secondary overflow-hidden">
            <div
              className="h-full rounded-full bg-gradient-to-r from-primary to-success transition-all duration-500"
              style={{ width: `${(totalPaid / totalDebt) * 100}%` }}
            />
          </div>
          <p className="mt-2 text-xs text-muted-foreground">
            Has pagado ${totalPaid.toLocaleString("es-MX")} de ${totalDebt.toLocaleString("es-MX")}
          </p>
        </div>

        {/* Debt Cards Grid */}
        <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-4">
          {debts.map((debt) => (
            <DebtCard key={debt.id} debt={debt} />
          ))}
        </div>
      </div>
    </AppLayout>
  )
}
