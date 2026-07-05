"use client"

import { AppLayout } from "@/components/layout/app-layout"
import { NotificationPreferencesCard } from "@/components/settings/notification-preferences-card"
import { Badge } from "@/components/ui/badge"
import { Button } from "@/components/ui/button"
import { Input } from "@/components/ui/input"
import { Label } from "@/components/ui/label"
import { Globe, Palette, Save, Shield, User } from "lucide-react"

export default function ConfiguracionPage() {
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

        {/* Profile Section (static — edicion de perfil llega en un proximo sprint) */}
        <div className="rounded-xl bg-card border border-border p-6">
          <div className="flex items-center gap-3 mb-6">
            <div className="flex h-10 w-10 items-center justify-center rounded-lg bg-primary/10">
              <User className="h-5 w-5 text-primary" />
            </div>
            <div>
              <div className="flex items-center gap-2">
                <h2 className="text-lg font-semibold text-foreground">Perfil</h2>
                <Badge variant="secondary">Proximamente</Badge>
              </div>
              <p className="text-sm text-muted-foreground">Informacion personal</p>
            </div>
          </div>

          <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
            <div className="space-y-2">
              <Label htmlFor="name">Nombre completo</Label>
              <Input id="name" defaultValue="Jhon Quiceno" disabled className="bg-secondary border-border" />
            </div>
            <div className="space-y-2">
              <Label htmlFor="email">Correo electronico</Label>
              <Input id="email" type="email" defaultValue="jhon@ejemplo.com" disabled className="bg-secondary border-border" />
            </div>
          </div>

          <Button className="mt-4" disabled title="Disponible en un proximo sprint">
            <Save className="h-4 w-4 mr-2" />
            Guardar cambios
          </Button>
        </div>

        {/* Notifications Section */}
        <NotificationPreferencesCard />

        {/* Security Section (static — cambio de contrasena llega en un proximo sprint) */}
        <div className="rounded-xl bg-card border border-border p-6">
          <div className="flex items-center gap-3 mb-6">
            <div className="flex h-10 w-10 items-center justify-center rounded-lg bg-destructive/10">
              <Shield className="h-5 w-5 text-destructive" />
            </div>
            <div>
              <div className="flex items-center gap-2">
                <h2 className="text-lg font-semibold text-foreground">Seguridad</h2>
                <Badge variant="secondary">Proximamente</Badge>
              </div>
              <p className="text-sm text-muted-foreground">Protege tu cuenta</p>
            </div>
          </div>

          <div className="space-y-4">
            <div className="space-y-2">
              <Label htmlFor="current-password">Contrasena actual</Label>
              <Input id="current-password" type="password" placeholder="********" disabled className="bg-secondary border-border" />
            </div>
            <div className="space-y-2">
              <Label htmlFor="new-password">Nueva contrasena</Label>
              <Input id="new-password" type="password" placeholder="********" disabled className="bg-secondary border-border" />
            </div>
          </div>

          <Button className="mt-4" variant="outline" disabled title="Disponible en un proximo sprint">
            Cambiar contrasena
          </Button>
        </div>

        {/* Preferences Section (static) */}
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
                <span className="text-sm text-foreground">Espanol</span>
              </div>
            </div>
          </div>
        </div>
      </div>
    </AppLayout>
  )
}
