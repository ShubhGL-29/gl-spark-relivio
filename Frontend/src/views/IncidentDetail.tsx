import { useState } from 'react'
import { useApp } from '../AppContext'
import { api } from '../api'
import { useApi, formatDate } from '../hooks'
import { canManageIncidents } from '../auth'
import { incidentNextStates, isRequestOpen } from '../helpers'
import type { Incident, ReliefRequest } from '../types'
import {
  Badge,
  severityTone,
  statusTone,
  priorityTone,
  EmptyState,
  Spinner,
  IconArrowRight,
  IconMapPin,
  IconClock,
  IconHandHeart,
  IconPlus,
} from '../components/ui'

export function IncidentDetail({ incidentId }: { incidentId: number }) {
  const { navigate, toast, refreshTick, bump, user } = useApp()
  const [updating, setUpdating] = useState(false)
  const role = user?.role ?? 'ADMIN'

  const incident = useApi<Incident>(() => api.incidents.get(incidentId), [incidentId, refreshTick])
  const requests = useApi<ReliefRequest[]>(
    () => api.reliefRequests.list({ incidentId: String(incidentId) }),
    [incidentId, refreshTick],
  )

  const inc = incident.data

  async function changeStatus(status: string) {
    setUpdating(true)
    try {
      await api.incidents.patch(incidentId, { status })
      toast('success', `Incident status updated to ${status.replace('_', ' ')}`)
      bump()
    } catch (err) {
      toast('error', api.errorMessage(err))
    } finally {
      setUpdating(false)
    }
  }

  if (incident.loading) return <Spinner />
  if (!inc) {
    return (
      <div className="card">
        <EmptyState icon="🗺️" title="Incident not found" hint="It may have been deleted." />
      </div>
    )
  }

  const openCount = (requests.data ?? []).filter((r) => isRequestOpen(r.status)).length
  const nextStates = incidentNextStates(inc.status)

  return (
    <div>
      <div className="page-head">
        <button className="btn btn-ghost" onClick={() => navigate('incidents')}>
          ← Back to incidents
        </button>
        <div className="spacer" />
        <button className="btn btn-primary" onClick={() => navigate('relief', incidentId)}>
          <IconPlus /> Raise relief request
        </button>
      </div>

      <div className="hero-strip" style={{ marginBottom: 18 }}>
        <div className="hero-icon">
          <IconHandHeart />
        </div>
        <div>
          <h2>Incident #{inc.incidentId} — {inc.title}</h2>
          <p>
            {inc.disasterType} at {inc.location} · reported {formatDate(inc.reportedDate)}
          </p>
        </div>
        <Badge text={inc.status} tone={statusTone(inc.status)} />
      </div>

      <div className="grid grid-2">
        <div className="card">
          <div className="card-header">
            <h3>Incident Details</h3>
            <div className="spacer" />
            <Badge text={inc.severity} tone={severityTone(inc.severity)} />
          </div>
          <div className="card-pad">
            <div className="detail-grid">
              <div className="detail-item">
                <div className="k">Disaster type</div>
                <div className="v">{inc.disasterType}</div>
              </div>
              <div className="detail-item">
                <div className="k">Location</div>
                <div className="v"><IconMapPin size={14} style={{ verticalAlign: -2 }} /> {inc.location}</div>
              </div>
              <div className="detail-item">
                <div className="k">Reported by</div>
                <div className="v">{inc.reporterName ?? 'Anonymous'}</div>
              </div>
              <div className="detail-item">
                <div className="k">Contact</div>
                <div className="v">{inc.reporterContact ?? '—'}</div>
              </div>
              <div className="detail-item">
                <div className="k">Reported</div>
                <div className="v small">{formatDate(inc.reportedDate)}</div>
              </div>
              <div className="detail-item">
                <div className="k">Last updated</div>
                <div className="v small"><IconClock size={14} style={{ verticalAlign: -2 }} /> {formatDate(inc.lastUpdated)}</div>
              </div>
            </div>
            <div className="detail-item" style={{ marginTop: 18 }}>
              <div className="k">Description</div>
              <div className="soft" style={{ marginTop: 4 }}>{inc.description}</div>
            </div>
          </div>
        </div>

        <div className="card">
          <div className="card-header">
            <h3>Update Status</h3>
            <div className="spacer" />
            {openCount > 0 ? (
              <span className="chip tone-warning">{openCount} open relief request(s)</span>
            ) : (
              <span className="chip tone-success">No open requests</span>
            )}
          </div>
          <div className="card-pad">
            <p className="soft small" style={{ marginTop: 0 }}>
              Current status:{' '}
              <Badge text={inc.status} tone={statusTone(inc.status)} />
            </p>
            {canManageIncidents(role) ? (
              <>
                <div style={{ display: 'flex', flexWrap: 'wrap', gap: 8 }}>
                  {nextStates.map((s) => (
                    <button key={s} className="btn btn-outline btn-sm" disabled={updating} onClick={() => changeStatus(s)}>
                      Mark {s.replace('_', ' ')}
                    </button>
                  ))}
                  {nextStates.length === 0 && (
                    <span className="muted small">This incident is in a terminal state.</span>
                  )}
                </div>
                {inc.status === 'RESOLVED' && (
                  <p className="small soft" style={{ marginTop: 14 }}>
                    Note: an incident cannot be marked Resolved while it still has pending or in-progress relief requests.
                  </p>
                )}
              </>
            ) : (
              <p className="soft small" style={{ marginBottom: 0 }}>
                Status updates are managed by administrators and volunteers.
              </p>
            )}
          </div>
        </div>
      </div>

      <div className="card" style={{ marginTop: 18 }}>
        <div className="card-header">
          <h3>Linked Relief Requests</h3>
          <div className="spacer" />
          <button className="btn btn-outline btn-sm" onClick={() => navigate('relief', incidentId)}>
            View all <IconArrowRight />
          </button>
        </div>
        {requests.loading ? (
          <Spinner />
        ) : (requests.data ?? []).length === 0 ? (
          <EmptyState icon="🤝" title="No relief requests for this incident" hint="Raise a request to match volunteers and resources." />
        ) : (
          <div className="table-wrap">
            <table className="data-table">
              <thead>
                <tr>
                  <th>ID</th>
                  <th>Victim</th>
                  <th>Type</th>
                  <th>Priority</th>
                  <th>Assigned</th>
                  <th>Status</th>
                  <th></th>
                </tr>
              </thead>
              <tbody>
                {(requests.data ?? []).map((r) => (
                  <tr key={r.requestId}>
                    <td className="mono">#{r.requestId}</td>
                    <td>
                      <div className="row-main">{r.victimName}</div>
                      <div className="row-sub">{r.phone}</div>
                    </td>
                    <td>{r.requestType.replace('_', ' ').toLowerCase()}</td>
                    <td>
                      <Badge text={r.priority} tone={priorityTone(r.priority)} />
                    </td>
                    <td className="row-sub">
                      {r.assignedVolunteerName ?? '—'}
                      {r.allocatedResourceName ? ` / ${r.allocatedResourceName}` : ''}
                    </td>
                    <td>
                      <Badge text={r.status} tone={statusTone(r.status)} />
                    </td>
                    <td>
                      <button className="btn btn-outline btn-sm" onClick={() => navigate('relief')}>
                        Open
                      </button>
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        )}
      </div>
    </div>
  )
}
