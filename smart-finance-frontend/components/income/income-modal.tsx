"use client"

import { useState } from "react"
import { X } from "lucide-react"
import { Button } from "@/components/ui/button"
import { Input } from "@/components/ui/input"
import { Label } from "@/components/ui/label"
import {
  Select,
  SelectContent,
  SelectItem,
  SelectTrigger,
  SelectValue,
} from "@/components/ui/select"

interface IncomeModalProps {
  isOpen: boolean
  onClose: () => void
  onSubmit: (income: {
    description: string
    amount: number
    type: string
    source: string
    date: string
  }) => void
}

const incomeTypes = ["Fijo", "Variable"]

const incomeSources = [
  "Salario",
  "Freelance",
  "Inversiones",
  "Negocio",
  "Alquiler",
  "Bonos",
  "Otros",
]

export function IncomeModal({ isOpen, onClose, onSubmit }: IncomeModalProps) {
  const [description, setDescription] = useState("")
  const [amount, setAmount] = useState("")
  const [type, setType] = useState("")
  const [source, setSource] = useState("")
  const [date, setDate] = useState(new Date().toISOString().split("T")[0])

  if (!isOpen) return null

  const handleSubmit = (e: React.FormEvent) => {
    e.preventDefault()
    onSubmit({
      description,
      amount: parseFloat(amount),
      type,
      source,
      date,
    })
    setDescription("")
    setAmount("")
    setType("")
    setSource("")
    setDate(new Date().toISOString().split("T")[0])
    onClose()
  }

  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center">
      <div
        className="absolute inset-0 bg-background/80 backdrop-blur-sm"
        onClick={onClose}
      />

      <div className="relative w-full max-w-md rounded-xl bg-card border border-border p-6 shadow-xl mx-4">
        <div className="flex items-center justify-between mb-6">
          <h2 className="text-xl font-semibold text-foreground">Agregar Ingreso</h2>
          <button
            onClick={onClose}
            className="rounded-lg p-1.5 text-muted-foreground hover:bg-secondary hover:text-foreground transition-smooth"
          >
            <X className="h-5 w-5" />
          </button>
        </div>

        <form onSubmit={handleSubmit} className="space-y-4">
          <div className="space-y-2">
            <Label htmlFor="description">Descripcion</Label>
            <Input
              id="description"
              value={description}
              onChange={(e) => setDescription(e.target.value)}
              placeholder="Ej: Salario quincenal"
              required
              className="bg-secondary border-border"
            />
          </div>

          <div className="space-y-2">
            <Label htmlFor="amount">Monto</Label>
            <Input
              id="amount"
              type="number"
              value={amount}
              onChange={(e) => setAmount(e.target.value)}
              placeholder="0.00"
              required
              min="0"
              step="0.01"
              className="bg-secondary border-border"
            />
          </div>

          <div className="space-y-2">
            <Label>Tipo de Ingreso</Label>
            <Select value={type} onValueChange={setType} required>
              <SelectTrigger className="bg-secondary border-border">
                <SelectValue placeholder="Fijo o Variable" />
              </SelectTrigger>
              <SelectContent>
                {incomeTypes.map((t) => (
                  <SelectItem key={t} value={t}>
                    {t}
                  </SelectItem>
                ))}
              </SelectContent>
            </Select>
          </div>

          <div className="space-y-2">
            <Label>Fuente</Label>
            <Select value={source} onValueChange={setSource} required>
              <SelectTrigger className="bg-secondary border-border">
                <SelectValue placeholder="Selecciona la fuente" />
              </SelectTrigger>
              <SelectContent>
                {incomeSources.map((s) => (
                  <SelectItem key={s} value={s}>
                    {s}
                  </SelectItem>
                ))}
              </SelectContent>
            </Select>
          </div>

          <div className="space-y-2">
            <Label htmlFor="date">Fecha</Label>
            <Input
              id="date"
              type="date"
              value={date}
              onChange={(e) => setDate(e.target.value)}
              required
              className="bg-secondary border-border"
            />
          </div>

          <div className="flex gap-3 pt-4">
            <Button
              type="button"
              variant="outline"
              onClick={onClose}
              className="flex-1"
            >
              Cancelar
            </Button>
            <Button type="submit" className="flex-1 bg-success text-success-foreground hover:bg-success/90">
              Agregar Ingreso
            </Button>
          </div>
        </form>
      </div>
    </div>
  )
}
