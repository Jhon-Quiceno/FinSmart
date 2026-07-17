import { apiClient } from "../api-client"
import type { CreditCard, CreditCardCreateRequest, CreditCardUpdateRequest } from "../types/credit-card"
import type { PaginatedResponse } from "../types/pagination"

export interface CreditCardFilters {
  page?: number
  size?: number
  sort?: string
}

export async function getCards(filters: CreditCardFilters = {}): Promise<PaginatedResponse<CreditCard>> {
  const response = await apiClient.get<PaginatedResponse<CreditCard>>("/api/cards", {
    params: filters,
  })

  return response.data
}

export async function getCard(id: number): Promise<CreditCard> {
  const response = await apiClient.get<CreditCard>(`/api/cards/${id}`)
  return response.data
}

export async function createCard(payload: CreditCardCreateRequest): Promise<CreditCard> {
  const response = await apiClient.post<CreditCard>("/api/cards", payload)
  return response.data
}

export async function updateCard(id: number, payload: CreditCardUpdateRequest): Promise<CreditCard> {
  const response = await apiClient.put<CreditCard>(`/api/cards/${id}`, payload)
  return response.data
}

export async function deleteCard(id: number): Promise<void> {
  await apiClient.delete(`/api/cards/${id}`)
}
