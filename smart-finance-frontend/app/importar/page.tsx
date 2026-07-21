"use client"

import { useMemo, useState } from "react"
import type { ChangeEvent } from "react"
import Link from "next/link"
import { CheckCircle2, FileUp, Upload } from "lucide-react"
import { toast } from "sonner"
import { ImportPreviewTable } from "@/components/statement-import/import-preview-table"
import type { EditableImportRow } from "@/components/statement-import/import-preview-table"
import { AppLayout } from "@/components/layout/app-layout"
import { Button } from "@/components/ui/button"
import { Input } from "@/components/ui/input"
import { Label } from "@/components/ui/label"
import { Select, SelectContent, SelectGroup, SelectItem, SelectTrigger, SelectValue } from "@/components/ui/select"
import { Skeleton } from "@/components/ui/skeleton"
import { useCategories } from "@/hooks/use-categories"
import { toastApiError } from "@/lib/api-client"
import { confirmImport, previewStatement } from "@/lib/services/statement-import.service"
import { PAYMENT_METHODS } from "@/lib/types/expense"
import type { PaymentMethodType } from "@/lib/types/expense"
import type { ImportConfirmRow } from "@/lib/types/statement-import"

type PageStatus = "select" | "preview" | "success"

const paymentMethodLabels: Record<PaymentMethodType, string> = {
  CASH: "Efectivo",
  DEBIT_CARD: "Tarjeta de Debito",
  CREDIT_CARD: "Tarjeta de Credito",
  TRANSFER: "Transferencia",
  OTHER: "Otro",
}

function isPdfFile(file: File): boolean {
  return file.name.toLowerCase().endsWith(".pdf")
}

