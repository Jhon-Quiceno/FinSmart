"use client"

import { useState } from "react"
import { toast } from "sonner"
import { CategoryModal } from "@/components/categories/category-modal"
import {
  Select,
  SelectContent,
  SelectGroup,
  SelectItem,
  SelectSeparator,
  SelectTrigger,
  SelectValue,
} from "@/components/ui/select"
import { useCategories, useCreateCategory } from "@/hooks/use-categories"
import { getApiErrorMessage } from "@/lib/api-client"
import type { CategoryFormValues } from "@/lib/schemas/category.schema"
import type { CategoryRequest, CategoryType } from "@/lib/types/category"

const CREATE_CATEGORY_VALUE = "__create__"

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
  const { createCategory, isLoading: isCreating } = useCreateCategory()
  const [createModalOpen, setCreateModalOpen] = useState(false)

  const handleValueChange = (nextValue: string) => {
    if (nextValue === CREATE_CATEGORY_VALUE) {
      setCreateModalOpen(true)
      return
    }

    onValueChange(nextValue)
  }

  const handleCreateCategory = async (values: CategoryFormValues) => {
    const payload: CategoryRequest = {
      name: values.name,
      type: values.type,
    }

    try {
      const created = await createCategory(payload)
      toast.success("Categoria creada correctamente")
      setCreateModalOpen(false)
      onValueChange(String(created.id))
    } catch (error) {
      toast.error(getApiErrorMessage(error, "No fue posible crear la categoria"))
    }
  }

  return (
    <>
      <Select value={value} onValueChange={handleValueChange} disabled={disabled || isLoading}>
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
          <SelectSeparator />
          <SelectGroup>
            <SelectItem value={CREATE_CATEGORY_VALUE}>+ Nueva categoria</SelectItem>
          </SelectGroup>
        </SelectContent>
      </Select>

      <CategoryModal
        open={createModalOpen}
        onOpenChange={setCreateModalOpen}
        defaultType={type}
        isSubmitting={isCreating}
        onSubmit={handleCreateCategory}
      />
    </>
  )
}
