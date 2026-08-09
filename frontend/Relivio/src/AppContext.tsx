import {
  createContext,
  useCallback,
  useContext,
  useEffect,
  useMemo,
  useState,
  type ReactNode,
} from 'react'
import type { AuthUser, Route } from './types'
import { loadSession, saveSession } from './auth'
import { api } from './api'

interface Toast {
  id: number
  kind: 'success' | 'error' | 'info'
  message: string
}

type NavigateFn = (route: Route, incidentId?: number | null) => void

interface AppContextValue {
  route: Route
  incidentId: number | null
  navigate: NavigateFn
  toasts: Toast[]
  toast: (kind: Toast['kind'], message: string) => void
  dismissToast: (id: number) => void
  user: AuthUser | null
  login: (user: AuthUser) => void
  logout: () => void
  userId: number
  refreshTick: number
  bump: () => void
}

const AppContext = createContext<AppContextValue | null>(null)

let toastSeq = 1

export function AppProvider({ children }: { children: ReactNode }) {
  const [route, setRoute] = useState<Route>('dashboard')
  const [incidentId, setIncidentId] = useState<number | null>(null)
  const [toasts, setToasts] = useState<Toast[]>([])
  const [user, setUser] = useState<AuthUser | null>(() => loadSession())
  const [refreshTick, setRefreshTick] = useState(0)

  const navigate = useCallback<NavigateFn>((next, id = null) => {
    setRoute(next)
    setIncidentId(id)
    window.scrollTo(0, 0)
  }, [])

  const dismissToast = useCallback((id: number) => {
    setToasts((prev) => prev.filter((t) => t.id !== id))
  }, [])

  const toast = useCallback(
    (kind: Toast['kind'], message: string) => {
      const id = toastSeq++
      setToasts((prev) => [...prev.slice(-3), { id, kind, message }])
      window.setTimeout(() => dismissToast(id), 4200)
    },
    [dismissToast],
  )

  const login = useCallback((next: AuthUser) => {
    saveSession(next)
    setUser(next)
  }, [])

  const logout = useCallback(() => {
    const session = loadSession()
    if (session?.token) {
      api.auth.logout().catch(() => undefined)
    }
    saveSession(null)
    setUser(null)
  }, [])

  const bump = useCallback(() => setRefreshTick((t) => t + 1), [])

  const userId = user?.id ?? 1

  const value = useMemo(
    () => ({
      route,
      incidentId,
      navigate,
      toasts,
      toast,
      dismissToast,
      user,
      login,
      logout,
      userId,
      refreshTick,
      bump,
    }),
    [route, incidentId, navigate, toasts, toast, dismissToast, user, login, logout, userId, refreshTick, bump],
  )

  useEffect(() => {
    document.title =
      route === 'dashboard' ? 'Relivio — Disaster Relief' : `Relivio — ${route}`
  }, [route])

  return <AppContext.Provider value={value}>{children}</AppContext.Provider>
}

export function useApp(): AppContextValue {
  const ctx = useContext(AppContext)
  if (!ctx) throw new Error('useApp must be used within AppProvider')
  return ctx
}
