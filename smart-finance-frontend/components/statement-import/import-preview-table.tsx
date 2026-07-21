"use client"

import { Badge } from "@/components/ui/badge"
import { Checkbox } from "@/components/ui/checkbox"
import { Select, SelectContent, SelectGroup, SelectItem, SelectTrigger, SelectValue } from "@/components/ui/select"
import { Table, TableBody, TableCell, TableHead, TableHeader, TableRow } from "@/components/ui/table"
import type { Category } from "@/lib/types/category"
import type { MovementType } from "@/lib/types/statement-import"

export interface EditableImportRow {
  id: string
  date: string
  description: string
  amount: number
  movementType: MovementType
  isDuplicate: boolean
  categoryId: number | null
}

interface ImportPreviewTableProps {
  rows: EditableImportRow[]
  selectedIds: ReadonlySet<string>
  categoriesByType: Record<MovementType, Category[]>
  onToggleRow: (id: string) => void
  onToggleAll: (checked: boolean) => void
  onCategoryChange: (id: string, categoryId: number | null) => void
}

const movementTypeLabels: Record<MovementType, string> = {
  INCOME: "Ingreso",
  EXPENSE: "Gasto",
}

const movementTypeBadgeClasses: Record<MovementType, string> = {
  INCOME: "border-transparent bg-success/10 text-success",
  EXPENSE: "border-transparent bg-destructive/10 text-destructive",
}

function formatAmount(amount: number): string {
  return amount.toLocaleString("es-MX", { minimumFractionDigits: 2 })
}

function RowCategorySelect({
  row,
  categoriesByType,
  onCategoryChange,
}: {
  row: EditableImportRow
  categoriesByType: Record<MovementType, Category[]>
  onCategoryChange: (id: string, categoryId: number | null) => void
}) {
  const options = categoriesByType[row.movementType]
  const value = row.categoryId === null ? "none" : String(row.categoryId)

  return (
    <Select
      value={value}
      onValueChange={(nextValue) => onCategoryChange(row.id, nextValue === "none" ? null : Number(nextValue))}
    >
      <SelectTrigger className="w-full">
        <SelectValue placeholder="Selecciona una categoria" />
      </SelectTrigger>
      <SelectContent>
        <SelectGroup>
          <SelectItem value="none">Sin categoria</SelectItem>
          {options.map((category) => (
            <SelectItem key={category.id} value={String(category.id)}>
              {category.name}
            </SelectItem>
          ))}
        </SelectGroup>
      </SelectContent>
    </Select>
  )
}

export function ImportPreviewTable({
  rows,
  selectedIds,
  categoriesByType,
  onToggleRow,
  onToggleAll,
  onCategoryChange,
}: ImportPreviewTableProps) {
  const allSelected = rows.length > 0 && selectedIds.size === rows.length
  const headerCheckedState: boolean | "indeterminate" =
    selectedIds.size === 0 ? false : allSelected ? true : "indeterminate"

  return (
    <div className="w-full">
      <div className="hidden md:block w-full overflow-x-auto rounded-xl border border-border bg-card">
        <Table>
          <TableHeader>
            <TableRow>
              <TableHead>
                <Checkbox
                  checked={headerCheckedState}
                  onCheckedChange={(checked) => onToggleAll(checked === true)}
                  aria-label="Seleccionar todos los movimientos"
                />
              </TableHead>
              <TableHead>Fecha</TableHead>
              <TableHead>Descripcion</TableHead>
              <TableHead>Monto</TableHead>
              <TableHead>Tipo</TableHead>
              <TableHead>Categoria</TableHead>
              <TableHead>Estado</TableHead>
            </TableRow>
          </TableHeader>
          <TableBody>
            {rows.map((row) => (
              <TableRow key={row.id}>
                <TableCell>
                  <Checkbox
                    checked={selectedIds.has(row.id)}
                    onCheckedChange={() => onToggleRow(row.id)}
                    aria-label={`Seleccionar movimiento ${row.description}`}
                  />
                </TableCell>
                <TableCell>{row.date}</TableCell>
                <TableCell className="font-medium">{row.description || "Sin descripcion"}</TableCell>
                <TableCell
                  className={
                    row.movementType === "INCOME" ? "font-semibold text-success" : "font-semibold text-destructive"
                  }
                >
                  {row.movementType === "INCOME" ? "+" : "-"}${formatAmount(row.amount)}
                </TableCell>
                <TableCell>
                  <Badge className={movementTypeBadgeClasses[row.movementType]}>
                    {movementTypeLabels[row.movementType]}
                  </Badge>
                </TableCell>
                <TableCell className="min-w-[200px]">
                  <RowCategorySelect row={row} categoriesByType={categoriesByType} onCategoryChange={onCategoryChange} />
                </TableCell>
                <TableCell>
                  {row.isDuplicate && <Badge variant="secondary">Posible duplicado</Badge>}
                </TableCell>
              </TableRow>
            ))}
          </TableBody>
        </Table>
      </div>

      <div className="md:hidden space-y-3">
        <div className="flex items-center gap-2 rounded-lg border border-border bg-card p-3">
          <Checkbox
            checked={headerCheckedState}
            onCheckedChange={(checked) => onToggleAll(checked === true)}
            aria-label="Seleccionar todos los movimientos"
          />
          <span className="text-sm font-medium text-foreground">Seleccionar todos</span>
        </div>

        {rows.map((row) => (
          <div key={row.id} className="rounded-lg border border-border bg-card p-4">
            <div className="flex items-start justify-between gap-2">
              <div className="flex items-start gap-2">
                <Checkbox
                  checked={selectedIds.has(row.id)}
                  onCheckedChange={() => onToggleRow(row.id)}
                  className="mt-1"
                  aria-label={`Seleccionar movimiento ${row.description}`}
                />
                <div>
                  <span className="font-semibold">{row.description || "Sin descripcion"}</span>
                  {row.isDuplicate && (
                    <div className="mt-1">
                      <Badge variant="secondary">Posible duplicado</Badge>
                    </div>
                  )}
                </div>
              </div>
              <span
                className={
                  row.movementType === "INCOME"
                    ? "shrink-0 text-lg font-semibold text-success"
                    : "shrink-0 text-lg font-semibold text-destructive"
                }
              >
                {row.movementType === "INCOME" ? "+" : "-"}${formatAmount(row.amount)}
              </span>
            </div>

            <div className="mt-3 grid grid-cols-2 gap-y-2 text-sm">
              <div>
                <span className="block text-xs uppercase tracking-wide text-muted-foreground">Fecha</span>
                <span className="text-foreground">{row.date}</span>
              </div>
              <div>
                <span className="block text-xs uppercase tracking-wide text-muted-foreground">Tipo</span>
                <Badge className={movementTypeBadgeClasses[row.movementType]}>
                  {movementTypeLabels[row.movementType]}
                </Badge>
              </div>
            </div>

            <div className="mt-3">
              <span className="mb-1 block text-xs uppercase tracking-wide text-muted-foreground">Categoria</span>
              <RowCategorySelect row={row} categoriesByType={categoriesByType} onCategoryChange={onCategoryChange} />
            </div>
          </div>
        ))}
      </div>
    </div>
  )
}
