"use client"

import { useEffect, useState } from "react"
import { useTheme } from "next-themes"
import { AppLayout } from "@/components/layout/app-layout"
import { NotificationPreferencesCard } from "@/components/settings/notification-preferences-card"
import { PasswordCard } from "@/components/settings/password-card"
import { ProfileCard } from "@/components/settings/profile-card"
import { Label } from "@/components/ui/label"
import { Globe, Palette } from "lucide-react"

export default function ConfiguracionPage() {
  const { theme, setTheme } = useTheme()
  const [mounted, setMounted] = useState(false)

  // Theme is only known after mount (next-themes reads it from localStorage on the client), so we
  // avoid rendering the active/inactive button state until then to prevent a hydration mismatch.
  useEffect(() => {
    // eslint-disable-next-line react-hooks/set-state-in-effect -- standard next-themes mount-detection pattern
    setMounted(true)
  }, [])

  const isDark = mounted ? theme === "dark" : true

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

        <ProfileCard />

        <NotificationPreferencesCard />

        <PasswordCard />

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
                <button
                  type="button"
                  onClick={() => setTheme("dark")}
                  className={`flex-1 p-3 rounded-lg border-2 text-sm font-medium transition-smooth ${
                    isDark
                      ? "bg-secondary border-primary text-foreground"
                      : "bg-secondary border-border text-muted-foreground hover:border-primary/50"
                  }`}
                >
                  Oscuro
                </button>
                <button
                  type="button"
                  onClick={() => setTheme("light")}
                  className={`flex-1 p-3 rounded-lg border-2 text-sm font-medium transition-smooth ${
                    !isDark
                      ? "bg-secondary border-primary text-foreground"
                      : "bg-secondary border-border text-muted-foreground hover:border-primary/50"
                  }`}
                >
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
