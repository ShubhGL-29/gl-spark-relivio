import type { ReactNode } from 'react'
import { useApp } from '../AppContext'

/* ---------------- Tone helpers ---------------- */

export function statusTone(status: string): string {
  switch (status) {
    case 'REPORTED':
    case 'PENDING':
    case 'AVAILABLE':
    case 'UNREAD':
      return 'tone-info'
    case 'VERIFIED':
    case 'ASSIGNED':
    case 'IN_PROGRESS':
    case 'LOW_STOCK':
      return 'tone-warning'
    case 'RESOLVED':
    case 'FULFILLED':
    case 'CLOSED':
    case 'READ':
      return 'tone-success'
    case 'REOPENED':
      return 'tone-primary'
    case 'CANCELLED':
    case 'EXPIRED':
    case 'OUT_OF_STOCK':
    case 'UNAVAILABLE':
      return 'tone-danger'
    case 'ON_LEAVE':
      return 'tone-muted'
    default:
      return 'tone-muted'
  }
}

export function severityTone(severity: string): string {
  switch (severity) {
    case 'CRITICAL':
      return 'tone-danger'
    case 'HIGH':
      return 'tone-accent'
    case 'MEDIUM':
      return 'tone-warning'
    default:
      return 'tone-success'
  }
}

export function priorityTone(priority: string): string {
  switch (priority) {
    case 'URGENT':
      return 'tone-danger'
    case 'HIGH':
      return 'tone-accent'
    case 'MEDIUM':
      return 'tone-warning'
    default:
      return 'tone-success'
  }
}

export function resourceTone(status: string): string {
  switch (status) {
    case 'AVAILABLE':
      return 'tone-success'
    case 'LOW_STOCK':
      return 'tone-warning'
    case 'OUT_OF_STOCK':
    case 'EXPIRED':
      return 'tone-danger'
    default:
      return 'tone-muted'
  }
}

/* ---------------- Badge ---------------- */

export function Badge({ text, tone }: { text: string; tone: string }) {
  return (
    <span className={`badge ${tone}`}>
      <span className="badge-dot" />
      {text.replace(/_/g, ' ')}
    </span>
  )
}

/* ---------------- Progress ---------------- */

export function ProgressBar({ value, max }: { value: number; max: number }) {
  const pct = max <= 0 ? 0 : Math.min(100, Math.round((value / max) * 100))
  const cls = pct >= 100 ? 'progress-fill danger' : pct >= 80 ? 'progress-fill warn' : 'progress-fill'
  return (
    <div className="progress-track">
      <div className={cls} style={{ width: `${pct}%` }} />
    </div>
  )
}

/* ---------------- Spinner / Empty ---------------- */

export function Spinner() {
  return (
    <div className="spinner-center">
      <div className="spinner" />
    </div>
  )
}

export function EmptyState({ icon, title, hint }: { icon: string; title: string; hint?: string }) {
  return (
    <div className="empty-state">
      <div className="icon">{icon}</div>
      <h4>{title}</h4>
      {hint ? <div className="small">{hint}</div> : null}
    </div>
  )
}

/* ---------------- Modal ---------------- */

export function Modal({
  title,
  onClose,
  children,
  wide,
}: {
  title: string
  onClose: () => void
  children: ReactNode
  wide?: boolean
}) {
  return (
    <div
      className="modal-overlay"
      onMouseDown={(e) => {
        if (e.target === e.currentTarget) onClose()
      }}
    >
      <div className={`modal ${wide ? 'modal-lg' : ''}`}>
        <div className="modal-head">
          <h3>{title}</h3>
          <div className="spacer" />
          <button className="btn btn-ghost btn-icon" onClick={onClose} aria-label="Close">
            <IconX />
          </button>
        </div>
        <div className="modal-body">{children}</div>
      </div>
    </div>
  )
}

/* ---------------- Toasts ---------------- */

export function ToastStack() {
  const { toasts, dismissToast } = useApp()
  return (
    <div className="toast-stack">
      {toasts.map((t) => (
        <div key={t.id} className={`toast toast-${t.kind}`} onClick={() => dismissToast(t.id)}>
          {t.kind === 'success' ? <IconCheckCircle /> : t.kind === 'error' ? <IconAlert /> : <IconInfo />}
          <span>{t.message}</span>
        </div>
      ))}
    </div>
  )
}

/* ---------------- Icons ---------------- */

type IconProps = { size?: number; style?: React.CSSProperties }

const makeIcon =
  (path: ReactNode) =>
  ({ size = 18, style }: IconProps = {}) =>
    (
      <svg width={size} height={size} viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round" style={style}>
        {path}
      </svg>
    )

export const IconGrid = makeIcon(
  <>
    <rect x="3" y="3" width="7" height="7" rx="1" />
    <rect x="14" y="3" width="7" height="7" rx="1" />
    <rect x="3" y="14" width="7" height="7" rx="1" />
    <rect x="14" y="14" width="7" height="7" rx="1" />
  </>,
)