export default function ImportarExtractoPage() {
  const [status, setStatus] = useState<PageStatus>("select")
  const [file, setFile] = useState<File | null>(null)
  const [password, setPassword] = useState("")
  const [isAnalyzing, setIsAnalyzing] = useState(false)
  const [isConfirming, setIsConfirming] = useState(false)
  const [rows, setRows] = useState<EditableImportRow[]>([])
  const [selectedIds, setSelectedIds] = useState<Set<string>>(new Set())
  const [paymentMethod, setPaymentMethod] = useState<PaymentMethodType>("OTHER")
  const [createdCount, setCreatedCount] = useState(0)

  const { categories: expenseCategories } = useCategories("EXPENSE")
  const { categories: incomeCategories } = useCategories("INCOME")

  const categoriesByType = useMemo(
    () => ({
      EXPENSE: expenseCategories,
      INCOME: incomeCategories,
    }),
    [expenseCategories, incomeCategories],
  )

  const duplicateCount = useMemo(() => rows.filter((row) => row.isDuplicate).length, [rows])

  const resetToSelect = () => {
    setStatus("select")
    setFile(null)
    setPassword("")
    setRows([])
    setSelectedIds(new Set())
    setPaymentMethod("OTHER")
    setCreatedCount(0)
  }

  const handleFileChange = (event: ChangeEvent<HTMLInputElement>) => {
    const nextFile = event.target.files?.[0] ?? null
    setFile(nextFile)
    if (!nextFile || !isPdfFile(nextFile)) {
      setPassword("")
    }
  }

  const handleAnalyze = async () => {
    if (!file) return

    setIsAnalyzing(true)
    try {
      const response = await previewStatement(file, password || undefined)

      const editableRows: EditableImportRow[] = response.rows.map((row, index) => ({
        id: `${index}-${row.date}-${row.description}`,
        date: row.date,
        description: row.description,
        amount: row.amount,
        movementType: row.movementType,
        isDuplicate: row.isDuplicate,
        categoryId: row.suggestedCategoryId,
      }))

      setRows(editableRows)
      setSelectedIds(new Set(editableRows.filter((row) => !row.isDuplicate).map((row) => row.id)))
      setStatus("preview")
    } catch (error) {
      toastApiError(error, "No fue posible analizar el extracto")
    } finally {
      setIsAnalyzing(false)
    }
  }

  const handleToggleRow = (id: string) => {
    setSelectedIds((current) => {
      const next = new Set(current)
      if (next.has(id)) {
        next.delete(id)
      } else {
        next.add(id)
      }
      return next
    })
  }

  const handleToggleAll = (checked: boolean) => {
    setSelectedIds(checked ? new Set(rows.map((row) => row.id)) : new Set())
  }

  const handleCategoryChange = (id: string, categoryId: number | null) => {
    setRows((current) => current.map((row) => (row.id === id ? { ...row, categoryId } : row)))
  }

  const handleConfirm = async () => {
    const selectedRows = rows.filter((row) => selectedIds.has(row.id))
    if (selectedRows.length === 0) return

    setIsConfirming(true)
    try {
      const confirmRows: ImportConfirmRow[] = selectedRows.map((row) => ({
        movementType: row.movementType,
        amount: row.amount,
        date: row.date,
        description: row.description || undefined,
        categoryId: row.categoryId,
        paymentMethod: row.movementType === "EXPENSE" ? paymentMethod : undefined,
      }))

      const result = await confirmImport({ rows: confirmRows })
      setCreatedCount(result.createdCount)
      setStatus("success")
      toast.success(`Se crearon ${result.createdCount} movimientos`)
    } catch (error) {
      toastApiError(error, "No fue posible confirmar la importacion")
    } finally {
      setIsConfirming(false)
    }
  }

  return (
    <AppLayout>
      <div className="flex flex-col gap-6">
        <div>
          <h1 className="text-2xl font-bold text-foreground">Importar extracto</h1>
          <p className="text-sm text-muted-foreground">
            Sube el extracto de tu banco (PDF, CSV o XLSX) y la IA extrae los movimientos. Nada se guarda hasta que
            confirmes.
          </p>
        </div>

        {status === "select" && !isAnalyzing && (
          <div className="rounded-xl border border-border bg-card p-6">
            <div className="flex flex-col items-center justify-center gap-3 rounded-lg border border-dashed border-border p-8 text-center">
              <FileUp className="size-8 text-muted-foreground" />
              <div>
                <Label htmlFor="statement-file" className="cursor-pointer text-sm font-medium text-foreground">
                  {file ? file.name : "Selecciona el extracto de tu banco"}
                </Label>
                <p className="mt-1 text-xs text-muted-foreground">Formatos aceptados: PDF, CSV o XLSX (max. 10MB)</p>
              </div>
              <Input
                id="statement-file"
                type="file"
                accept=".pdf,.csv,.xlsx"
                onChange={handleFileChange}
                className="max-w-xs"
              />
            </div>

            {file && isPdfFile(file) && (
              <div className="mt-4 space-y-1">
                <Label htmlFor="statement-password">Contrasena del PDF (si tiene)</Label>
                <Input
                  id="statement-password"
                  type="password"
                  value={password}
                  onChange={(event) => setPassword(event.target.value)}
                  placeholder="********"
                />
                <p className="text-xs text-muted-foreground">La contrasena no se guarda.</p>
              </div>
            )}

            <p className="mt-4 text-xs text-muted-foreground">
              El contenido del extracto se procesa con el proveedor de IA configurado para extraer los movimientos.
            </p>

            <div className="mt-6 flex justify-end">
              <Button disabled={!file} onClick={() => void handleAnalyze()}>
                Analizar extracto
              </Button>
            </div>
          </div>
        )}

        {isAnalyzing && (
          <div className="rounded-xl border border-border bg-card p-6">
            <div className="flex flex-col items-center gap-3 py-8 text-center">
              <Skeleton className="h-8 w-8 rounded-full" />
              <p className="text-sm text-muted-foreground">Analizando extracto...</p>
            </div>
          </div>
        )}

        {status === "preview" && (
          <div className="flex flex-col gap-4">
            <div className="rounded-xl border border-border bg-card p-5">
              <p className="text-sm text-foreground">
                {rows.length} movimientos detectados, {duplicateCount} posibles duplicados
              </p>
            </div>

            <div className="max-w-xs space-y-2">
              <Label>Metodo de pago (para gastos)</Label>
              <Select value={paymentMethod} onValueChange={(value) => setPaymentMethod(value as PaymentMethodType)}>
                <SelectTrigger>
                  <SelectValue />
                </SelectTrigger>
                <SelectContent>
                  <SelectGroup>
                    {PAYMENT_METHODS.map((method) => (
                      <SelectItem key={method} value={method}>
                        {paymentMethodLabels[method]}
                      </SelectItem>
                    ))}
                  </SelectGroup>
                </SelectContent>
              </Select>
            </div>

            <ImportPreviewTable
              rows={rows}
              selectedIds={selectedIds}
              categoriesByType={categoriesByType}
              onToggleRow={handleToggleRow}
              onToggleAll={handleToggleAll}
              onCategoryChange={handleCategoryChange}
            />

            <div className="flex flex-col-reverse gap-2 sm:flex-row sm:justify-end">
              <Button variant="outline" onClick={resetToSelect} disabled={isConfirming}>
                Cancelar
              </Button>
              <Button disabled={selectedIds.size === 0 || isConfirming} onClick={() => void handleConfirm()}>
                {isConfirming ? "Confirmando..." : `Confirmar ${selectedIds.size} movimientos`}
              </Button>
            </div>
          </div>
        )}

        {status === "success" && (
          <div className="rounded-xl border border-border bg-card p-8">
            <div className="flex flex-col items-center gap-3 text-center">
              <CheckCircle2 className="size-10 text-success" />
              <h2 className="text-lg font-semibold text-foreground">
                Se crearon {createdCount} movimientos correctamente
              </h2>
              <div className="mt-2 flex flex-col gap-2 sm:flex-row">
                <Button variant="outline" asChild>
                  <Link href="/gastos">Ver gastos</Link>
                </Button>
                <Button variant="outline" asChild>
                  <Link href="/ingresos">Ver ingresos</Link>
                </Button>
                <Button onClick={resetToSelect}>
                  <Upload data-icon="inline-start" />
                  Importar otro extracto
                </Button>
              </div>
            </div>
          </div>
        )}
      </div>
    </AppLayout>
  )
}
