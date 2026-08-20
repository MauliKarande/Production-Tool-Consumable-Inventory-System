import { createContext, useContext, useMemo, useState, type ReactNode } from 'react'
import { AuthApi } from '../api/endpoints'
import { apiErrorMessage } from '../api/client'
import { getStoredAuth, setStoredAuth, type StoredAuth } from '../api/client'

interface AuthContextValue {
  auth: StoredAuth | null
  login: (username: string, password: string) => Promise<void>
  logout: () => void
  isAdmin: boolean
  isIssuer: boolean
}

const AuthContext = createContext<AuthContextValue | undefined>(undefined)

export function AuthProvider({ children }: { children: ReactNode }) {
  const [auth, setAuth] = useState<StoredAuth | null>(getStoredAuth())

  const login = async (username: string, password: string) => {
    try {
      const res = await AuthApi.login(username, password)
      const next: StoredAuth = {
        token: res.data.token,
        username: res.data.username,
        role: res.data.role as StoredAuth['role'],
        userId: res.data.userId,
      }
      setStoredAuth(next)
      setAuth(next)
    } catch (err) {
      throw new Error(apiErrorMessage(err))
    }
  }

  const logout = () => {
    setStoredAuth(null)
    setAuth(null)
  }

  const value = useMemo<AuthContextValue>(
    () => ({
      auth,
      login,
      logout,
      isAdmin: auth?.role === 'ADMIN',
      isIssuer: auth?.role === 'ISSUER' || auth?.role === 'ADMIN',
    }),
    [auth],
  )

  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>
}

export function useAuth() {
  const ctx = useContext(AuthContext)
  if (!ctx) throw new Error('useAuth must be used within AuthProvider')
  return ctx
}
