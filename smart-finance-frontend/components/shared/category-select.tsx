"use client"

import { Select, SelectContent, SelectGroup, SelectItem, SelectTrigger, SelectValue } from "@/components/ui/select"
import { useCategories } from "@/hooks/use-categories"
import type { CategoryType } from "@/lib/types/category"

interface CategorySelectProps {
  type: CategoryType
  value: string
  onValueChange: (value: string) => void
  placeholder?: string
  disabled?: boolean
  includeEmptyOption?: boolean
}

export function CategorySelect({
  type,
  value,
  onValueChange,
  placeholder = "Selecciona una categoria",
  disabled = false,
  includeEmptyOption = true,
}: CategorySelectProps) {
  const { categories, isLoading } = useCategories(type)

  return (
    <Select value={value} onValueChange={onValueChange} disabled={disabled || isLoading}>
      <SelectTrigger>
        <SelectValue placeholder={isLoading ? "Cargando categorias..." : placeholder} />
      </SelectTrigger>
      <SelectContent>
        <SelectGroup>
          {includeEmptyOption && <SelectItem value="none">Sin categoria</SelectItem>}
          {categories.map((category) => (
            <SelectItem key={category.id} value={String(category.id)}>
              {category.name}
            </SelectItem>
          ))}
        </SelectGroup>
      </SelectContent>
    </Select>
  )
}