export const IconAlert = makeIcon(
  <>
    <path d="M10.3 3.7 1.8 18a2 2 0 0 0 1.7 3h17a2 2 0 0 0 1.7-3L13.7 3.7a2 2 0 0 0-3.4 0z" />
    <line x1="12" y1="9" x2="12" y2="13" />
    <line x1="12" y1="17" x2="12.01" y2="17" />
  </>,
)

export const IconHandHeart = makeIcon(
  <>
    <path d="M11 14h2a2 2 0 1 0 0-4h-3c-.6 0-1.1.2-1.4.6L3 16" />
    <path d="m7 20 1.6-1.4c.3-.4.8-.6 1.4-.6h4c1.1 0 2.1-.4 2.8-1.2l4.6-4.4a2 2 0 0 0-2.75-2.91l-4.2 3.9" />
    <path d="m2 15 6 6" />
    <path d="M19.5 8.5c.7-.7 1-1.6 1-2.5a3.5 3.5 0 0 0-7 0c0 .9.3 1.8 1 2.5" />
  </>,
)

export const IconUsers = makeIcon(
  <>
    <path d="M17 21v-2a4 4 0 0 0-4-4H5a4 4 0 0 0-4 4v2" />
    <circle cx="9" cy="7" r="4" />
    <path d="M23 21v-2a4 4 0 0 0-3-3.87" />
    <path d="M16 3.13a4 4 0 0 1 0 7.75" />
  </>,
)

export const IconBox = makeIcon(
  <>
    <path d="M21 8a2 2 0 0 0-1-1.73l-7-4a2 2 0 0 0-2 0l-7 4A2 2 0 0 0 3 8v8a2 2 0 0 0 1 1.73l7 4a2 2 0 0 0 2 0l7-4A2 2 0 0 0 21 16Z" />
    <path d="m3.3 7 8.7 5 8.7-5" />
    <path d="M12 22V12" />
  </>,
)

export const IconBell = makeIcon(  <>
    <path d="M18 8A6 6 0 0 0 6 8c0 7-3 9-3 9h18s-3-2-3-9" />
    <path d="M13.7 21a2 2 0 0 1-3.4 0" />
  </>,
)

export const IconPlus = makeIcon(
  <>
    <line x1="12" y1="5" x2="12" y2="19" />
    <line x1="5" y1="12" x2="19" y2="12" />
  </>,
)

export const IconX = makeIcon(
  <>
    <line x1="18" y1="6" x2="6" y2="18" />
    <line x1="6" y1="6" x2="18" y2="18" />
  </>,
)

export const IconSearch = makeIcon(
  <>
    <circle cx="11" cy="11" r="8" />
    <line x1="21" y1="21" x2="16.65" y2="16.65" />
  </>,
)

export const IconArrowRight = makeIcon(
  <>
    <line x1="5" y1="12" x2="19" y2="12" />
    <polyline points="12 5 19 12 12 19" />
  </>,
)

export const IconRefresh = makeIcon(
  <>
    <polyline points="23 4 23 10 17 10" />
    <path d="M20.5 15a9 9 0 1 1-2-9.4L23 10" />
  </>,
)

export const IconMapPin = makeIcon(
  <>
    <path d="M20 10c0 6-8 12-8 12s-8-6-8-12a8 8 0 0 1 16 0Z" />
    <circle cx="12" cy="10" r="3" />
  </>,
)

export const IconShield = makeIcon(
  <>
    <path d="M12 22s8-4 8-10V5l-8-3-8 3v7c0 6 8 10 8 10" />
    <path d="m9 12 2 2 4-4" />
  </>,
)

export const IconClock = makeIcon(
  <>
    <circle cx="12" cy="12" r="10" />
    <polyline points="12 6 12 12 16 14" />
  </>,
)

export const IconCheckCircle = makeIcon(
  <>
    <circle cx="12" cy="12" r="10" />
    <polyline points="9 12 11 14 15 10" />
  </>,
)

export const IconInfo = makeIcon(
  <>
    <circle cx="12" cy="12" r="10" />
    <line x1="12" y1="16" x2="12" y2="12" />
    <line x1="12" y1="8" x2="12.01" y2="8" />
  </>,
)

export const IconUserPlus = makeIcon(
  <>
    <path d="M16 21v-2a4 4 0 0 0-4-4H5a4 4 0 0 0-4 4v2" />
    <circle cx="8.5" cy="7" r="4" />
    <line x1="20" y1="8" x2="20" y2="14" />
    <line x1="23" y1="11" x2="17" y2="11" />
  </>,
)

export const IconTent = makeIcon(
  <>
    <path d="M4 21 12 3l8 18" />
    <path d="M8 21v-6" />
    <path d="M16 21v-6" />
    <path d="M12 3 3 21" />
  </>,
)

