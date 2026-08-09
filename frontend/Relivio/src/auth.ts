import type { AuthUser, AuthResponse, Role, Route } from './types'
import { api } from './api'

export const SEEDED_USERS: Record<string, { id: number; role: Role; name: string; phone: string; password: string }> = {
  admin: { id: 1, role: 'ADMIN', name: 'Administrator', phone: '1000000001', password: 'admin123' },
  volunteer: { id: 2, role: 'VOLUNTEER', name: 'Relivio Volunteer', phone: '1000000002', password: 'vol123' },
}

export const ROUTE_ACCESS: Record<Role, Route[]> = {
  ADMIN: ['dashboard', 'incidents', 'relief', 'volunteers', 'resources', 'shelters', 'notifications'],
  VOLUNTEER: ['dashboard', 'incidents', 'relief', 'notifications'],
  CITIZEN: ['dashboard', 'incidents', 'relief', 'shelters', 'notifications'],
}

export const ROUTE_LABELS: Record<Route, string> = {
  dashboard: 'Dashboard',
  incidents: 'Incidents',
  relief: 'Relief Requests',
  volunteers: 'Volunteers',
  resources: 'Resources',
  shelters: 'Shelters',
  notifications: 'Notifications',
}

export const ROLE_LABELS: Record<Role, string> = {
  ADMIN: 'Administrator',
  VOLUNTEER: 'Volunteer',
  CITIZEN: 'Citizen',
}

export function canAccess(role: Role, route: Route): boolean {
  return ROUTE_ACCESS[role].includes(route)
}

export function canManageIncidents(role: Role): boolean {
  return role === 'ADMIN' || role === 'VOLUNTEER'
}

export function canAssign(role: Role): boolean {
  return role === 'ADMIN' || role === 'VOLUNTEER'
}

export function canManageRegistry(role: Role): boolean {
  return role === 'ADMIN'
}

const SESSION_KEY = 'relivio:session'

export function registerCitizen(name: string, email: string, phone: string, password: string): Promise<AuthUser> {
  return api.auth.register({ name, phone, email, password }).then(toAuthUser)
}

export function registerVolunteer(name: string, email: string, phone: string, password: string): Promise<AuthUser> {
  return api.auth.registerVolunteer({ name, phone, email, password }).then(toAuthUser)
}

export function loginCitizen(phone: string, password: string): Promise<AuthUser> {
  return api.auth.login({ phone, password }).then(toAuthUser)
}

function toAuthUser(res: AuthResponse): AuthUser {
  return {
    id: res.user.id,
    role: res.user.role,
    name: res.user.name,
    email: res.user.email ?? undefined,
    phone: res.user.phone,
    token: res.token,
  }
}

export function loadSession(): AuthUser | null {
  try {
    return JSON.parse(localStorage.getItem(SESSION_KEY) ?? 'null')
  } catch {
    return null
  }
}

export function saveSession(session: AuthUser | null) {
  if (session) localStorage.setItem(SESSION_KEY, JSON.stringify(session))
  else localStorage.removeItem(SESSION_KEY)
}

export function clearSession() {
  localStorage.removeItem(SESSION_KEY)
}
