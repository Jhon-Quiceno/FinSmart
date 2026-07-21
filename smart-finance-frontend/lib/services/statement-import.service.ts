import { apiClient } from "../api-client"
import type {
  StatementConfirmRequest,
  StatementImportResultResponse,
  StatementPreviewResponse,
} from "../types/statement-import"

export async function previewStatement(file: File, password?: string): Promise<StatementPreviewResponse> {
  const formData = new FormData()
  formData.append("file", file)
  if (password) {
    formData.append("password", password)
  }

  // El api-client define "Content-Type: application/json" por defecto. Ese default se pegaria
  // tambien a esta request e impediria que el navegador genere el boundary de multipart, asi que
  // se anula explicitamente para que axios/el navegador armen "multipart/form-data; boundary=..."
  // automaticamente a partir del FormData.
  const response = await apiClient.post<StatementPreviewResponse>("/api/statement-imports/preview", formData, {
    headers: { "Content-Type": undefined },
  })

  return response.data
}

export async function confirmImport(request: StatementConfirmRequest): Promise<StatementImportResultResponse> {
  const response = await apiClient.post<StatementImportResultResponse>("/api/statement-imports/confirm", request)
  return response.data
}
