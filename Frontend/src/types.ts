export type Route =
  | 'dashboard'
  | 'incidents'
  | 'relief'
  | 'volunteers'
  | 'resources'
  | 'shelters'
  | 'notifications'

export type Role = 'ADMIN' | 'VOLUNTEER' | 'CITIZEN'

export interface AuthUser {
  id: number
  role: Role
  name: string
  email?: string
  phone?: string
  token?: string
}

export interface AuthResponse {
  token: string
  user: {
    id: number
    name: string
    phone: string
    email?: string | null
    role: Role
    createdAt?: string
  }
}

export interface RegisterPayload {
  name: string
  phone: string
  email?: string
  password: string
}

export interface LoginPayload {
  phone: string
  password: string
}

export type Severity = 'LOW' | 'MEDIUM' | 'HIGH' | 'CRITICAL'
export type IncidentStatus =
  | 'REPORTED'
  | 'VERIFIED'
  | 'IN_PROGRESS'
  | 'RESOLVED'
  | 'CLOSED'
  | 'REOPENED'

export type RequestType =
  | 'MEDICAL'
  | 'FOOD'
  | 'WATER'
  | 'SHELTER'
  | 'EVACUATION'
  | 'TRANSPORTATION'
  | 'OTHER'

export type Priority = 'LOW' | 'MEDIUM' | 'HIGH' | 'URGENT'

export type RequestStatus =
  | 'PENDING'
  | 'ASSIGNED'
  | 'IN_PROGRESS'
  | 'FULFILLED'
  | 'CLOSED'
  | 'CANCELLED'

export type AvailabilityStatus =
  | 'AVAILABLE'
  | 'ASSIGNED'
  | 'ON_LEAVE'
  | 'UNAVAILABLE'

export type Gender = 'MALE' | 'FEMALE' | 'OTHER'

export type ResourceCategory =
  | 'FOOD'
  | 'WATER'
  | 'MEDICINE'
  | 'BLANKET'
  | 'TENT'
  | 'CLOTHING'
  | 'HYGIENE_KIT'
  | 'MEDICAL_EQUIPMENT'
  | 'OTHER'

export type ResourceStatus =
  | 'AVAILABLE'
  | 'LOW_STOCK'
  | 'OUT_OF_STOCK'
  | 'EXPIRED'

type NotificationStatus = 'UNREAD' | 'READ'

type NotificationType =
  | 'INCIDENT_CREATED'
  | 'INCIDENT_STATUS_CHANGED'
  | 'RELIEF_REQUEST_CREATED'
  | 'RELIEF_REQUEST_STATUS_CHANGED'
  | 'VOLUNTEER_ASSIGNED'
  | 'VOLUNTEER_RELEASED'
  | 'RESOURCE_ALLOCATED'
  | 'SHELTER_ALLOCATED'
  | 'GENERAL'

type NotificationPriority = 'LOW' | 'MEDIUM' | 'HIGH' | 'URGENT'

export interface Incident {
  incidentId: number
  title: string
  disasterType: string
  severity: Severity
  location: string
  description: string
  status: IncidentStatus
  reporterId: number | null
  reporterName: string | null
  reporterContact: string | null
  reportedDate: string
  lastUpdated: string
}

export interface IncidentRequest {
  title: string
  disasterType: string
  severity: Severity
  location: string
  description: string
  reporterId?: number | null
  reporterName?: string | null
  reporterContact?: string | null
}

export interface ReliefRequest {
  requestId: number
  incidentId: number
  victimName: string
  phone: string
  email: string
  requestType: RequestType
  priority: Priority
  description: string
  address: string
  status: RequestStatus
  assignedVolunteerId: number | null
  assignedVolunteerName: string | null
  allocatedResourceId: number | null
  allocatedResourceName: string | null
  allocatedShelterId: number | null
  allocatedShelterName: string | null
  requestDate: string
  updatedAt: string
}

export interface ReliefRequestPayload {
  incidentId: number
  victimName: string
  phone: string
  email: string
  requestType: RequestType
  priority: Priority
  description?: string
}

export interface Volunteer {
  volunteerId: number
  firstName: string
  lastName: string
  email: string
  phoneNumber: string
  age: number
  gender: Gender
  skills: string
  address: string
  city: string
  state: string
  latitude: number | null
  longitude: number | null
  availabilityStatus: AvailabilityStatus
  assignedIncidentId: number | null
  assignedArea: string | null
  assignedReliefRequestId: number | null
  emergencyContactName: string
  emergencyContactNumber: string
  registrationDate: string
  updatedAt: string
}

export interface VolunteerPayload {
  firstName: string
  lastName: string
  email: string
  phoneNumber: string
  age: number
  gender: Gender
  skills?: string
  address?: string
  city?: string
  state?: string
  latitude?: number | null
  longitude?: number | null
  availabilityStatus?: AvailabilityStatus
  emergencyContactName: string
  emergencyContactNumber: string
}

export interface Resource {
  resourceId: number
  resourceName: string
  category: ResourceCategory
  quantityAvailable: number
  quantityAllocated: number
  unit: string
  warehouseLocation: string
  latitude: number | null
  longitude: number | null
  expiryDate: string | null
  supplierName: string
  status: ResourceStatus
  createdAt: string
  updatedAt: string
}

export interface ResourcePayload {
  resourceName: string
  category: ResourceCategory
  quantity: number
  unit: string
  warehouseLocation: string
  latitude?: number | null
  longitude?: number | null
  expiryDate?: string | null
  supplierName?: string
}

export interface Shelter {
  shelterId: number
  name: string
  location: string
  address: string
  city: string
  state: string
  latitude: number | null
  longitude: number | null
  capacity: number
  currentOccupancy: number
  hasCapacity: boolean
  amenities: string
  contactNumber: string
  createdAt: string
  updatedAt: string
}

export interface ShelterPayload {
  name: string
  location: string
  address?: string
  city?: string
  state?: string
  latitude?: number | null
  longitude?: number | null
  capacity: number
  currentOccupancy?: number
  amenities?: string
  contactNumber: string
}

export interface Notification {
  notificationId: number
  recipientId: number
  recipientName: string
  recipientEmail: string | null
  title: string
  message: string
  notificationType: NotificationType
  priority: NotificationPriority
  status: NotificationStatus
  relatedEntityId: number | null
  relatedEntityType: string | null
  createdAt: string
  readAt: string | null
}

export interface Page<T> {
  content: T[]
  totalPages: number
  totalElements: number
  size: number
  number: number
  first: boolean
  last: boolean
}
