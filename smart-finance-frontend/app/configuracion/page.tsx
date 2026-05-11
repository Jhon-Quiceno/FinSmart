"use client"

import { useState } from "react"
import { AppLayout } from "@/components/layout/app-layout"
import { Button } from "@/components/ui/button"
import { Input } from "@/components/ui/input"
import { Label } from "@/components/ui/label"
import { Switch } from "@/components/ui/switch"
import { User, Bell, Shield, Palette, Globe, Save } from "lucide-react"

export default function ConfiguracionPage() {
  const [notifications, setNotifications] = useState({
    paymentReminders: true,
    weeklyReports: true,
    aiRecommendations: true,
    budgetAlerts: true,
  })

  return (
    <AppLayout>
      <div className="space-y-6 max-w-4xl">
        {/* Header */}
        <div>
          <h1 className="text-2xl font-bold text-foreground">Configuracion</h1>
          <p className="text-sm text-muted-foreground">
            Personaliza tu experiencia en la plataforma
          </p>
        </div>

        {/* Profile Section */}
        <div className="rounded-xl bg-card border border-border p-6">
          <div className="flex items-center gap-3 mb-6">
            <div className="flex h-10 w-10 items-center justify-center rounded-lg bg-primary/10">
              <User className="h-5 w-5 text-primary" />
            </div>
            <div>
              <h2 className="text-lg font-semibold text-foreground">Perfil</h2>
              <p className="text-sm text-muted-foreground">Informacion personal</p>
            </div>
          </div>

          <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
            <div className="space-y-2">
              <Label htmlFor="name">Nombre completo</Label>
              <Input id="name" defaultValue="Juan Perez" className="bg-secondary border-border" />
            </div>
            <div className="space-y-2">
              <Label htmlFor="email">Correo electronico</Label>
              <Input id="email" type="email" defaultValue="juan@ejemplo.com" className="bg-secondary border-border" />
            </div>
            <div className="space-y-2">
              <Label htmlFor="phone">Telefono</Label>
              <Input id="phone" defaultValue="+52 55 1234 5678" className="bg-secondary border-border" />
            </div>
            <div className="space-y-2">
              <Label htmlFor="currency">Moneda</Label>
              <Input id="currency" defaultValue="MXN - Peso Mexicano" className="bg-secondary border-border" />
            </div>
          </div>

          <Button className="mt-4 bg-primary text-primary-foreground hover:bg-primary/90">
            <Save className="h-4 w-4 mr-2" />
            Guardar cambios
          </Button>
        </div>

        {/* Notifications Section */}
        <div className="rounded-xl bg-card border border-border p-6">
          <div className="flex items-center gap-3 mb-6">
            <div className="flex h-10 w-10 items-center justify-center rounded-lg bg-warning/10">
              <Bell className="h-5 w-5 text-warning" />
            </div>
            <div>
              <h2 className="text-lg font-semibold text-foreground">Notificaciones</h2>
              <p className="text-sm text-muted-foreground">Configura tus alertas</p>
            </div>
          </div>

          <div className="space-y-4">
            <div className="flex items-center justify-between p-4 rounded-lg bg-secondary/50">
              <div>
                <p className="text-sm font-medium text-foreground">Recordatorios de pago</p>
                <p className="text-xs text-muted-foreground">Recibe alertas antes del vencimiento</p>
              </div>
              <Switch
                checked={notifications.paymentReminders}
                onCheckedChange={(checked) => setNotifications({...notifications, paymentReminders: checked})}
              />
            </div>

            <div className="flex items-center justify-between p-4 rounded-lg bg-secondary/50">
              <div>
                <p className="text-sm font-medium text-foreground">Reportes semanales</p>
                <p className="text-xs text-muted-foreground">Resumen de tu actividad financiera</p>
              </div>
              <Switch
                checked={notifications.weeklyReports}
                onCheckedChange={(checked) => setNotifications({...notifications, weeklyReports: checked})}
              />
            </div>

            <div className="flex items-center justify-between p-4 rounded-lg bg-secondary/50">
              <div>
                <p className="text-sm font-medium text-foreground">Recomendaciones IA</p>
                <p className="text-xs text-muted-foreground">Sugerencias personalizadas</p>
              </div>
              <Switch
                checked={notifications.aiRecommendations}
                onCheckedChange={(checked) => setNotifications({...notifications, aiRecommendations: checked})}
              />
            </div>

            <div className="flex items-center justify-between p-4 rounded-lg bg-secondary/50">
              <div>
                <p className="text-sm font-medium text-foreground">Alertas de presupuesto</p>
                <p className="text-xs text-muted-foreground">Aviso al exceder limites</p>
              </div>
              <Switch
                checked={notifications.budgetAlerts}
                onCheckedChange={(checked) => setNotifications({...notifications, budgetAlerts: checked})}
              />
            </div>
          </div>
        </div>

        {/* Security Section */}
        <div className="rounded-xl bg-card border border-border p-6">
          <div className="flex items-center gap-3 mb-6">
            <div className="flex h-10 w-10 items-center justify-center rounded-lg bg-destructive/10">
              <Shield className="h-5 w-5 text-destructive" />
            </div>
            <div>
              <h2 className="text-lg font-semibold text-foreground">Seguridad</h2>
              <p className="text-sm text-muted-foreground">Protege tu cuenta</p>
            </div>
          </div>

          <div className="space-y-4">
            <div className="space-y-2">
              <Label htmlFor="current-password">Contrasena actual</Label>
              <Input id="current-password" type="password" placeholder="********" className="bg-secondary border-border" />
            </div>
            <div className="space-y-2">
              <Label htmlFor="new-password">Nueva contrasena</Label>
              <Input id="new-password" type="password" placeholder="********" className="bg-secondary border-border" />
            </div>
            <div className="space-y-2">
              <Label htmlFor="confirm-password">Confirmar contrasena</Label>
              <Input id="confirm-password" type="password" placeholder="********" className="bg-secondary border-border" />
            </div>
          </div>

          <Button className="mt-4" variant="outline">
            Cambiar contrasena
          </Button>
        </div>

        {/* Preferences Section */}
        <div className="rounded-xl bg-card border border-border p-6">
          <div className="flex items-center gap-3 mb-6">
            <div className="flex h-10 w-10 items-center justify-center rounded-lg bg-primary/10">
              <Palette className="h-5 w-5 text-primary" />
            </div>
            <div>
              <h2 className="text-lg font-semibold text-foreground">Preferencias</h2>
              <p className="text-sm text-muted-foreground">Personaliza la aplicacion</p>
            </div>
          </div>

          <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
            <div className="space-y-2">
              <Label>Tema</Label>
              <div className="flex gap-2">
                <button className="flex-1 p-3 rounded-lg bg-secondary border-2 border-primary text-sm font-medium text-foreground">
                  Oscuro
                </button>
                <button className="flex-1 p-3 rounded-lg bg-secondary border border-border text-sm font-medium text-muted-foreground hover:border-primary/50 transition-smooth">
                  Claro
                </button>
              </div>
            </div>
            <div className="space-y-2">
              <Label>Idioma</Label>
              <div className="flex items-center gap-2 p-3 rounded-lg bg-secondary border border-border">
                <Globe className="h-4 w-4 text-muted-foreground" />
                <span className="text-sm text-foreground">Espanol (MX)</span>
              </div>
            </div>
          </div>
        </div>

        {/* Danger Zone */}
        <div className="rounded-xl bg-card border border-destructive/30 p-6">
          <h2 className="text-lg font-semibold text-destructive mb-2">Zona de peligro</h2>
          <p className="text-sm text-muted-foreground mb-4">
            Estas acciones son irreversibles. Procede con precaucion.
          </p>
          <div className="flex gap-3">
            <Button variant="outline" className="border-destructive/30 text-destructive hover:bg-destructive/10">
              Exportar datos
            </Button>
            <Button variant="outline" className="border-destructive/30 text-destructive hover:bg-destructive/10">
              Eliminar cuenta
            </Button>
          </div>
        </div>
      </div>
    </AppLayout>
  )
}
