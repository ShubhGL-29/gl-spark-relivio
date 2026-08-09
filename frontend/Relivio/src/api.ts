import type {
  AuthResponse,
  Incident,
  IncidentRequest,
  LoginPayload,
  Notification,
  Page,
  RegisterPayload,
  ReliefRequest,
  ReliefRequestPayload,
  Resource,
  ResourcePayload,
  Shelter,
  ShelterPayload,
  Volunteer,
  VolunteerPayload,
} from './types'

const BASE_URL = (import.meta.env.VITE_API_URL as string | undefined) ?? 'http://localhost:8080'

class ApiError extends Error {
  status: number
  details: unknown

  constructor(status: number, message: string, details?: unknown) {
    super(message)
    this.status = status
    this.details = details
  }
}

function getSessionToken(): string | null {
  try {
    const session = JSON.parse(localStorage.getItem('relivio:session') ?? 'null')
    return typeof session?.token === 'string' ? session.token : null
  } catch {
    return null
  }
}

async function request<T>(path: string, init: RequestInit = {}): Promise<T> {
  const token = getSessionToken()
  const headers: Record<string, string> = { 'Content-Type': 'application/json', ...(init.headers as Record<string, string> | undefined) }
  if (token) headers['Authorization'] = `Bearer ${token}`
  const res = await fetch(`${BASE_URL}${path}`, {
    headers,
    ...init,
  })

  if (!res.ok) {
    let message = `Request failed with status ${res.status}`
    let details: unknown
    try {
      const body = await res.json()
      details = body
      message =
        typeof body?.message === 'string' && body.message
          ? body.message
          : typeof body?.error === 'string' && body.error
            ? body.error
            : Array.isArray(body?.errors) && body.errors.length
              ? body.errors.map((e: unknown) => (e as { message?: string })?.message ?? '').join('; ')
              : message
    } catch {
      /* ignore non-JSON error bodies */
    }
    throw new ApiError(res.status, message, details)
  }

  if (res.status === 204) {
    return undefined as T
  }
  return (await res.json()) as T
}

function errorMessage(err: unknown): string {
  if (err instanceof ApiError) return err.message
  if (err instanceof Error) return err.message
  return 'Unexpected error'
}

export const api = {
  errorMessage,
  // ---- Auth ----
  auth: {
    register: (payload: RegisterPayload) =>
      request<AuthResponse>('/api/auth/register', { method: 'POST', body: JSON.stringify(payload) }),
    registerVolunteer: (payload: RegisterPayload) =>
      request<AuthResponse>('/api/auth/register/volunteer', { method: 'POST', body: JSON.stringify(payload) }),
    login: (payload: LoginPayload) =>
      request<AuthResponse>('/api/auth/login', { method: 'POST', body: JSON.stringify(payload) }),
    logout: () => request<void>('/api/auth/logout', { method: 'POST' }),
    users: () => request<AuthResponse['user'][]>('/api/auth/users'),
  },
  // ---- Incidents ----
  incidents: {
    list: (params?: Record<string, string>) => {
      const qs = params ? `?${new URLSearchParams(params).toString()}` : ''
      return request<Incident[]>(`/api/incidents${qs}`)
    },
    get: (id: number) => request<Incident>(`/api/incidents/${id}`),
    create: (payload: IncidentRequest) =>
      request<Incident>('/api/incidents', { method: 'POST', body: JSON.stringify(payload) }),
    patch: (id: number, updates: Record<string, unknown>) =>
      request<Incident>(`/api/incidents/${id}`, { method: 'PATCH', body: JSON.stringify(updates) }),
  },
  // ---- Relief requests ----
  reliefRequests: {
    list: (params?: Record<string, string>) => {
      const qs = params ? `?${new URLSearchParams(params).toString()}` : ''
      return request<ReliefRequest[]>(`/api/relief-requests${qs}`)
    },
    create: (payload: ReliefRequestPayload) =>
      request<ReliefRequest>('/api/relief-requests', { method: 'POST', body: JSON.stringify(payload) }),
    patch: (id: number, updates: Record<string, unknown>) =>
      request<ReliefRequest>(`/api/relief-requests/${id}`, {
        method: 'PATCH',
        body: JSON.stringify(updates),
      }),
  },
  // ---- Volunteers ----
  volunteers: {
    list: (page = 0, size = 50) =>
      request<Page<Volunteer>>(`/api/volunteers?page=${page}&size=${size}`),
    all: () => request<Page<Volunteer>>(`/api/volunteers?size=100`).then((p) => p.content),
    register: (payload: VolunteerPayload) =>
      request<Volunteer>('/api/volunteers/register', {
        method: 'POST',
        body: JSON.stringify(payload),
      }),
    assign: (
      id: number,
      body: {
        incidentId: number
        assignedArea: string
        reliefRequestId?: number | null
        latitude?: number | null
        longitude?: number | null
      },
    ) =>
      request<Volunteer>(`/api/volunteers/${id}/assign`, {
        method: 'POST',
        body: JSON.stringify(body),
      }),
    release: (id: number) =>
      request<Volunteer>(`/api/volunteers/${id}/release`, { method: 'POST' }),
    updateStatus: (id: number, newStatus: string) =>
      request<Volunteer>(`/api/volunteers/${id}/status`, {
        method: 'PATCH',
        body: JSON.stringify({ newStatus }),
      }),
  },
  // ---- Resources ----
  resources: {
    all: () => request<Page<Resource>>('/api/resources?size=100').then((p) => p.content),
    create: (payload: ResourcePayload) =>
      request<Resource>('/api/resources', { method: 'POST', body: JSON.stringify(payload) }),
    allocate: (id: number, quantity: number, reliefRequestId?: number | null) => {
      const qs = new URLSearchParams({ quantity: String(quantity) })
      if (reliefRequestId) qs.set('reliefRequestId', String(reliefRequestId))
      return request<Resource>(`/api/resources/${id}/allocate?${qs}`, { method: 'POST' })
    },
    restock: (id: number, quantity: number) =>
      request<Resource>(`/api/resources/${id}/restock?quantity=${quantity}`, { method: 'POST' }),
    lowStock: () => request<Resource[]>('/api/resources/low-stock'),
  },
  // ---- Shelters ----
  shelters: {
    list: () => request<Shelter[]>(`/api/shelters`),
    create: (payload: ShelterPayload) =>
      request<Shelter>('/api/shelters', { method: 'POST', body: JSON.stringify(payload) }),
    allocate: (id: number, people: number, reliefRequestId?: number | null) => {
      const qs = new URLSearchParams({ people: String(people) })
      if (reliefRequestId) qs.set('reliefRequestId', String(reliefRequestId))
      return request<Shelter>(`/api/shelters/${id}/allocate?${qs}`, { method: 'POST' })
    },
    updateOccupancy: (id: number, occupancy: number) =>
      request<Shelter>(`/api/shelters/${id}/occupancy?occupancy=${occupancy}`, {
        method: 'PATCH',
      }),
  },
  // ---- Notifications ----
  notifications: {
    forUser: (userId: number) =>
      request<Notification[]>(`/api/notifications/user/${userId}`),
    unreadCount: (userId: number) =>
      request<number>(`/api/notifications/user/${userId}/unread-count`),
    markRead: (id: number) =>
      request<Notification>(`/api/notifications/${id}/read`, { method: 'PATCH' }),
  },
}
