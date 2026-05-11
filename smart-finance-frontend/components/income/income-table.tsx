"use client"

import { Briefcase, Laptop, TrendingUp, Building, Home, Gift, MoreHorizontal } from "lucide-react"
import { cn } from "@/lib/utils"

export interface Income {
  id: number
  description: string
  amount: number
  type: string
  source: string
  date: string
}

interface IncomeTableProps {
  incomes: Income[]
}

const sourceIcons: Record<string, React.ReactNode> = {
  Salario: <Briefcase className="h-4 w-4" />,
  Freelance: <Laptop className="h-4 w-4" />,
  Inversiones: <TrendingUp className="h-4 w-4" />,
  Negocio: <Building className="h-4 w-4" />,
  Alquiler: <Home className="h-4 w-4" />,
  Bonos: <Gift className="h-4 w-4" />,
  Otros: <MoreHorizontal className="h-4 w-4" />,
}

export function IncomeTable({ incomes }: IncomeTableProps) {
  return (
    <div className="rounded-xl bg-card border border-border overflow-hidden">
      {/* Desktop Table */}
      <div className="hidden md:block overflow-x-auto">
        <table className="w-full">
          <thead>
            <tr className="border-b border-border bg-secondary/50">
              <th className="px-4 py-3 text-left text-sm font-medium text-muted-foreground">
                Descripcion
              </th>
              <th className="px-4 py-3 text-left text-sm font-medium text-muted-foreground">
                Fuente
              </th>
              <th className="px-4 py-3 text-left text-sm font-medium text-muted-foreground">
                Tipo
              </th>
              <th className="px-4 py-3 text-left text-sm font-medium text-muted-foreground">
                Monto
              </th>
              <th className="px-4 py-3 text-left text-sm font-medium text-muted-foreground">
                Fecha
              </th>
            </tr>
          </thead>
          <tbody>
            {incomes.map((income) => (
              <tr
                key={income.id}
                className="border-b border-border last:border-0 hover:bg-secondary/30 transition-smooth"
              >
                <td className="px-4 py-3">
                  <div className="flex items-center gap-3">
                    <div className="flex h-8 w-8 items-center justify-center rounded-lg bg-success/10 text-success">
                      {sourceIcons[income.source]}
                    </div>
                    <span className="text-sm font-medium text-foreground">
                      {income.description}
                    </span>
                  </div>
                </td>
                <td className="px-4 py-3">
                  <span className="text-xs font-medium px-2 py-1 rounded-md bg-success/10 text-success">
                    {income.source}
                  </span>
                </td>
                <td className="px-4 py-3">
                  <span className={cn(
                    "text-xs font-medium px-2 py-1 rounded-md",
                    income.type === "Fijo" ? "bg-primary/10 text-primary" : "bg-warning/10 text-warning"
                  )}>
                    {income.type}
                  </span>
                </td>
                <td className="px-4 py-3">
                  <span className="text-sm font-semibold text-success">
                    +${income.amount.toLocaleString("es-MX", { minimumFractionDigits: 2 })}
                  </span>
                </td>
                <td className="px-4 py-3">
                  <span className="text-sm text-muted-foreground">
                    {income.date}
                  </span>
                </td>
              </tr>
            ))}
          </tbody>
        </table>
      </div>

      {/* Mobile List */}
      <div className="md:hidden divide-y divide-border">
        {incomes.map((income) => (
          <div key={income.id} className="p-4 hover:bg-secondary/30 transition-smooth">
            <div className="flex items-center justify-between">
              <div className="flex items-center gap-3">
                <div className="flex h-10 w-10 items-center justify-center rounded-lg bg-success/10 text-success">
                  {sourceIcons[income.source]}
                </div>
                <div>
                  <p className="text-sm font-medium text-foreground">{income.description}</p>
                  <p className="text-xs text-muted-foreground">{income.source} - {income.type}</p>
                </div>
              </div>
              <div className="text-right">
                <p className="text-sm font-semibold text-success">
                  +${income.amount.toLocaleString("es-MX", { minimumFractionDigits: 2 })}
                </p>
                <p className="text-xs text-muted-foreground">{income.date}</p>
              </div>
            </div>
          </div>
        ))}
      </div>
    </div>
  )
}
