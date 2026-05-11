"use client"

import { useState } from "react"
import { Plus, Repeat, AlertTriangle, CheckCircle, Clock } from "lucide-react"
import { AppLayout } from "@/components/layout/app-layout"
import { Button } from "@/components/ui/button"
import { ServiceCard, type Service } from "@/components/services/service-card"

const initialServices: Service[] = [
  { id: 1, name: "Netflix", amount: 299, paymentDate: "25 Mar", daysUntilDue: 5, category: "Streaming", status: "upcoming" },
  { id: 2, name: "CFE (Luz)", amount: 520, paymentDate: "20 Mar", daysUntilDue: 0, category: "Luz", status: "pending" },
  { id: 3, name: "Agua SAPAL", amount: 180, paymentDate: "28 Mar", daysUntilDue: 8, category: "Agua", status: "upcoming" },
  { id: 4, name: "Telmex Internet", amount: 599, paymentDate: "15 Mar", daysUntilDue: -5, category: "Internet", status: "paid" },
  { id: 5, name: "Spotify Premium", amount: 129, paymentDate: "22 Mar", daysUntilDue: 2, category: "Musica", status: "pending" },
  { id: 6, name: "Disney+", amount: 179, paymentDate: "01 Abr", daysUntilDue: 12, category: "Streaming", status: "upcoming" },
  { id: 7, name: "Telcel", amount: 399, paymentDate: "10 Mar", daysUntilDue: -10, category: "Telefono", status: "paid" },
  { id: 8, name: "iCloud", amount: 49, paymentDate: "05 Abr", daysUntilDue: 16, category: "Cloud", status: "upcoming" },
  { id: 9, name: "Amazon Prime", amount: 99, paymentDate: "18 Mar", daysUntilDue: -2, category: "Streaming", status: "paid" },
  { id: 10, name: "Gas Natural", amount: 350, paymentDate: "23 Mar", daysUntilDue: 3, category: "Otros", status: "pending" },
]

export default function ServiciosPage() {
  const [services] = useState<Service[]>(initialServices)

  const totalMonthly = services.reduce((sum, s) => sum + s.amount, 0)
  const pendingServices = services.filter(s => s.status === "pending" || s.status === "upcoming")
  const paidServices = services.filter(s => s.status === "paid")
  const urgentServices = services.filter(s => s.status !== "paid" && s.daysUntilDue <= 3)

  return (
    <AppLayout>
      <div className="space-y-6">
        {/* Header */}
        <div className="flex flex-col sm:flex-row sm:items-center sm:justify-between gap-4">
          <div>
            <h1 className="text-2xl font-bold text-foreground">Servicios y Suscripciones</h1>
            <p className="text-sm text-muted-foreground">
              Administra tus pagos recurrentes
            </p>
          </div>
          <Button className="bg-primary text-primary-foreground hover:bg-primary/90">
            <Plus className="h-4 w-4 mr-2" />
            Agregar Servicio
          </Button>
        </div>

        {/* Summary Cards */}
        <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-4 gap-4">
          <div className="rounded-xl bg-card border border-border p-5">
            <div className="flex items-center gap-3">
              <div className="flex h-10 w-10 items-center justify-center rounded-lg bg-primary/10">
                <Repeat className="h-5 w-5 text-primary" />
              </div>
              <div>
                <p className="text-sm text-muted-foreground">Total Mensual</p>
                <p className="text-xl font-bold text-foreground">
                  ${totalMonthly.toLocaleString("es-MX")}
                </p>
              </div>
            </div>
          </div>

          <div className="rounded-xl bg-card border border-border p-5">
            <div className="flex items-center gap-3">
              <div className="flex h-10 w-10 items-center justify-center rounded-lg bg-warning/10">
                <Clock className="h-5 w-5 text-warning" />
              </div>
              <div>
                <p className="text-sm text-muted-foreground">Pendientes</p>
                <p className="text-xl font-bold text-warning">{pendingServices.length}</p>
              </div>
            </div>
          </div>

          <div className="rounded-xl bg-card border border-border p-5">
            <div className="flex items-center gap-3">
              <div className="flex h-10 w-10 items-center justify-center rounded-lg bg-destructive/10">
                <AlertTriangle className="h-5 w-5 text-destructive" />
              </div>
              <div>
                <p className="text-sm text-muted-foreground">Urgentes</p>
                <p className="text-xl font-bold text-destructive">{urgentServices.length}</p>
              </div>
            </div>
          </div>

          <div className="rounded-xl bg-card border border-border p-5">
            <div className="flex items-center gap-3">
              <div className="flex h-10 w-10 items-center justify-center rounded-lg bg-success/10">
                <CheckCircle className="h-5 w-5 text-success" />
              </div>
              <div>
                <p className="text-sm text-muted-foreground">Pagados</p>
                <p className="text-xl font-bold text-success">{paidServices.length}</p>
              </div>
            </div>
          </div>
        </div>

        {/* Urgent Services Alert */}
        {urgentServices.length > 0 && (
          <div className="rounded-xl border border-destructive/30 bg-destructive/5 p-4">
            <div className="flex items-center gap-3">
              <AlertTriangle className="h-5 w-5 text-destructive shrink-0" />
              <div>
                <p className="text-sm font-medium text-destructive">
                  Tienes {urgentServices.length} servicio{urgentServices.length > 1 ? "s" : ""} por vencer en los proximos 3 dias
                </p>
                <p className="text-xs text-muted-foreground mt-1">
                  {urgentServices.map(s => s.name).join(", ")}
                </p>
              </div>
            </div>
          </div>
        )}

        {/* Services Grid */}
        <div>
          <h2 className="text-lg font-semibold text-foreground mb-4">Todos los Servicios</h2>
          <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-4">
            {services
              .sort((a, b) => {
                if (a.status === "paid" && b.status !== "paid") return 1
                if (a.status !== "paid" && b.status === "paid") return -1
                return a.daysUntilDue - b.daysUntilDue
              })
              .map((service) => (
                <ServiceCard key={service.id} service={service} />
              ))}
          </div>
        </div>
      </div>
    </AppLayout>
  )
}
