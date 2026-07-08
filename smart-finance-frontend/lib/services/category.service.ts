import { apiClient } from "@/lib/api-client"
import type { Category, CategoryRequest, CategoryType } from "@/lib/types/category"

export async function getCategories(type?: CategoryType): Promise<Category[]> {
  const response = await apiClient.get<Category[]>("/api/categories", {
    params: type ? { type } : undefined,
  })

  return response.data
}

export async function createCategory(payload: CategoryRequest): Promise<Category> {
  const response = await apiClient.post<Category>("/api/categories", payload)
  return response.data
}

export async function updateCategory(id: number, payload: CategoryRequest): Promise<Category> {
  const response = await apiClient.put<Category>(`/api/categories/${id}`, payload)
  return response.data
}

export async function deleteCategory(id: number): Promise<void> {
  await apiClient.delete(`/api/categories/${id}`)
}
