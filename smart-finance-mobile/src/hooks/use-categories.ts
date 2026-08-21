import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query"

import {
  createCategory,
  deleteCategory,
  getCategories,
  updateCategory,
} from "@/lib/services/category.service"
import type { CategoryRequest, CategoryType } from "@/lib/types/category"

export const CATEGORIES_QUERY_KEY = "categories" as const

export function categoriesQueryKey(type?: CategoryType) {
  return [CATEGORIES_QUERY_KEY, type ?? null] as const
}

export function useCategories(type?: CategoryType) {
  return useQuery({
    queryKey: categoriesQueryKey(type),
    queryFn: () => getCategories(type),
  })
}

export function useCreateCategory() {
  const queryClient = useQueryClient()
  return useMutation({
    mutationFn: (payload: CategoryRequest) => createCategory(payload),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: [CATEGORIES_QUERY_KEY] })
    },
  })
}

export function useUpdateCategory() {
  const queryClient = useQueryClient()
  return useMutation({
    mutationFn: ({ id, payload }: { id: number; payload: CategoryRequest }) => updateCategory(id, payload),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: [CATEGORIES_QUERY_KEY] })
    },
  })
}

export function useDeleteCategory() {
  const queryClient = useQueryClient()
  return useMutation({
    mutationFn: (id: number) => deleteCategory(id),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: [CATEGORIES_QUERY_KEY] })
    },
  })
}
