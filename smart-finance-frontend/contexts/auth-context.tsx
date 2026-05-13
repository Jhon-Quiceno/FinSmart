"use client"

import { createContext, useContext, useState, useEffect, ReactNode } from "react"
import { useRouter } from "next/navigation"
import { getApiErrorMessage, loginRequest, registerRequest } from "@/lib/api-client"

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
  logout: () => void
  isAuthenticated: boolean
}

const AuthContext = createContext<AuthContextType | undefined>(undefined)

const STORAGE_USER_KEY = "financeai_user"
const STORAGE_TOKEN_KEY = "financeai_token"

export function AuthProvider({ children }: { children: ReactNode }) {
  const [user, setUser] = useState<User | null>(null)
  const [isLoading, setIsLoading] = useState(true)
  const router = useRouter()

  const setTokenCookie = (token: string) => {
    document.cookie = `financeai_token=${token}; path=/; max-age=604800; samesite=lax`
  }

  const clearTokenCookie = () => {
    document.cookie = "financeai_token=; path=/; max-age=0; samesite=lax"
  }

  const clearSession = () => {
    setUser(null)
    window.localStorage.removeItem(STORAGE_USER_KEY)
    window.localStorage.removeItem(STORAGE_TOKEN_KEY)
    clearTokenCookie()
  }

  useEffect(() => {
    const timer = setTimeout(() => {
      const storedUser = window.localStorage.getItem(STORAGE_USER_KEY)
      const storedToken = window.localStorage.getItem(STORAGE_TOKEN_KEY)

      if (storedUser && storedToken) {
        setUser(JSON.parse(storedUser) as User)
      } else {
        clearSession()
      }

      setIsLoading(false)
    }, 0)

    return () => clearTimeout(timer)
  }, []) // eslint-disable-line react-hooks/exhaustive-deps

  const persistSession = (nextUser: User, token: string) => {
    setUser(nextUser)
    window.localStorage.setItem(STORAGE_USER_KEY, JSON.stringify(nextUser))
    window.localStorage.setItem(STORAGE_TOKEN_KEY, token)
    setTokenCookie(token)
  }

  const login = async (email: string, password: string): Promise<AuthActionResult> => {
    setIsLoading(true)

    try {
      const response = await loginRequest(email, password)
      const nextUser: User = {
        id: String(response.user.id),
        name: response.user.name,
        email: response.user.email,
      }

      persistSession(nextUser, response.token)
      setIsLoading(false)
      return { success: true }
    } catch (error) {
      setIsLoading(false)
      return {
        success: false,
        error: getApiErrorMessage(error, "No fue posible iniciar sesión"),
      }
    }
  }

  const register = async (name: string, email: string, password: string): Promise<AuthActionResult> => {
    setIsLoading(true)

    try {
      const response = await registerRequest(name, email, password)
      const nextUser: User = {
        id: String(response.user.id),
        name: response.user.name,
        email: response.user.email,
      }

      persistSession(nextUser, response.token)
      setIsLoading(false)
      return { success: true }
    } catch (error) {
      setIsLoading(false)
      return {
        success: false,
        error: getApiErrorMessage(error, "No fue posible crear la cuenta"),
      }
    }
  }

  const logout = () => {
    clearSession()
    router.push("/login")
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
      }}
    >
      {children}
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
