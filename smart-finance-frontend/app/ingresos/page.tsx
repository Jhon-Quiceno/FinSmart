"use client"

import { useState } from "react"
import { Plus, TrendingUp, Briefcase, Repeat } from "lucide-react"
import { AppLayout } from "@/components/layout/app-layout"
import { Button } from "@/components/ui/button"
import { IncomeTable, type Income } from "@/components/income/income-table"
import { IncomeModal } from "@/components/income/income-modal"

const initialIncomes: Income[] = [
  { id: 1, description: "Salario Quincenal", amount: 15000, type: "Fijo", source: "Salario", date: "2026-03-15" },
  { id: 2, description: "Proyecto Web Freelance", amount: 8500, type: "Variable", source: "Freelance", date: "2026-03-12" },
  { id: 3, description: "Dividendos Acciones", amount: 2300, type: "Variable", source: "Inversiones", date: "2026-03-10" },
  { id: 4, description: "Salario Quincenal", amount: 15000, type: "Fijo", source: "Salario", date: "2026-03-01" },
  { id: 5, description: "Renta Departamento", amount: 6500, type: "Fijo", source: "Alquiler", date: "2026-03-01" },
  { id: 6, description: "Bono Anual", amount: 12000, type: "Variable", source: "Bonos", date: "2026-02-28" },
  { id: 7, description: "Diseno de Logo", amount: 3000, type: "Variable", source: "Freelance", date: "2026-02-25" },
  { id: 8, description: "Intereses Inversiones", amount: 850, type: "Variable", source: "Inversiones", date: "2026-02-20" },
]

export default function IngresosPage() {
  const [incomes, setIncomes] = useState<Income[]>(initialIncomes)
  const [isModalOpen, setIsModalOpen] = useState(false)

  const totalIncome = incomes.reduce((sum, inc) => sum + inc.amount, 0)
  const fixedIncome = incomes.filter(i => i.type === "Fijo").reduce((sum, inc) => sum + inc.amount, 0)
  const variableIncome = incomes.filter(i => i.type === "Variable").reduce((sum, inc) => sum + inc.amount, 0)

  const handleAddIncome = (newIncome: {
    description: string
    amount: number
    type: string
    source: string
    date: string
  }) => {
    const income: Income = {
      id: incomes.length + 1,
      ...newIncome,
    }
    setIncomes([income, ...incomes])
  }

  return (
    <AppLayout>
      <div className="space-y-6">
        {/* Header */}
        <div className="flex flex-col sm:flex-row sm:items-center sm:justify-between gap-4">
          <div>
            <h1 className="text-2xl font-bold text-foreground">Ingresos</h1>
            <p className="text-sm text-muted-foreground">
              Registra y visualiza todos tus ingresos
            </p>
          </div>
          <Button
            onClick={() => setIsModalOpen(true)}
            className="bg-success text-success-foreground hover:bg-success/90"
          >
            <Plus className="h-4 w-4 mr-2" />
            Agregar Ingreso
          </Button>
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
                  +${totalIncome.toLocaleString("es-MX", { minimumFractionDigits: 2 })}
                </p>
              </div>
            </div>
          </div>

          <div className="rounded-xl bg-card border border-border p-5">
            <div className="flex items-center gap-3">
              <div className="flex h-10 w-10 items-center justify-center rounded-lg bg-primary/10">
                <Briefcase className="h-5 w-5 text-primary" />
              </div>
              <div>
                <p className="text-sm text-muted-foreground">Ingresos Fijos</p>
                <p className="text-xl font-bold text-primary">
                  ${fixedIncome.toLocaleString("es-MX", { minimumFractionDigits: 2 })}
                </p>
              </div>
            </div>
          </div>

          <div className="rounded-xl bg-card border border-border p-5">
            <div className="flex items-center gap-3">
              <div className="flex h-10 w-10 items-center justify-center rounded-lg bg-warning/10">
                <Repeat className="h-5 w-5 text-warning" />
              </div>
              <div>
                <p className="text-sm text-muted-foreground">Ingresos Variables</p>
                <p className="text-xl font-bold text-warning">
                  ${variableIncome.toLocaleString("es-MX", { minimumFractionDigits: 2 })}
                </p>
              </div>
            </div>
          </div>
        </div>

        {/* Table */}
        <IncomeTable incomes={incomes} />

        {/* Modal */}
        <IncomeModal
          isOpen={isModalOpen}
          onClose={() => setIsModalOpen(false)}
          onSubmit={handleAddIncome}
        />
      </div>
    </AppLayout>
  )
}
