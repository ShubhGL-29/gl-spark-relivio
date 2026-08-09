import { useApp } from '../AppContext'
import { api } from '../api'
import { useApi, formatDate } from '../hooks'
import type { Incident, ReliefRequest, Shelter, Volunteer, Resource } from '../types'
import {
  Badge,
  severityTone,
  statusTone,
  priorityTone,
  EmptyState,
  ProgressBar,
  Spinner,
  IconAlert,
  IconUsers,
  IconBox,
  IconTent,
  IconHandHeart,
  IconArrowRight,
  IconShield,
  IconBell,
} from '../components/ui'

export function Dashboard() {
  const { navigate, userId, refreshTick } = useApp()

  const incidents = useApi<Incident[]>(() => api.incidents.list(), [refreshTick])
  const requests = useApi<ReliefRequest[]>(() => api.reliefRequests.list(), [refreshTick])
  const volunteers = useApi<Volunteer[]>(() => api.volunteers.all(), [refreshTick])
  const resources = useApi<Resource[]>(() => api.resources.all(), [refreshTick])
  const shelters = useApi<Shelter[]>(() => api.shelters.list(), [refreshTick])
  const notifications = useApi(() => api.notifications.forUser(userId), [userId, refreshTick])

  const inc = incidents.data ?? []
  const req = requests.data ?? []
  const vol = volunteers.data ?? []
  const res = resources.data ?? []
  const shl = shelters.data ?? []

  const activeIncidents = inc.filter((i) =>
    ['REPORTED', 'VERIFIED', 'IN_PROGRESS', 'REOPENED'].includes(i.status),
  ).length
  const critical = inc.filter((i) => i.severity === 'CRITICAL').length
  const openRequests = req.filter((r) =>
    ['PENDING', 'ASSIGNED', 'IN_PROGRESS'].includes(r.status),
  ).length
  const urgentRequests = req.filter((r) => r.priority === 'URGENT' && !['FULFILLED', 'CLOSED', 'CANCELLED'].includes(r.status)).length
  const availableVolunteers = vol.filter((v) => v.availabilityStatus === 'AVAILABLE').length
  const assignedVolunteers = vol.filter((v) => v.availabilityStatus === 'ASSIGNED').length
  const totalCapacity = shl.reduce((s, x) => s + x.capacity, 0)
  const totalOccupancy = shl.reduce((s, x) => s + x.currentOccupancy, 0)
  const lowStock = res.filter((r) => r.status === 'LOW_STOCK' || r.status === 'OUT_OF_STOCK').length

  const recentIncidents = [...inc]
    .sort((a, b) => new Date(b.reportedDate).getTime() - new Date(a.reportedDate).getTime())
    .slice(0, 5)
  const recentNotifs = (notifications.data ?? []).slice(0, 5)

  return (
    <div>
      <div className="hero-strip">
        <div className="hero-icon">
          <IconHandHeart />
        </div>
        <div>
          <h2>Disaster Relief Coordination Center</h2>
          <p>Track incidents, match relief requests to volunteers and resources, and keep everyone informed.</p>
        </div>
        <button className="btn hero-btn" onClick={() => navigate('incidents')}>
          Report an incident <IconArrowRight />
        </button>
      </div>

      <div className="stats-grid">
        <StatTone label="Active Incidents" value={activeIncidents} foot={`${critical} critical severity`} icon={<IconAlert />} color="var(--danger)" soft="var(--danger-soft)" />
        <StatTone label="Open Relief Requests" value={openRequests} foot={`${urgentRequests} urgent now`} icon={<IconHandHeart />} color="var(--accent)" soft="var(--accent-soft)" />
        <StatTone label="Available Volunteers" value={availableVolunteers} foot={`${assignedVolunteers} deployed`} icon={<IconUsers />} color="var(--success)" soft="var(--success-soft)" />
        <StatTone label="Shelter Capacity" value={`${Math.round(totalOccupancy)}/${totalCapacity}`} foot={`${shl.length} shelters registered`} icon={<IconTent />} color="var(--info)" soft="var(--info-soft)" />
        <StatTone label="Resource Alerts" value={lowStock} foot="low stock / out of stock" icon={<IconBox />} color="var(--warning)" soft="var(--warning-soft)" />
      </div>

      <div className="grid grid-2">
        <div className="card">
          <div className="card-header">
            <h3>Recent Incidents</h3>
            <div className="spacer" />
            <button className="btn btn-ghost btn-sm" onClick={() => navigate('incidents')}>
              View all <IconArrowRight />
            </button>
          </div>
          {incidents.loading ? (
            <Spinner />
          ) : recentIncidents.length === 0 ? (
            <EmptyState icon="🗺️" title="No incidents yet" hint="Report the first incident to get started." />
          ) : (
            recentIncidents.map((i) => (
              <div className="list-item" key={i.incidentId}>
                <div
                  className="avatar"
                  style={{ background: i.severity === 'CRITICAL' ? 'var(--danger-soft)' : 'var(--primary-soft)', color: i.severity === 'CRITICAL' ? 'var(--danger)' : 'var(--primary)' }}
                >
                  {i.title.slice(0, 1).toUpperCase()}
                </div>
                <div className="grow">
                  <div className="row-main">{i.title}</div>
                  <div className="row-sub">
                    {i.location} · {formatDate(i.reportedDate)}
                  </div>
                </div>
                <Badge text={i.severity} tone={severityTone(i.severity)} />
                <Badge text={i.status} tone={statusTone(i.status)} />
                <button className="btn btn-outline btn-sm" onClick={() => navigate('incidents', i.incidentId)}>
                  Open
                </button>
              </div>
            ))
          )}
        </div>

        <div className="card">
          <div className="card-header">
            <h3>Latest Notifications</h3>
            <div className="spacer" />
            <button className="btn btn-ghost btn-sm" onClick={() => navigate('notifications')}>
              Inbox <IconArrowRight />
            </button>
          </div>
          {notifications.loading ? (
            <Spinner />
          ) : recentNotifs.length === 0 ? (
            <EmptyState icon="🔔" title="No notifications" hint="Status changes will show up here." />
          ) : (
            recentNotifs.map((n) => (
              <div className="list-item" key={n.notificationId}>
                <div className="avatar" style={{ background: 'var(--info-soft)', color: 'var(--info)' }}>
                  <IconBell />
                </div>
                <div className="grow">
                  <div className="row-main">{n.title}</div>
                  <div className="row-sub">{n.message}</div>
                  <div className="row-sub" style={{ marginTop: 2 }}>{formatDate(n.createdAt)}</div>
                </div>
                {n.status === 'UNREAD' ? <span className="badge tone-danger">new</span> : null}
              </div>
            ))
          )}
        </div>
      </div>

      <div className="grid grid-2" style={{ marginTop: 18 }}>
        <div className="card">
          <div className="card-header">
            <h3>Shelter Occupancy</h3>
            <div className="spacer" />
            <button className="btn btn-ghost btn-sm" onClick={() => navigate('shelters')}>
              Shelters <IconArrowRight />
            </button>
          </div>
          <div className="card-pad" style={{ display: 'grid', gap: 14 }}>
            {shelters.loading ? (
              <Spinner />
            ) : shl.length === 0 ? (
              <EmptyState icon="⛺" title="No shelters" hint="Register shelters to track capacity." />
            ) : (
              shl.slice(0, 6).map((s) => (
                <div key={s.shelterId}>
                  <div style={{ display: 'flex', justifyContent: 'space-between', marginBottom: 6 }}>
                    <span className="row-main small">{s.name}</span>
                    <span className="small mono">
                      {s.currentOccupancy}/{s.capacity}
                    </span>
                  </div>
                  <ProgressBar value={s.currentOccupancy} max={s.capacity} />
                </div>
              ))
            )}
          </div>
        </div>

        <div className="card">
          <div className="card-header">
            <h3>Relief Requests by Priority</h3>
            <div className="spacer" />
            <button className="btn btn-ghost btn-sm" onClick={() => navigate('relief')}>
              All requests <IconArrowRight />
            </button>
          </div>
          <div className="card-pad" style={{ display: 'grid', gap: 10 }}>
            {requests.loading ? (
              <Spinner />
            ) : req.length === 0 ? (
              <EmptyState icon="🤝" title="No relief requests" hint="Requests raised by citizens appear here." />
            ) : (
              req.slice(0, 7).map((r) => (
                <div className="list-item" key={r.requestId} style={{ padding: '8px 0' }}>
                  <div className="grow">
                    <div className="row-main">
                      {r.requestType.replace('_', ' ').toLowerCase()} — {r.victimName}
                    </div>
                    <div className="row-sub">#{r.requestId} · incident #{r.incidentId}</div>
                  </div>
                  <Badge text={r.priority} tone={priorityTone(r.priority)} />
                  <Badge text={r.status} tone={statusTone(r.status)} />
                </div>
              ))
            )}
          </div>
        </div>
      </div>

      <div className="card" style={{ marginTop: 18 }}>
        <div className="card-header">
          <h3>System Services</h3>
          <div className="spacer" />
          <span className="chip"><span className="dot" />All services healthy</span>
        </div>
        <div className="card-pad" style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fit, minmax(200px, 1fr))', gap: 14 }}>
          {[
            { name: 'Incident Service', icon: <IconAlert />, port: 8081 },
            { name: 'Relief Request Service', icon: <IconHandHeart />, port: 8082 },
            { name: 'Volunteer Service', icon: <IconUsers />, port: 8083 },
            { name: 'Resource Service', icon: <IconBox />, port: 8084 },
            { name: 'Notification Service', icon: <IconBell />, port: 8085 },
            { name: 'API Gateway', icon: <IconShield />, port: 8080 },
          ].map((s) => (
            <div key={s.name} className="chip" style={{ justifyContent: 'flex-start', padding: '10px 12px' }}>
              {s.icon}
              <span>{s.name}</span>
              <span className="muted">:{s.port}</span>
            </div>
          ))}
        </div>
      </div>
    </div>
  )
}

function StatTone(props: {
  label: string
  value: number | string
  foot: string
  icon: React.ReactNode
  color: string
  soft: string
}) {
  return (
    <div className="stat-card" style={{ ['--stat-color' as string]: props.color, ['--stat-soft' as string]: props.soft }}>
      <div className="stat-label">
        {props.label}
        <span className="stat-icon">{props.icon}</span>
      </div>
      <div className="stat-value">{props.value}</div>
      <div className="stat-foot">{props.foot}</div>
    </div>
  )
}
