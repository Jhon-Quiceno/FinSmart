"use client"

import { createContext, ReactNode, useContext, useEffect, useState } from "react"
import { usePathname, useRouter } from "next/navigation"
import { clearAccessToken, getApiErrorMessage } from "@/lib/api-client"
import { loginRequest, logoutRequest, refreshRequest, registerRequest } from "@/lib/services/auth.service"
import { Spinner } from "@/components/ui/spinner"

interface User {
  id: string
  name: string
  email: string
  avatar?: string
}

interface AuthActionResult {
  success: boolean
  error?: string
}

interface AuthContextType {
  user: User | null
  isLoading: boolean
  login: (email: string, password: string) => Promise<AuthActionResult>
  register: (name: string, email: string, password: string) => Promise<AuthActionResult>
  logout: () => Promise<void>
  isAuthenticated: boolean
  /** Updates the in-memory user and its localStorage snapshot after a successful profile edit. */
  updateUser: (nextUser: { name: string; email: string }) => void
}

const AuthContext = createContext<AuthContextType | undefined>(undefined)

const STORAGE_USER_KEY = "financeai_user"
const PUBLIC_ROUTES = ["/login", "/registro", "/recuperar-password"]

export function AuthProvider({ children }: { children: ReactNode }) {
  const [user, setUser] = useState<User | null>(null)
  const [isLoading, setIsLoading] = useState(true)
  const router = useRouter()
  const pathname = usePathname()

  const clearSession = () => {
    setUser(null)
    window.localStorage.removeItem(STORAGE_USER_KEY)
    clearAccessToken()
  }

  const persistSession = (nextUser: User) => {
    setUser(nextUser)
    window.localStorage.setItem(STORAGE_USER_KEY, JSON.stringify(nextUser))
  }

  useEffect(() => {
    const bootstrapSession = async () => {
      const storedUser = window.localStorage.getItem(STORAGE_USER_KEY)
      if (!storedUser) {
        clearSession()
        setIsLoading(false)
        return
      }

      try {
        JSON.parse(storedUser) as User
        const refreshResponse = await refreshRequest()
        persistSession({
          id: String(refreshResponse.user.id),
          name: refreshResponse.user.name,
          email: refreshResponse.user.email,
        })
      } catch {
        clearSession()
      } finally {
        setIsLoading(false)
      }
    }

    void bootstrapSession()
  }, [])

  useEffect(() => {
    if (isLoading) return

    const isPublicRoute = PUBLIC_ROUTES.includes(pathname)

    if (!user && !isPublicRoute) {
      router.push("/login")
      return
    }

    if (user && isPublicRoute) {
      router.push("/")
    }
  }, [isLoading, pathname, router, user])

  const isPublicRoute = PUBLIC_ROUTES.includes(pathname)
  const shouldRenderChildren = isPublicRoute ? !user : !isLoading && !!user

  const login = async (email: string, password: string): Promise<AuthActionResult> => {
    setIsLoading(true)

    try {
      const response = await loginRequest(email, password)
      persistSession({
        id: String(response.user.id),
        name: response.user.name,
        email: response.user.email,
      })
      return { success: true }
    } catch (error) {
      clearSession()
      return {
        success: false,
        error: getApiErrorMessage(error, "No fue posible iniciar sesion"),
      }
    } finally {
      setIsLoading(false)
    }
  }

  const register = async (name: string, email: string, password: string): Promise<AuthActionResult> => {
    setIsLoading(true)

    try {
      const response = await registerRequest(name, email, password)
      persistSession({
        id: String(response.user.id),
        name: response.user.name,
        email: response.user.email,
      })
      return { success: true }
    } catch (error) {
      clearSession()
      return {
        success: false,
        error: getApiErrorMessage(error, "No fue posible crear la cuenta"),
      }
    } finally {
      setIsLoading(false)
    }
  }

  const logout = async () => {
    try {
      await logoutRequest()
    } catch {
      // Keep logout resilient on client side.
    } finally {
      clearSession()
      router.push("/login")
    }
  }

  const updateUser = (nextUser: { name: string; email: string }) => {
    if (!user) return
    persistSession({ ...user, name: nextUser.name, email: nextUser.email })
  }

  return (
    <AuthContext.Provider
      value={{
        user,
        isLoading,
        login,
        register,
        logout,
        isAuthenticated: !!user,
        updateUser,
      }}
    >
      {shouldRenderChildren ? (
        children
      ) : (
        // Shown while the mount-time session check (bootstrapSession) is resolving, and during
        // the brief window before the redirect effect lands on the right route. Previously this
        // rendered null, which looked like a blank/black screen whenever a stale refresh cookie
        // made that check take longer (e.g. an expired session on a protected route).
        <div className="flex min-h-screen items-center justify-center bg-background">
          <Spinner className="size-8 text-muted-foreground" />
        </div>
      )}
    </AuthContext.Provider>
  )
}

export function useAuth() {
  const context = useContext(AuthContext)
  if (context === undefined) {
    throw new Error("useAuth must be used within an AuthProvider")
  }

  return context
}
