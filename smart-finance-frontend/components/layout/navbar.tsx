"use client"

import { useState } from "react"
import { Bell, User, Menu, X, LogOut, Settings, UserCircle } from "lucide-react"
import { Button } from "@/components/ui/button"
import {
  DropdownMenu,
  DropdownMenuContent,
  DropdownMenuItem,
  DropdownMenuLabel,
  DropdownMenuSeparator,
  DropdownMenuTrigger,
} from "@/components/ui/dropdown-menu"
import { cn } from "@/lib/utils"
import { useAuth } from "@/contexts/auth-context"
import Link from "next/link"

interface NavbarProps {
  onMobileMenuToggle: () => void
  isMobileMenuOpen: boolean
}

const notifications = [
  { id: 1, title: "Pago de Netflix pendiente", description: "Vence en 3 dias", type: "warning" },
  { id: 2, title: "Meta de ahorro alcanzada", description: "Has ahorrado $500 este mes", type: "success" },
  { id: 3, title: "Gasto inusual detectado", description: "Gasto alto en restaurantes", type: "alert" },
]

export function Navbar({ onMobileMenuToggle, isMobileMenuOpen }: NavbarProps) {
  const [hasUnread, setHasUnread] = useState(true)
  const { user, logout } = useAuth()

  return (
    <header className="sticky top-0 z-30 flex h-16 items-center justify-between border-b border-border bg-background/95 backdrop-blur supports-[backdrop-filter]:bg-background/60 px-4 lg:px-6">
      {/* Mobile Menu Button */}
      <button
        onClick={onMobileMenuToggle}
        className="lg:hidden flex items-center justify-center rounded-lg p-2 text-muted-foreground hover:bg-accent hover:text-foreground transition-smooth"
      >
        {isMobileMenuOpen ? <X className="h-5 w-5" /> : <Menu className="h-5 w-5" />}
      </button>

      {/* Page Title - Hidden on mobile */}
      <div className="hidden lg:flex items-center gap-2">
        <h1 className="text-lg font-semibold text-foreground">
          Plataforma de Gestion Financiera
        </h1>
      </div>

      {/* Right side actions */}
      <div className="flex items-center gap-2">
        {/* Notifications */}
        <DropdownMenu>
          <DropdownMenuTrigger asChild>
            <Button variant="ghost" size="icon" className="relative">
              <Bell className="h-5 w-5 text-muted-foreground" />
              {hasUnread && (
                <span className="absolute top-1.5 right-1.5 h-2 w-2 rounded-full bg-warning" />
              )}
            </Button>
          </DropdownMenuTrigger>
          <DropdownMenuContent align="end" className="w-80">
            <DropdownMenuLabel className="flex items-center justify-between">
              <span>Notificaciones</span>
              <button 
                onClick={() => setHasUnread(false)}
                className="text-xs text-muted-foreground hover:text-foreground"
              >
                Marcar leidas
              </button>
            </DropdownMenuLabel>
            <DropdownMenuSeparator />
            {notifications.map((notification) => (
              <DropdownMenuItem key={notification.id} className="flex flex-col items-start gap-1 p-3">
                <div className="flex items-center gap-2">
                  <span className={cn(
                    "h-2 w-2 rounded-full",
                    notification.type === "warning" && "bg-warning",
                    notification.type === "success" && "bg-success",
                    notification.type === "alert" && "bg-destructive"
                  )} />
                  <span className="font-medium text-sm">{notification.title}</span>
                </div>
                <span className="text-xs text-muted-foreground ml-4">{notification.description}</span>
              </DropdownMenuItem>
            ))}
          </DropdownMenuContent>
        </DropdownMenu>

        {/* User Menu */}
        <DropdownMenu>
          <DropdownMenuTrigger asChild>
            <Button variant="ghost" className="flex items-center gap-2 px-2">
              <div className="flex h-8 w-8 items-center justify-center rounded-full bg-primary/20">
                <User className="h-4 w-4 text-primary" />
              </div>
              <span className="hidden md:block text-sm font-medium">
                {user?.name || "Usuario"}
              </span>
            </Button>
          </DropdownMenuTrigger>
          <DropdownMenuContent align="end" className="w-56">
            <DropdownMenuLabel className="font-normal">
              <div className="flex flex-col space-y-1">
                <p className="text-sm font-medium">{user?.name}</p>
                <p className="text-xs text-muted-foreground">{user?.email}</p>
              </div>
            </DropdownMenuLabel>
            <DropdownMenuSeparator />
            <DropdownMenuItem asChild>
              <Link href="/configuracion" className="flex items-center gap-2 cursor-pointer">
                <UserCircle className="h-4 w-4" />
                Perfil
              </Link>
            </DropdownMenuItem>
            <DropdownMenuItem asChild>
              <Link href="/configuracion" className="flex items-center gap-2 cursor-pointer">
                <Settings className="h-4 w-4" />
                Preferencias
              </Link>
            </DropdownMenuItem>
            <DropdownMenuSeparator />
            <DropdownMenuItem 
              onClick={logout}
              className="text-destructive focus:text-destructive cursor-pointer"
            >
              <LogOut className="h-4 w-4 mr-2" />
              Cerrar Sesion
            </DropdownMenuItem>
          </DropdownMenuContent>
        </DropdownMenu>
      </div>
    </header>
  )
}
