import { useEffect, useMemo, useState } from 'react'
import { useApp } from '../AppContext'
import { api } from '../api'
import { useApi, formatDate } from '../hooks'
import type {
  AvailabilityStatus,
  Gender,
  Incident,
  ReliefRequest,
  Volunteer,
  VolunteerPayload,
} from '../types'
import {
  Badge,
  statusTone,
  EmptyState,
  Modal,
  Spinner,
  IconUserPlus,
  IconUsers,
} from '../components/ui'

const AV_STATUSES: AvailabilityStatus[] = ['AVAILABLE', 'ASSIGNED', 'ON_LEAVE', 'UNAVAILABLE']

export function Volunteers() {
  const { toast, refreshTick, bump } = useApp()
  const [filter, setFilter] = useState('ALL')
  const [showRegister, setShowRegister] = useState(false)
  const [selected, setSelected] = useState<Volunteer | null>(null)
  const [assignFor, setAssignFor] = useState<Volunteer | null>(null)

  const { data, loading, error, reload } = useApi<Volunteer[]>(() => api.volunteers.all(), [refreshTick])

  const filtered = useMemo(() => {
    let list = data ?? []
    if (filter !== 'ALL') list = list.filter((v) => v.availabilityStatus === filter)
    return [...list].sort((a, b) => a.lastName.localeCompare(b.lastName))
  }, [data, filter])

  const counts = useMemo(() => {
    const list = data ?? []
    return {
      total: list.length,
      available: list.filter((v) => v.availabilityStatus === 'AVAILABLE').length,
      assigned: list.filter((v) => v.availabilityStatus === 'ASSIGNED').length,
    }
  }, [data])

  return (
    <div>
      <div className="page-head">
        <div>
          <h2>Volunteers</h2>
          <p>Registered responders and their deployment status</p>
        </div>
        <div className="spacer" />
        <span className="chip"><IconUsers size={13} /> {counts.total} registered</span>
        <span className="chip tone-success">{counts.available} available</span>
        <span className="chip tone-info">{counts.assigned} deployed</span>
        <button className="btn btn-outline" onClick={reload}>Refresh</button>
        <button className="btn btn-primary" onClick={() => setShowRegister(true)}>
          <IconUserPlus /> Register Volunteer
        </button>
      </div>

      <div className="toolbar">
        <div className="seg">
          {['ALL', ...AV_STATUSES].map((s) => (
            <button key={s} className={filter === s ? 'active' : ''} onClick={() => setFilter(s)}>
              {s === 'ALL' ? 'All' : s.replace('_', ' ')}
            </button>
          ))}
        </div>
      </div>

      {error ? (
        <div className="card card-pad">
          <div className="form-error" style={{ margin: 0 }}>{error}</div>
        </div>
      ) : null}

      {loading ? (
        <Spinner />
      ) : filtered.length === 0 ? (
        <div className="card">
          <EmptyState icon="👥" title="No volunteers found" hint="Register volunteers to build your response team." />
        </div>
      ) : (
        <div className="table-wrap">
          <table className="data-table">
            <thead>
              <tr>
                <th>Name</th>
                <th>Contact</th>
                <th>Age</th>
                <th>City</th>
                <th>Skills</th>
                <th>Assigned To</th>
                <th>Availability</th>
                <th></th>
              </tr>
            </thead>
            <tbody>
              {filtered.map((v) => (
                <tr key={v.volunteerId}>
                  <td>
                    <div style={{ display: 'flex', alignItems: 'center', gap: 10 }}>
                      <div className="avatar">{v.firstName[0]}{v.lastName[0]}</div>
                      <div>
                        <div className="row-main">{v.firstName} {v.lastName}</div>
                        <div className="row-sub mono">#{v.volunteerId}</div>
                      </div>
                    </div>
                  </td>
                  <td>
                    <div className="row-sub">{v.email}</div>
                    <div className="row-sub">{v.phoneNumber}</div>
                  </td>
                  <td>{v.age}</td>
                  <td>{v.city ?? '—'}</td>
                  <td className="row-sub" style={{ maxWidth: 180, overflow: 'hidden', textOverflow: 'ellipsis', whiteSpace: 'nowrap' }}>
                    {v.skills || '—'}
                  </td>
                  <td className="row-sub">
                    {v.availabilityStatus === 'ASSIGNED' ? (
                      <>
                        incident #{v.assignedIncidentId}
                        {v.assignedReliefRequestId ? ` · request #${v.assignedReliefRequestId}` : ''}
                      </>
                    ) : '—'}
                  </td>
                  <td>
                    <Badge text={v.availabilityStatus} tone={statusTone(v.availabilityStatus)} />
                  </td>
                  <td>
                    <div className="cell-actions">
                      {v.availabilityStatus === 'AVAILABLE' && (
                        <button className="btn btn-outline btn-sm" onClick={() => setAssignFor(v)}>
                          Assign
                        </button>
                      )}
                      {v.availabilityStatus === 'ASSIGNED' && (
                        <button
                          className="btn btn-danger btn-sm"
                          onClick={async () => {
                            try {
                              await api.volunteers.release(v.volunteerId)
                              toast('success', `${v.firstName} released and marked available`)
                              bump()
                            } catch (err) {
                              toast('error', api.errorMessage(err))
                            }
                          }}
                        >
                          Release
                        </button>
                      )}
                      <button className="btn btn-ghost btn-sm" onClick={() => setSelected(v)}>
                        Details
                      </button>
                    </div>
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      )}

      {showRegister ? (
        <RegisterVolunteerModal onClose={() => setShowRegister(false)} onDone={() => { setShowRegister(false); toast('success', 'Volunteer registered'); bump() }} />
      ) : null}

      {selected ? (
        <VolunteerDetailModal volunteer={selected} onClose={() => setSelected(null)} />
      ) : null}

      {assignFor ? (
        <AssignVolunteerToIncidentModal volunteer={assignFor} onClose={() => setAssignFor(null)} onDone={() => { setAssignFor(null); toast('success', 'Volunteer assigned'); bump() }} />
      ) : null}
    </div>
  )
}

function RegisterVolunteerModal({ onClose, onDone }: { onClose: () => void; onDone: () => void }) {
  const [form, setForm] = useState<VolunteerPayload>({
    firstName: '',
    lastName: '',
    email: '',
    phoneNumber: '',
    age: 25,
    gender: 'MALE',
    skills: '',
    city: '',
    state: '',
    address: '',
    emergencyContactName: '',
    emergencyContactNumber: '',
  })
  const [submitting, setSubmitting] = useState(false)
  const [error, setError] = useState<string | null>(null)

  const set = <K extends keyof VolunteerPayload>(k: K, v: VolunteerPayload[K]) =>
    setForm((f) => ({ ...f, [k]: v }))

  async function submit() {
    if (!form.firstName || !form.lastName || !form.email || !form.phoneNumber || !form.emergencyContactName || !form.emergencyContactNumber) {
      setError('First name, last name, email, phone and emergency contact are required.')
      return
    }
    if (!/^[0-9]{10}$/.test(form.phoneNumber) || !/^[0-9]{10}$/.test(form.emergencyContactNumber)) {
      setError('Phone and emergency contact must be exactly 10 digits.')
      return
    }
    setSubmitting(true)
    setError(null)
    try {
      await api.volunteers.register(form)
      onDone()
    } catch (err) {
      setError(api.errorMessage(err))
      setSubmitting(false)
    }
  }

  return (
    <Modal title="Register a Volunteer" onClose={onClose} wide>
      {error ? <div className="form-error">{error}</div> : null}
      <div className="form-grid">
        <div className="field">
          <label>First name *</label>
          <input className="input" value={form.firstName} onChange={(e) => set('firstName', e.target.value)} />
        </div>
        <div className="field">
          <label>Last name *</label>
          <input className="input" value={form.lastName} onChange={(e) => set('lastName', e.target.value)} />
        </div>
        <div className="field">
          <label>Email *</label>
          <input className="input" type="email" value={form.email} onChange={(e) => set('email', e.target.value)} />
        </div>
        <div className="field">
          <label>Phone (10 digits) *</label>
          <input className="input" value={form.phoneNumber} onChange={(e) => set('phoneNumber', e.target.value.replace(/[^0-9]/g, ''))} maxLength={10} />
        </div>
        <div className="field">
          <label>Age *</label>
          <input className="input" type="number" min={18} max={65} value={form.age} onChange={(e) => set('age', Number(e.target.value))} />
        </div>
        <div className="field">
          <label>Gender *</label>
          <select className="select" value={form.gender} onChange={(e) => set('gender', e.target.value as Gender)}>
            <option>MALE</option>
            <option>FEMALE</option>
            <option>OTHER</option>
          </select>
        </div>
        <div className="field" style={{ gridColumn: '1 / -1' }}>
          <label>Skills</label>
          <input className="input" value={form.skills} onChange={(e) => set('skills', e.target.value)} placeholder="e.g. First aid, driving, rescue, cooking" />
        </div>
        <div className="field">
          <label>City</label>
          <input className="input" value={form.city} onChange={(e) => set('city', e.target.value)} />
        </div>
        <div className="field">
          <label>State</label>
          <input className="input" value={form.state} onChange={(e) => set('state', e.target.value)} />
        </div>
        <div className="field" style={{ gridColumn: '1 / -1' }}>
          <label>Address</label>
          <input className="input" value={form.address} onChange={(e) => set('address', e.target.value)} />
        </div>
        <div className="field">
          <label>Emergency contact name *</label>
          <input className="input" value={form.emergencyContactName} onChange={(e) => set('emergencyContactName', e.target.value)} />
        </div>
        <div className="field">
          <label>Emergency contact number *</label>
          <input className="input" value={form.emergencyContactNumber} onChange={(e) => set('emergencyContactNumber', e.target.value.replace(/[^0-9]/g, ''))} maxLength={10} />
        </div>
      </div>
      <div className="form-actions">
        <button className="btn btn-ghost" onClick={onClose}>Cancel</button>
        <button className="btn btn-primary" disabled={submitting} onClick={submit}>
          {submitting ? 'Registering…' : 'Register Volunteer'}
        </button>
      </div>
    </Modal>
  )
}

function VolunteerDetailModal({ volunteer, onClose }: { volunteer: Volunteer; onClose: () => void }) {
  const { toast, bump } = useApp()
  const [updating, setUpdating] = useState(false)

  async function setStatus(status: AvailabilityStatus) {
    setUpdating(true)
    try {
      await api.volunteers.updateStatus(volunteer.volunteerId, status)
      toast('success', 'Availability updated')
      bump()
      onClose()
    } catch (err) {
      toast('error', api.errorMessage(err))
      setUpdating(false)
    }
  }

  return (
    <Modal title={`${volunteer.firstName} ${volunteer.lastName}`} onClose={onClose}>
      <div className="detail-grid" style={{ marginBottom: 16 }}>
        <div className="detail-item"><div className="k">Email</div><div className="v small">{volunteer.email}</div></div>
        <div className="detail-item"><div className="k">Phone</div><div className="v">{volunteer.phoneNumber}</div></div>
        <div className="detail-item"><div className="k">Age / Gender</div><div className="v">{volunteer.age} · {volunteer.gender.toLowerCase()}</div></div>
        <div className="detail-item"><div className="k">Availability</div><div className="v"><Badge text={volunteer.availabilityStatus} tone={statusTone(volunteer.availabilityStatus)} /></div></div>
        <div className="detail-item"><div className="k">City</div><div className="v">{volunteer.city ?? '—'}, {volunteer.state ?? ''}</div></div>
        <div className="detail-item"><div className="k">Registered</div><div className="v small">{formatDate(volunteer.registrationDate)}</div></div>
      </div>
      <div className="detail-item" style={{ marginBottom: 16 }}>
        <div className="k">Skills</div>
        <div className="soft">{volunteer.skills || '—'}</div>
      </div>
      <div className="detail-item" style={{ marginBottom: 16 }}>
        <div className="k">Emergency contact</div>
        <div className="soft">{volunteer.emergencyContactName} · {volunteer.emergencyContactNumber}</div>
      </div>
      <div className="form-actions">
        {volunteer.availabilityStatus !== 'ASSIGNED' && (
          <>
            <button className="btn btn-outline btn-sm" disabled={updating} onClick={() => setStatus('AVAILABLE')}>Available</button>
            <button className="btn btn-outline btn-sm" disabled={updating} onClick={() => setStatus('ON_LEAVE')}>On leave</button>
            <button className="btn btn-outline btn-sm" disabled={updating} onClick={() => setStatus('UNAVAILABLE')}>Unavailable</button>
          </>
        )}
      </div>
    </Modal>
  )
}

function AssignVolunteerToIncidentModal({ volunteer, onClose, onDone }: { volunteer: Volunteer; onClose: () => void; onDone: () => void }) {
  const [incidents, setIncidents] = useState<Incident[]>([])
  const [requests, setRequests] = useState<ReliefRequest[]>([])
  const [incidentId, setIncidentId] = useState<number>(0)
  const [reliefRequestId, setReliefRequestId] = useState<number>(0)
  const [area, setArea] = useState('')
  const [error, setError] = useState<string | null>(null)
  const [submitting, setSubmitting] = useState(false)
  const [loaded, setLoaded] = useState(false)

  useEffect(() => {
    api.incidents
      .list()
      .then((list) => {
        setIncidents(list)
        if (list.length > 0) {
          setIncidentId(list[0].incidentId)
          setArea(list[0].location)
        }
      })
      .catch((err) => setError(api.errorMessage(err)))
      .finally(() => setLoaded(true))
  }, [])

  useEffect(() => {
    if (!incidentId) return
    api.reliefRequests
      .list({ incidentId: String(incidentId) })
      .then((list) => {
        setRequests(list.filter((r) => ['PENDING', 'ASSIGNED', 'IN_PROGRESS'].includes(r.status)))
        setReliefRequestId(0)
      })
      .catch(() => setRequests([]))
  }, [incidentId])

  async function assign() {
    setSubmitting(true)
    setError(null)
    try {
      await api.volunteers.assign(volunteer.volunteerId, {
        incidentId,
        assignedArea: area || 'Relief zone',
        reliefRequestId: reliefRequestId || null,
      })
      onDone()
    } catch (err) {
      setError(api.errorMessage(err))
      setSubmitting(false)
    }
  }

  return (
    <Modal title={`Assign ${volunteer.firstName} ${volunteer.lastName}`} onClose={onClose}>
      {error ? <div className="form-error">{error}</div> : null}
      {!loaded ? (
        <Spinner />
      ) : incidents.length === 0 ? (
        <EmptyState icon="🗺️" title="No incidents to assign to" hint="Report an incident first." />
      ) : (
        <>
          <div className="field">
            <label>Incident</label>
            <select
              className="select"
              value={incidentId}
              onChange={(e) => {
                const id = Number(e.target.value)
                setIncidentId(id)
                setArea(incidents.find((i) => i.incidentId === id)?.location ?? '')
              }}
            >
              {incidents.map((i) => (
                <option key={i.incidentId} value={i.incidentId}>
                  #{i.incidentId} — {i.title} ({i.location})
                </option>
              ))}
            </select>
          </div>
          <div className="field">
            <label>Linked relief request (optional)</label>
            <select className="select" value={reliefRequestId} onChange={(e) => setReliefRequestId(Number(e.target.value))}>
              <option value={0}>— None —</option>
              {requests.map((r) => (
                <option key={r.requestId} value={r.requestId}>
                  #{r.requestId} — {r.requestType.toLowerCase()} ({r.victimName})
                </option>
              ))}
            </select>
          </div>
          <div className="field">
            <label>Assigned area</label>
            <input className="input" value={area} onChange={(e) => setArea(e.target.value)} />
          </div>
          <div className="form-actions">
            <button className="btn btn-ghost" onClick={onClose}>Cancel</button>
            <button className="btn btn-primary" disabled={submitting} onClick={assign}>
              {submitting ? 'Assigning…' : 'Assign Volunteer'}
            </button>
          </div>
        </>
      )}
    </Modal>
  )
}
