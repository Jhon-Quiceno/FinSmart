"use client"

import { useEffect, useState } from "react"
import { Sparkles } from "lucide-react"
import { toast } from "sonner"
import { Button } from "@/components/ui/button"
import { CommandDialog } from "@/components/ui/command"
import { Input } from "@/components/ui/input"
import { useQuickAdd } from "@/contexts/quick-add-context"
import { useCategorize } from "@/hooks/use-ai"
import { useCreateExpense, useExpenses } from "@/hooks/use-expenses"
import { toastApiError } from "@/lib/api-client"
import { getTodayDateInput } from "@/lib/date"
import type { ExpenseRequest, PaymentMethodType } from "@/lib/types/expense"

const MIN_DESCRIPTION_LENGTH_FOR_SUGGESTION = 3
const SUGGESTION_DEBOUNCE_MS = 500
/** Used only when the user has no prior expenses to infer a payment method from. */
const DEFAULT_PAYMENT_METHOD: PaymentMethodType = "OTHER"

interface ParsedQuickAdd {
  description: string
  amount: number | undefined
}

/** A category suggestion tagged with the description it was computed for, so a stale suggestion
 *  from a previous keystroke never gets applied to the current text (see `activeSuggestion`
 *  below) without needing an extra effect just to clear it. */
interface CategorySuggestion {
  forDescription: string
  categoryId: number | null
  categoryName: string | null
}

/**
 * Splits a free-text quick-add entry into a description and an optional amount, e.g.
 * "Uber 15000" -> { description: "Uber", amount: 15000 }. Tolerant of thousands separators
 * ("15.000" or "15,000"). Kept intentionally simple: the first numeric token found is treated
 * as the amount and removed from the text; everything else is the description. No NLP, no
 * locale-aware number parsing — this is a quick shortcut, not a form.
 */
function parseQuickAddInput(rawInput: string): ParsedQuickAdd {
  const trimmed = rawInput.trim()
  const match = trimmed.match(/\d[\d.,]*/)

  if (!match || match.index === undefined) {
    return { description: trimmed, amount: undefined }
  }

  const numericToken = match[0]
  const parsedAmount = Number(numericToken.replace(/[.,]/g, ""))
  const description = (trimmed.slice(0, match.index) + trimmed.slice(match.index + numericToken.length))
    .replace(/\s+/g, " ")
    .trim()

  return {
    description: description || trimmed,
    amount: Number.isFinite(parsedAmount) && parsedAmount > 0 ? parsedAmount : undefined,
  }
}

/**
 * Global quick-add dialog for expenses, opened via the FAB or `Ctrl+K` / `Cmd+K`. Free-text in,
 * one confirm click out: parses description + amount from a single input, auto-suggests a
 * category through the same AI categorization endpoint used by `ExpenseModal`, and defaults
 * date/payment method/category so the user only has to type and confirm.
 */
export function QuickAddCommand() {
  const { isOpen, close } = useQuickAdd()
  const [rawInput, setRawInput] = useState("")
  const [suggestion, setSuggestion] = useState<CategorySuggestion | null>(null)

  const { categorize, isLoading: isCategorizing } = useCategorize()
  const { createExpense, isLoading: isSubmitting } = useCreateExpense()
  // Reuses the already-fetched expenses list (most recent first, per backend default sort) to
  // infer sensible defaults instead of adding a new aggregation endpoint just for this.
  const { expenses: recentExpenses } = useExpenses({ page: 0, size: 1 })

  const { description, amount } = parseQuickAddInput(rawInput)
  const lastExpense = recentExpenses.content[0]
  const fallbackPaymentMethod = lastExpense?.paymentMethod ?? DEFAULT_PAYMENT_METHOD
  const fallbackCategoryId = lastExpense?.categoryId ?? null

  // Only trust the suggestion if it was computed for the exact text currently on screen —
  // avoids showing/using a stale suggestion from a previous keystroke while the debounced
  // request for the new text is still in flight.
  const activeSuggestion = suggestion?.forDescription === description ? suggestion : null

  useEffect(() => {
    if (!isOpen || description.length < MIN_DESCRIPTION_LENGTH_FOR_SUGGESTION) {
      return
    }

    const timeoutId = setTimeout(() => {
      categorize({ description, amount, type: "EXPENSE" })
        .then((result) => {
          setSuggestion({
            forDescription: description,
            categoryId: result.categoryId,
            categoryName: result.categoryName,
          })
        })
        .catch(() => {
          // Background convenience only — a failed auto-suggestion silently falls back to the
          // last-used category at confirm time, no need to interrupt the user with a toast.
        })
    }, SUGGESTION_DEBOUNCE_MS)

    return () => clearTimeout(timeoutId)
    // Re-fires only when the parsed text changes or the dialog re-opens; `categorize` is a
    // stable useCallback from useCategorize.
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [description, amount, isOpen])

  const resetAndClose = () => {
    setRawInput("")
    setSuggestion(null)
    close()
  }

  const handleConfirm = async () => {
    if (!description) {
      toast.error("Escribe una descripcion para el gasto")
      return
    }
    if (!amount) {
      toast.error("No detectamos un monto valido. Incluye el monto en el texto, ej: Uber 15000")
      return
    }

    const payload: ExpenseRequest = {
      amount,
      description,
      date: getTodayDateInput(),
      paymentMethod: fallbackPaymentMethod,
      categoryId: activeSuggestion?.categoryId ?? fallbackCategoryId,
    }

    try {
      await createExpense(payload)
      toast.success("Gasto creado correctamente")
      resetAndClose()
    } catch (error) {
      toastApiError(error, "No fue posible crear el gasto")
    }
  }

  return (
    <CommandDialog
      open={isOpen}
      onOpenChange={(nextOpen) => {
        if (!nextOpen) resetAndClose()
      }}
      title="Agregar gasto rapido"
      description="Escribe una descripcion y el monto, ej: Uber 15000"
    >
      <div className="flex flex-col gap-3 p-4">
        <Input
          autoFocus
          placeholder="Ej: Uber 15000"
          value={rawInput}
          onChange={(event) => setRawInput(event.target.value)}
          onKeyDown={(event) => {
            if (event.key === "Enter") {
              event.preventDefault()
              void handleConfirm()
            }
          }}
        />

        {description && (
          <div className="flex flex-col gap-1 rounded-md border border-border bg-muted/30 p-3 text-sm">
            <p>
              <span className="text-muted-foreground">Descripcion: </span>
              {description}
            </p>
            <p>
              <span className="text-muted-foreground">Monto: </span>
              {amount ? `$${amount.toLocaleString("es-MX")}` : "No detectado"}
            </p>
            <p className="flex items-center gap-1">
              <span className="text-muted-foreground">Categoria: </span>
              {isCategorizing ? (
                "Sugiriendo..."
              ) : activeSuggestion?.categoryId ? (
                <span className="inline-flex items-center gap-1">
                  <Sparkles className="size-3.5" />
                  {activeSuggestion.categoryName}
                </span>
              ) : (
                "Sin sugerencia"
              )}
            </p>
            <p className="text-xs text-muted-foreground">
              Fecha: hoy &middot; Metodo de pago: {fallbackPaymentMethod}
            </p>
          </div>
        )}

        <div className="flex justify-end gap-2">
          <Button type="button" variant="outline" onClick={resetAndClose}>
            Cancelar
          </Button>
          <Button type="button" onClick={() => void handleConfirm()} disabled={isSubmitting}>
            {isSubmitting ? "Guardando..." : "Confirmar"}
          </Button>
        </div>
      </div>
    </CommandDialog>
  )
}
