"use client"

import { useState } from "react"
import { Plus, TrendingDown } from "lucide-react"
import { AppLayout } from "@/components/layout/app-layout"
import { Button } from "@/components/ui/button"
import { ExpensesTable, type Expense } from "@/components/expenses/expenses-table"
import { ExpensesFilters } from "@/components/expenses/expenses-filters"
import { ExpenseModal } from "@/components/expenses/expense-modal"

const initialExpenses: Expense[] = [
  { id: 1, description: "Supermercado Walmart", amount: 1250, category: "Alimentacion", paymentMethod: "Tarjeta de Debito", date: "2026-03-20" },
  { id: 2, description: "Gasolina Shell", amount: 850, category: "Transporte", paymentMethod: "Tarjeta de Credito", date: "2026-03-19" },
  { id: 3, description: "Netflix", amount: 299, category: "Entretenimiento", paymentMethod: "Tarjeta de Credito", date: "2026-03-18" },
  { id: 4, description: "Recibo de Luz CFE", amount: 520, category: "Servicios", paymentMethod: "Transferencia", date: "2026-03-17" },
  { id: 5, description: "Consulta Medica", amount: 800, category: "Salud", paymentMethod: "Efectivo", date: "2026-03-16" },
  { id: 6, description: "Curso Udemy", amount: 199, category: "Educacion", paymentMethod: "Tarjeta de Credito", date: "2026-03-15" },
  { id: 7, description: "Restaurante El Asador", amount: 450, category: "Alimentacion", paymentMethod: "Tarjeta de Debito", date: "2026-03-14" },
  { id: 8, description: "Uber", amount: 180, category: "Transporte", paymentMethod: "Tarjeta de Debito", date: "2026-03-13" },
  { id: 9, description: "Spotify Premium", amount: 129, category: "Entretenimiento", paymentMethod: "Tarjeta de Credito", date: "2026-03-12" },
  { id: 10, description: "Playera Zara", amount: 599, category: "Ropa", paymentMethod: "Tarjeta de Credito", date: "2026-03-11" },
]

export default function GastosPage() {
  const [expenses, setExpenses] = useState<Expense[]>(initialExpenses)
  const [isModalOpen, setIsModalOpen] = useState(false)
  const [searchQuery, setSearchQuery] = useState("")
  const [selectedCategory, setSelectedCategory] = useState("Todas")
  const [dateRange, setDateRange] = useState("all")

  const filteredExpenses = expenses.filter((expense) => {
    const matchesSearch = expense.description.toLowerCase().includes(searchQuery.toLowerCase())
    const matchesCategory = selectedCategory === "Todas" || expense.category === selectedCategory
    return matchesSearch && matchesCategory
  })

  const totalExpenses = filteredExpenses.reduce((sum, exp) => sum + exp.amount, 0)

  const handleAddExpense = (newExpense: {
    description: string
    amount: number
    category: string
    paymentMethod: string
    date: string
  }) => {
    const expense: Expense = {
      id: expenses.length + 1,
      ...newExpense,
    }
    setExpenses([expense, ...expenses])
  }

  return (
    <AppLayout>
      <div className="space-y-6">
        {/* Header */}
        <div className="flex flex-col sm:flex-row sm:items-center sm:justify-between gap-4">
          <div>
            <h1 className="text-2xl font-bold text-foreground">Gastos</h1>
            <p className="text-sm text-muted-foreground">
              Administra y registra todos tus gastos
            </p>
          </div>
          <Button
            onClick={() => setIsModalOpen(true)}
            className="bg-primary text-primary-foreground hover:bg-primary/90"
          >
            <Plus className="h-4 w-4 mr-2" />
            Agregar Gasto
          </Button>
        </div>

        {/* Summary Card */}
        <div className="rounded-xl bg-card border border-border p-5">
          <div className="flex items-center gap-4">
            <div className="flex h-12 w-12 items-center justify-center rounded-xl bg-destructive/10">
              <TrendingDown className="h-6 w-6 text-destructive" />
            </div>
            <div>
              <p className="text-sm text-muted-foreground">Total de Gastos (filtrado)</p>
              <p className="text-2xl font-bold text-destructive">
                -${totalExpenses.toLocaleString("es-MX", { minimumFractionDigits: 2 })}
              </p>
            </div>
            <div className="ml-auto text-right">
              <p className="text-sm text-muted-foreground">Transacciones</p>
              <p className="text-xl font-semibold text-foreground">{filteredExpenses.length}</p>
            </div>
          </div>
        </div>

        {/* Filters */}
        <ExpensesFilters
          searchQuery={searchQuery}
          onSearchChange={setSearchQuery}
          selectedCategory={selectedCategory}
          onCategoryChange={setSelectedCategory}
          dateRange={dateRange}
          onDateRangeChange={setDateRange}
        />

        {/* Table */}
        <ExpensesTable expenses={filteredExpenses} />

        {/* Modal */}
        <ExpenseModal
          isOpen={isModalOpen}
          onClose={() => setIsModalOpen(false)}
          onSubmit={handleAddExpense}
        />
      </div>
    </AppLayout>
  )
}
