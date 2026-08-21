import { apiClient } from "../api-client"
import type { CardMovement, CardMovementType, CardPaymentRequest, CardPurchaseRequest, Installment } from "../types/card-movement"
import type { PaginatedResponse } from "../types/pagination"

export interface CardMovementFilters {
  type?: CardMovementType
  page?: number
  size?: number
  sort?: string
}

export async function getMovements(
  cardId: number,
  filters: CardMovementFilters = {},
): Promise<PaginatedResponse<CardMovement>> {
  const response = await apiClient.get<PaginatedResponse<CardMovement>>(`/api/cards/${cardId}/movements`, {
    params: filters,
  })

  return response.data
}

export async function registerPurchase(cardId: number, payload: CardPurchaseRequest): Promise<CardMovement> {
  const response = await apiClient.post<CardMovement>(`/api/cards/${cardId}/purchases`, payload)
  return response.data
}

export async function registerPayment(cardId: number, payload: CardPaymentRequest): Promise<CardMovement> {
  const response = await apiClient.post<CardMovement>(`/api/cards/${cardId}/payments`, payload)
  return response.data
}

export async function getInstallments(cardId: number, movementId: number): Promise<Installment[]> {
  const response = await apiClient.get<Installment[]>(`/api/cards/${cardId}/movements/${movementId}/installments`)
  return response.data
}
