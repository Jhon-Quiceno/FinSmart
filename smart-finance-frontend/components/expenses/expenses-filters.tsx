"use client"

import { FilterX, Search } from "lucide-react"
import { CategorySelect } from "@/components/shared/category-select"
import { Button } from "@/components/ui/button"
import { Input } from "@/components/ui/input"
import { Select, SelectContent, SelectItem, SelectTrigger, SelectValue } from "@/components/ui/select"
import { getRecentPeriodOptions } from "@/lib/utils/period"

const periodOptions = getRecentPeriodOptions()

interface ExpensesFiltersProps {
  searchQuery: string
  onSearchChange: (value: string) => void
  selectedCategory: string
  onCategoryChange: (value: string) => void
  period: string
  onPeriodChange: (value: string) => void
  onClearFilters: () => void
  hasActiveFilters: boolean
}

export function ExpensesFilters({
  searchQuery,
  onSearchChange,
  selectedCategory,
  onCategoryChange,
  period,
  onPeriodChange,
  onClearFilters,
  hasActiveFilters,
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

      <Select value={period} onValueChange={onPeriodChange}>
        <SelectTrigger>
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
        variant="outline"
        onClick={onClearFilters}
        disabled={!hasActiveFilters}
        className="gap-2"
      >
        <FilterX data-icon="inline-start" />
        Limpiar filtros
      </Button>
    </div>
  )
}
