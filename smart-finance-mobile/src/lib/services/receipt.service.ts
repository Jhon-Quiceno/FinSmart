import { apiClient } from "../api-client"
import type { ReceiptExtraction } from "../types/receipt"

export async function scanReceipt(imageDataUri: string): Promise<ReceiptExtraction> {
  const response = await apiClient.post<ReceiptExtraction>("/api/receipts/scan", { imageDataUri })
  return response.data
}
