"use client"

import { useState } from "react"
import Image from "next/image"
import Link from "next/link"
import { usePathname } from "next/navigation"
import { cn } from "@/lib/utils"
import {
  LayoutDashboard,
  TrendingUp,
  TrendingDown,
  Tags,
  CreditCard,
  Repeat,
  BarChart3,
  Settings,
  ChevronLeft,
  ChevronRight,
  Bot,
} from "lucide-react"

const navigation = [
  { name: "Dashboard", href: "/", icon: LayoutDashboard },
  { name: "Ingresos", href: "/ingresos", icon: TrendingUp },
  { name: "Gastos", href: "/gastos", icon: TrendingDown },
  { name: "Categorias", href: "/categorias", icon: Tags },
  { name: "Deudas", href: "/deudas", icon: CreditCard },
  { name: "Servicios", href: "/servicios", icon: Repeat },
  { name: "Asistente IA", href: "/asistente-ia", icon: Bot },
  { name: "Reportes", href: "/reportes", icon: BarChart3 },
  { name: "Configuracion", href: "/configuracion", icon: Settings },
]

export function Sidebar() {
  const [collapsed, setCollapsed] = useState(false)
  const pathname = usePathname()

  return (
    <aside
      className={cn(
        "fixed left-0 top-0 z-40 h-screen bg-sidebar border-r border-sidebar-border transition-all duration-300 ease-in-out",
        collapsed ? "w-16" : "w-64"
      )}
    >
      <div className="flex h-full flex-col">
        {/* Logo */}
        <div className="flex h-16 items-center justify-between px-4 border-b border-sidebar-border">
          {!collapsed && (
            <Link href="/" className="flex items-center gap-2">
              <div className="flex h-8 w-8 items-center justify-center shrink-0">
                <Image
                  src="/logo_finsmart.svg"
                  alt="FinSmart"
                  width={32}
                  height={32}
                  className="h-8 w-8"
                  priority
                />
              </div>
              <span className="text-lg font-semibold text-sidebar-foreground">
                FinSmart
              </span>
            </Link>
          )}
          {collapsed && (
            <div className="flex items-center justify-center mx-auto">
              <Image
                src="/logo_finsmart.svg"
                alt="FinSmart"
                width={32}
                height={32}
                className="h-8 w-8"
                priority
              />
            </div>
          )}
        </div>

        {/* Navigation */}
        <nav className="flex-1 px-3 py-4 space-y-1">
          {navigation.map((item) => {
            const isActive = pathname === item.href
            return (
              <Link
                key={item.name}
                href={item.href}
                className={cn(
                  "flex items-center gap-3 rounded-lg px-3 py-2.5 text-sm font-medium transition-smooth",
                  isActive
                    ? "bg-sidebar-accent text-sidebar-primary"
                    : "text-sidebar-foreground/70 hover:bg-sidebar-accent hover:text-sidebar-foreground"
                )}
              >
                <item.icon className={cn("h-5 w-5 shrink-0", isActive && "text-sidebar-primary")} />
                {!collapsed && <span>{item.name}</span>}
              </Link>
            )
          })}
        </nav>

        {/* Collapse Toggle */}
        <div className="p-3 border-t border-sidebar-border">
          <button
            onClick={() => setCollapsed(!collapsed)}
            className="flex w-full items-center justify-center rounded-lg p-2 text-sidebar-foreground/70 hover:bg-sidebar-accent hover:text-sidebar-foreground transition-smooth"
          >
            {collapsed ? (
              <ChevronRight className="h-5 w-5" />
            ) : (
              <ChevronLeft className="h-5 w-5" />
            )}
          </button>
        </div>
      </div>
    </aside>
  )
}
