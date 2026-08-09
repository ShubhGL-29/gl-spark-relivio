import { useState } from 'react'
import { useApp } from '../AppContext'
import { api } from '../api'
import { useApi, formatDate } from '../hooks'
import type { Notification } from '../types'
import {
  Badge,
  priorityTone,
  EmptyState,
  Spinner,
  IconBell,
  IconRefresh,
} from '../components/ui'

const TYPE_LABEL: Record<string, string> = {
  INCIDENT_CREATED: 'Incident reported',
  INCIDENT_STATUS_CHANGED: 'Incident updated',
  RELIEF_REQUEST_CREATED: 'Request submitted',
  RELIEF_REQUEST_STATUS_CHANGED: 'Request updated',
  VOLUNTEER_ASSIGNED: 'Volunteer assigned',
  VOLUNTEER_RELEASED: 'Volunteer released',
  RESOURCE_ALLOCATED: 'Resource allocated',
  SHELTER_ALLOCATED: 'Shelter allocated',
  GENERAL: 'General',
}

export function Notifications() {
  const { userId, user, toast, refreshTick, bump } = useApp()
  const isAdmin = user?.role === 'ADMIN'
  const [viewingId, setViewingId] = useState(userId)

  const { data, loading, error, reload } = useApi<Notification[]>(
    () => api.notifications.forUser(viewingId),
    [viewingId, refreshTick],
  )
  const unread = useApi<number>(() => api.notifications.unreadCount(viewingId), [viewingId, refreshTick])

  const unreadCount = unread.data ?? 0

  async function markAllRead() {
    const unreadItems = (data ?? []).filter((n) => n.status === 'UNREAD')
    for (const n of unreadItems) {
      try {
        await api.notifications.markRead(n.notificationId)
      } catch {
        /* continue */
      }
    }
    toast('success', `${unreadItems.length} notification(s) marked as read`)
    bump()
  }

  return (
    <div>
      <div className="page-head">
        <div>
          <h2>Notifications</h2>
          <p>Status updates for your account, most recent first</p>
        </div>
        <div className="spacer" />
        <button className="btn btn-outline" onClick={reload}>
          <IconRefresh size={15} /> Refresh
        </button>
        {unreadCount > 0 && (
          <button className="btn btn-primary" onClick={markAllRead}>
            Mark all read ({unreadCount})
          </button>
        )}
      </div>

      {isAdmin ? (
        <div className="card card-pad" style={{ marginBottom: 18 }}>
          <div style={{ display: 'flex', alignItems: 'flex-end', gap: 12, flexWrap: 'wrap' }}>
            <div className="field" style={{ margin: 0, flex: 1, minWidth: 220 }}>
              <label>View notifications for user ID</label>
              <input className="input" type="number" min={1} value={viewingId} onChange={(e) => setViewingId(Number(e.target.value) || viewingId)} />
            </div>
            <div className="seg" style={{ height: 38 }}>
              {[
                { id: 1, label: 'Admin' },
                { id: 2, label: 'User 2' },
                { id: 3, label: 'User 3' },
              ].map((u) => (
                <button key={u.id} className={viewingId === u.id ? 'active' : ''} onClick={() => setViewingId(u.id)}>
                  {u.label}
                </button>
              ))}
            </div>
          </div>
        </div>
      ) : null}

      {error ? (
        <div className="card card-pad">
          <div className="form-error" style={{ margin: 0 }}>{error}</div>
        </div>
      ) : null}

      {loading ? (
        <Spinner />
      ) : (data ?? []).length === 0 ? (
        <div className="card">
          <EmptyState icon="🔔" title="No notifications for this user" hint="Status changes for incidents, requests, assignments and allocations will appear here." />
        </div>
      ) : (
        <div className="card">
          {(data ?? []).map((n) => (
            <NotificationRow key={n.notificationId} n={n} onRead={() => bump()} />
          ))}
        </div>
      )}
    </div>
  )
}

function NotificationRow({ n, onRead }: { n: Notification; onRead: () => void }) {
  const { toast } = useApp()
  const isUnread = n.status === 'UNREAD'

  return (
    <div className="list-item" style={{ background: isUnread ? 'var(--surface-2)' : undefined }}>
      <div className="avatar" style={{ background: isUnread ? 'var(--primary-soft)' : 'var(--surface-2)', color: 'var(--primary)' }}>
        <IconBell />
      </div>
      <div className="grow">
        <div style={{ display: 'flex', alignItems: 'center', gap: 8, flexWrap: 'wrap' }}>
          <span className="row-main">{n.title}</span>
          {isUnread && <span className="badge tone-danger">new</span>}
          <Badge text={n.priority} tone={priorityTone(n.priority)} />
          <span className="chip" style={{ textTransform: 'uppercase', fontSize: 10 }}>
            {TYPE_LABEL[n.notificationType] ?? n.notificationType}
          </span>
        </div>
        <div className="row-sub" style={{ marginTop: 3 }}>{n.message}</div>
        <div className="row-sub" style={{ marginTop: 3 }}>
          {formatDate(n.createdAt)}
          {n.relatedEntityType ? ` · ${n.relatedEntityType.toLowerCase()} #${n.relatedEntityId ?? ''}` : ''}
        </div>
      </div>
      {isUnread && (
        <button
          className="btn btn-outline btn-sm"
          onClick={async () => {
            try {
              await api.notifications.markRead(n.notificationId)
              onRead()
            } catch (err) {
              toast('error', api.errorMessage(err))
            }
          }}
        >
          Mark read
        </button>
      )}
    </div>
  )
}
