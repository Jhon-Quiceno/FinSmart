"use client"

import { formatDistanceToNow } from "date-fns"
import { es } from "date-fns/locale"
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
import { useMarkAllAsRead, useMarkAsRead, useNotifications, useUnreadCount } from "@/hooks/use-notifications"
import type { Notification, NotificationType } from "@/lib/types/notification"
import Link from "next/link"

interface NavbarProps {
  onMobileMenuToggle: () => void
  isMobileMenuOpen: boolean
}

const NOTIFICATION_TYPE_DOT_CLASS: Record<NotificationType, string> = {
  PAYMENT_REMINDER: "bg-warning",
  OVERSPEND_ALERT: "bg-destructive",
  WEEKLY_SUMMARY: "bg-primary",
  INACTIVITY_REMINDER: "bg-secondary-foreground",
  MONTH_END_PREDICTION: "bg-primary",
  SYSTEM: "bg-muted-foreground",
}

function formatRelativeTime(isoDate: string): string {
  try {
    return formatDistanceToNow(new Date(isoDate), { addSuffix: true, locale: es })
  } catch {
    return ""
  }
}

export function Navbar({ onMobileMenuToggle, isMobileMenuOpen }: NavbarProps) {
  const { unreadCount } = useUnreadCount()
  const { notifications: notificationsPage } = useNotifications({ size: 5 })
  const { markAsRead } = useMarkAsRead()
  const { markAllAsRead, isLoading: isMarkingAllAsRead } = useMarkAllAsRead()
  const notifications = notificationsPage.content
  const { user, logout } = useAuth()

  const handleNotificationClick = (notification: Notification) => {
    if (!notification.read) {
      void markAsRead(notification.id)
    }
  }

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
              {unreadCount > 0 && (
                <span className="absolute -top-1 -right-1 flex h-4 min-w-4 items-center justify-center rounded-full bg-destructive px-1 text-[10px] font-medium text-white">
                  {unreadCount > 9 ? "9+" : unreadCount}
                </span>
              )}
            </Button>
          </DropdownMenuTrigger>
          <DropdownMenuContent align="end" className="w-80">
            <DropdownMenuLabel className="flex items-center justify-between">
              <span>Notificaciones</span>
              <button
                onClick={() => void markAllAsRead()}
                disabled={isMarkingAllAsRead || unreadCount === 0}
                className="text-xs text-muted-foreground hover:text-foreground disabled:opacity-50 disabled:cursor-not-allowed"
              >
                Marcar todas como leidas
              </button>
            </DropdownMenuLabel>
            <DropdownMenuSeparator />
            {notifications.length === 0 ? (
              <div className="p-4 text-center text-sm text-muted-foreground">
                No tenes notificaciones por el momento.
              </div>
            ) : (
              notifications.map((notification) => (
                <DropdownMenuItem
                  key={notification.id}
                  onClick={() => handleNotificationClick(notification)}
                  className={cn("flex flex-col items-start gap-1 p-3", !notification.read && "bg-accent/50")}
                >
                  <div className="flex w-full items-center gap-2">
                    <span className={cn("h-2 w-2 shrink-0 rounded-full", NOTIFICATION_TYPE_DOT_CLASS[notification.type])} />
                    <span className={cn("flex-1 truncate text-sm", !notification.read ? "font-semibold" : "font-medium")}>
                      {notification.title}
                    </span>
                  </div>
                  <span className="text-xs text-muted-foreground ml-4 line-clamp-2">{notification.message}</span>
                  <span className="text-[10px] text-muted-foreground/70 ml-4">
                    {formatRelativeTime(notification.createdAt)}
                  </span>
                </DropdownMenuItem>
              ))
            )}
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
