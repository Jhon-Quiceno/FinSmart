"use client"

import { useState } from "react"
import { QuickAddCommand } from "@/components/quick-add/quick-add-command"
import { QuickAddFab } from "@/components/quick-add/quick-add-fab"
import { QuickAddProvider } from "@/contexts/quick-add-context"
import { cn } from "@/lib/utils"
import { Sidebar } from "./sidebar"
import { Navbar } from "./navbar"
import { MobileSidebar } from "./mobile-sidebar"

interface AppLayoutProps {
  children: React.ReactNode
}

export function AppLayout({ children }: AppLayoutProps) {
  const [isMobileMenuOpen, setIsMobileMenuOpen] = useState(false)
  const [isSidebarCollapsed, setIsSidebarCollapsed] = useState(false)

  return (
    <QuickAddProvider>
      <div className="min-h-screen bg-background">
        {/* Desktop Sidebar */}
        <div className="hidden lg:block">
          <Sidebar collapsed={isSidebarCollapsed} onCollapsedChange={setIsSidebarCollapsed} />
        </div>

        {/* Mobile Sidebar */}
        <MobileSidebar
          isOpen={isMobileMenuOpen}
          onClose={() => setIsMobileMenuOpen(false)}
        />

        {/* Main Content */}
        <div className={cn("transition-all duration-300", isSidebarCollapsed ? "lg:pl-16" : "lg:pl-64")}>
          <Navbar
            onMobileMenuToggle={() => setIsMobileMenuOpen(!isMobileMenuOpen)}
            isMobileMenuOpen={isMobileMenuOpen}
          />
          <main className="p-4 lg:p-6">
            {children}
          </main>
        </div>

        {/* Quick-add: FAB + Ctrl+K / Cmd+K dialog, available on every authenticated page */}
        <QuickAddFab />
        <QuickAddCommand />
      </div>
    </QuickAddProvider>
  )
}
