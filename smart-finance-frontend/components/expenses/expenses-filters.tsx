"use client"

import { CalendarDays, Search } from "lucide-react"
import { CategorySelect } from "@/components/shared/category-select"
import { Input } from "@/components/ui/input"

interface ExpensesFiltersProps {
  searchQuery: string
  onSearchChange: (value: string) => void
  selectedCategory: string
  onCategoryChange: (value: string) => void
  from: string
  onFromChange: (value: string) => void
  to: string
  onToChange: (value: string) => void
}

export function ExpensesFilters({
  searchQuery,
  onSearchChange,
  selectedCategory,
  onCategoryChange,
  from,
  onFromChange,
  to,
  onToChange,
}: ExpensesFiltersProps) {
  return (
    <div className="grid grid-cols-1 gap-3 lg:grid-cols-4">
      <div className="relative">
        <Search className="absolute left-3 top-1/2 -translate-y-1/2 text-muted-foreground" />
        <Input
          value={searchQuery}
          onChange={(event) => onSearchChange(event.target.value)}
          placeholder="Buscar por descripcion o metodo"
          className="pl-10"
        />
      </div>

      <CategorySelect
        type="EXPENSE"
        value={selectedCategory}
        onValueChange={onCategoryChange}
        placeholder="Todas las categorias"
      />

      <div className="relative">
        <CalendarDays className="absolute left-3 top-1/2 -translate-y-1/2 text-muted-foreground" />
        <Input type="date" value={from} onChange={(event) => onFromChange(event.target.value)} className="pl-10" />
      </div>

      <div className="relative">
        <CalendarDays className="absolute left-3 top-1/2 -translate-y-1/2 text-muted-foreground" />
        <Input type="date" value={to} onChange={(event) => onToChange(event.target.value)} className="pl-10" />
      </div>
    </div>
  )
}
