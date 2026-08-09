import { useMemo, useState } from 'react'
import { useApp } from '../AppContext'
import { api } from '../api'
import { useApi, formatDate } from '../hooks'
import type { AuthUser, Incident, IncidentRequest, Severity } from '../types'
import {
  Badge,
  severityTone,
  statusTone,
  EmptyState,
  Modal,
  Spinner,
  IconPlus,
  IconMapPin,
  IconSearch,
} from '../components/ui'

export function Incidents() {
  const { navigate, toast, refreshTick, bump, user } = useApp()
  const [statusFilter, setStatusFilter] = useState('ALL')
  const [query, setQuery] = useState('')
  const [showCreate, setShowCreate] = useState(false)

  const { data, loading, error, reload } = useApi<Incident[]>(() => api.incidents.list(), [refreshTick])

  const filtered = useMemo(() => {
    let list = data ?? []
    if (statusFilter !== 'ALL') list = list.filter((i) => i.status === statusFilter)
    if (query.trim()) {
      const q = query.trim().toLowerCase()
      list = list.filter(
        (i) =>
          String(i.incidentId).includes(q) ||
          i.title.toLowerCase().includes(q) ||
          i.location.toLowerCase().includes(q) ||
          i.disasterType.toLowerCase().includes(q),
      )
    }
    return [...list].sort(
      (a, b) => new Date(b.reportedDate).getTime() - new Date(a.reportedDate).getTime(),
    )
  }, [data, statusFilter, query])

  return (
    <div>
      <div className="page-head">
        <div>
          <h2>Incidents</h2>
          <p>Reported disaster events across the region</p>
        </div>
        <div className="spacer" />
        <div className="toolbar" style={{ margin: 0 }}>
          <div style={{ position: 'relative' }}>
            <input
              className="input"
              placeholder="Search by ID, title, location…"
              value={query}
              onChange={(e) => setQuery(e.target.value)}
              style={{ paddingLeft: 34, width: 240 }}
            />
            <span style={{ position: 'absolute', left: 10, top: 9, color: 'var(--muted)' }}>
              <IconSearch size={16} />
            </span>
          </div>
          <button className="btn btn-outline" onClick={reload}>
            Refresh
          </button>
          <button className="btn btn-primary" onClick={() => setShowCreate(true)}>
            <IconPlus /> Report Incident
          </button>
        </div>
      </div>

      <div className="toolbar">
        <div className="seg">
          {['ALL', 'REPORTED', 'VERIFIED', 'IN_PROGRESS', 'RESOLVED', 'CLOSED', 'REOPENED'].map((s) => (
            <button key={s} className={statusFilter === s ? 'active' : ''} onClick={() => setStatusFilter(s)}>
              {s === 'ALL' ? 'All' : s.replace('_', ' ')}
            </button>
          ))}
        </div>
      </div>

      {error ? (
        <div className="card card-pad">
          <div className="form-error" style={{ margin: 0 }}>
            {error} — make sure the API Gateway is running on port 8080.
          </div>
        </div>
      ) : null}

      {loading ? (
        <Spinner />
      ) : filtered.length === 0 ? (
        <div className="card">
          <EmptyState icon="🗺️" title="No incidents found" hint="Try a different filter or report a new incident." />
        </div>
      ) : (
        <div className="table-wrap">
          <table className="data-table">
            <thead>
              <tr>
                <th>ID</th>
                <th>Title</th>
                <th>Disaster</th>
                <th>Severity</th>
                <th>Location</th>
                <th>Reporter</th>
                <th>Reported</th>
                <th>Status</th>
                <th></th>
              </tr>
            </thead>
            <tbody>
              {filtered.map((i) => (
                <tr key={i.incidentId} style={{ cursor: 'pointer' }} onClick={() => navigate('incidents', i.incidentId)}>
                  <td className="mono">#{i.incidentId}</td>
                  <td>
                    <div className="row-main">{i.title}</div>
                    <div className="row-sub" style={{ maxWidth: 260, overflow: 'hidden', textOverflow: 'ellipsis', whiteSpace: 'nowrap' }}>
                      {i.description}
                    </div>
                  </td>
                  <td>
                    <span className="chip">{i.disasterType}</span>
                  </td>
                  <td>
                    <Badge text={i.severity} tone={severityTone(i.severity)} />
                  </td>
                  <td>
                    <span style={{ display: 'inline-flex', alignItems: 'center', gap: 5 }}>
                      <IconMapPin size={13} /> {i.location}
                    </span>
                  </td>
                  <td>{i.reporterName ?? '—'}</td>
                  <td className="row-sub">{formatDate(i.reportedDate)}</td>
                  <td>
                    <Badge text={i.status} tone={statusTone(i.status)} />
                  </td>
                  <td>
                    <button className="btn btn-outline btn-sm" onClick={(e) => { e.stopPropagation(); navigate('incidents', i.incidentId) }}>
                      Open
                    </button>
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      )}

      {showCreate ? (
        <CreateIncidentModal
          citizen={user?.role === 'CITIZEN' ? user : undefined}
          onClose={() => setShowCreate(false)}
          onCreated={(id) => { setShowCreate(false); toast('success', 'Incident reported'); bump(); navigate('incidents', id) }}
        />
      ) : null}
    </div>
  )
}

function CreateIncidentModal({ citizen, onClose, onCreated }: { citizen?: AuthUser; onClose: () => void; onCreated: (id: number) => void }) {
  const [form, setForm] = useState<IncidentRequest>({
    title: '',
    disasterType: 'Flood',
    severity: 'MEDIUM',
    location: '',
    description: '',
    reporterName: citizen?.name ?? '',
    reporterContact: citizen?.phone ?? '',
    reporterId: citizen?.id ?? null,
  })
  const [submitting, setSubmitting] = useState(false)
  const [error, setError] = useState<string | null>(null)

  const set = (k: keyof IncidentRequest, v: string) => setForm((f) => ({ ...f, [k]: v }))

  async function submit() {
    if (!form.title.trim() || !form.location.trim() || !form.description.trim()) {
      setError('Title, location and description are required.')
      return
    }
    if (!form.reporterName?.trim()) {
      setError('Reporter name is required.')
      return
    }
    if (form.reporterContact && !/^[0-9]{10}$/.test(form.reporterContact.trim())) {
      setError('Contact number must be exactly 10 digits.')
      return
    }
    setSubmitting(true)
    setError(null)
    try {
      const created = await api.incidents.create(form)
      onCreated(created.incidentId)
    } catch (err) {
      setError(api.errorMessage(err))
      setSubmitting(false)
    }
  }

  return (
    <Modal title="Report a Disaster Incident" onClose={onClose} wide>
      {error ? <div className="form-error">{error}</div> : null}
      <div className="form-grid">
        <div className="field" style={{ gridColumn: '1 / -1' }}>
          <label>Incident title *</label>
          <input className="input" value={form.title} onChange={(e) => set('title', e.target.value)} placeholder="e.g. Flash floods in low-lying areas" />
        </div>
        <div className="field">
          <label>Disaster type *</label>
          <select className="select" value={form.disasterType} onChange={(e) => set('disasterType', e.target.value)}>
            {['Flood', 'Earthquake', 'Fire', 'Cyclone', 'Landslide', 'Drought', 'Pandemic', 'Other'].map((d) => (
              <option key={d}>{d}</option>
            ))}
          </select>
        </div>
        <div className="field">
          <label>Severity *</label>
          <select className="select" value={form.severity} onChange={(e) => set('severity', e.target.value as Severity)}>
            {(['LOW', 'MEDIUM', 'HIGH', 'CRITICAL'] as const).map((s) => (
              <option key={s}>{s}</option>
            ))}
          </select>
        </div>
        <div className="field" style={{ gridColumn: '1 / -1' }}>
          <label>Location *</label>
          <input className="input" value={form.location} onChange={(e) => set('location', e.target.value)} placeholder="Area / town, e.g. Riverside Colony, Patna" />
        </div>
        <div className="field" style={{ gridColumn: '1 / -1' }}>
          <label>Description *</label>
          <textarea className="textarea" value={form.description} onChange={(e) => set('description', e.target.value)} placeholder="What is happening? How many people are affected?" />
        </div>
        <div className="field">
          <label>Reporter name *</label>
          <input className="input" value={form.reporterName ?? ''} onChange={(e) => set('reporterName', e.target.value)} />
        </div>
        <div className="field">
          <label>Reporter contact</label>
          <input className="input" value={form.reporterContact ?? ''} onChange={(e) => set('reporterContact', e.target.value.replace(/[^0-9]/g, ''))} placeholder="10-digit number" maxLength={10} />
        </div>
      </div>
      <div className="form-actions">
        <button className="btn btn-ghost" onClick={onClose}>Cancel</button>
        <button className="btn btn-primary" disabled={submitting} onClick={submit}>
          {submitting ? 'Submitting…' : 'Report Incident'}
        </button>
      </div>
    </Modal>
  )
}
