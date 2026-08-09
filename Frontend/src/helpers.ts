import type { IncidentStatus, RequestStatus } from './types'

const INCIDENT_NEXT: Record<IncidentStatus, IncidentStatus[]> = {
  REPORTED: ['VERIFIED', 'IN_PROGRESS', 'RESOLVED', 'CLOSED'],
  VERIFIED: ['IN_PROGRESS', 'RESOLVED', 'CLOSED'],
  IN_PROGRESS: ['RESOLVED', 'CLOSED'],
  RESOLVED: ['CLOSED', 'REOPENED'],
  CLOSED: ['REOPENED'],
  REOPENED: ['IN_PROGRESS', 'RESOLVED', 'CLOSED'],
}

const REQUEST_NEXT: Record<RequestStatus, RequestStatus[]> = {
  PENDING: ['ASSIGNED', 'IN_PROGRESS', 'CANCELLED'],
  ASSIGNED: ['IN_PROGRESS', 'FULFILLED', 'CANCELLED'],
  IN_PROGRESS: ['FULFILLED', 'CANCELLED'],
  FULFILLED: ['CLOSED'],
  CLOSED: [],
  CANCELLED: [],
}

export function incidentNextStates(status: IncidentStatus): IncidentStatus[] {
  return INCIDENT_NEXT[status] ?? []
}

export function requestNextStates(status: RequestStatus): RequestStatus[] {
  return REQUEST_NEXT[status] ?? []
}

export function isRequestOpen(status: RequestStatus): boolean {
  return status === 'PENDING' || status === 'ASSIGNED' || status === 'IN_PROGRESS'
}
