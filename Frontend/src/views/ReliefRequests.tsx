import { useEffect, useMemo, useState } from 'react'
import { useApp } from '../AppContext'
import { api } from '../api'
import { useApi, formatDate } from '../hooks'
import { canAssign } from '../auth'
import { requestNextStates, isRequestOpen } from '../helpers'
import type {
  AuthUser,
  Incident,
  Priority,
  ReliefRequest,
  ReliefRequestPayload,
  RequestType,
  Resource,
  Shelter,
  Volunteer,
} from '../types'
import {
  Badge,
  priorityTone,
  statusTone,
  EmptyState,
  Modal,
  Spinner,
  IconPlus,
  IconSearch,
  IconUsers,
  IconBox,
  IconTent,
} from '../components/ui'

export function ReliefRequests() {
  const { toast, refreshTick, bump, incidentId: contextIncidentId, user } = useApp()
  const [statusFilter, setStatusFilter] = useState('ALL')
  const [priorityFilter, setPriorityFilter] = useState('ALL')
  const [query, setQuery] = useState('')
  const [showCreate, setShowCreate] = useState(Boolean(contextIncidentId))
  const [selected, setSelected] = useState<ReliefRequest | null>(null)
  const [assignFor, setAssignFor] = useState<ReliefRequest | null>(null)
  const [allocateFor, setAllocateFor] = useState<ReliefRequest | null>(null)
  const [allocateShelterFor, setAllocateShelterFor] = useState<ReliefRequest | null>(null)

  const { data, loading, error, reload } = useApi<ReliefRequest[]>(
    () => api.reliefRequests.list(),
    [refreshTick],
  )

  const filtered = useMemo(() => {
    let list = data ?? []
    if (statusFilter !== 'ALL') list = list.filter((r) => r.status === statusFilter)
    if (priorityFilter !== 'ALL') list = list.filter((r) => r.priority === priorityFilter)
    if (query.trim()) {
      const q = query.trim().toLowerCase()
      list = list.filter(
        (r) =>
          String(r.requestId).includes(q) ||
          String(r.incidentId).includes(q) ||
          r.victimName.toLowerCase().includes(q),
      )
    }
    return [...list].sort(
      (a, b) => new Date(b.requestDate).getTime() - new Date(a.requestDate).getTime(),
    )
  }, [data, statusFilter, priorityFilter, query])

  const openCount = (data ?? []).filter((r) => isRequestOpen(r.status)).length
  const canManage = canAssign(user?.role ?? 'ADMIN')

  return (
    <div>
      <div className="page-head">
        <div>
          <h2>Relief Requests</h2>
          <p>Needs raised by citizens, matched to volunteers and resources</p>
        </div>
        <div className="spacer" />
        <span className="chip">{openCount} open</span>
        <button className="btn btn-outline" onClick={reload}>Refresh</button>
        <button className="btn btn-primary" onClick={() => setShowCreate(true)}>
          <IconPlus /> New Relief Request
        </button>
      </div>

      {contextIncidentId != null ? (
        <div className="card card-pad" style={{ marginBottom: 16, background: 'var(--info-soft)', borderColor: 'transparent' }}>
          <div className="soft" style={{ display: 'flex', alignItems: 'center', gap: 10 }}>
            <IconPlus size={15} /> Drafting a relief request linked to incident #{contextIncidentId} — fill in the details below.
          </div>
        </div>
      ) : null}

      <div className="toolbar">
        <div style={{ position: 'relative' }}>
          <input
            className="input"
            placeholder="Search by request ID, incident ID…"
            value={query}
            onChange={(e) => setQuery(e.target.value)}
            style={{ paddingLeft: 34, width: 240 }}
          />
          <span style={{ position: 'absolute', left: 10, top: 9, color: 'var(--muted)' }}>
            <IconSearch size={16} />
          </span>
        </div>
        <div className="seg">
          {['ALL', 'PENDING', 'ASSIGNED', 'IN_PROGRESS', 'FULFILLED', 'CLOSED', 'CANCELLED'].map((s) => (
            <button key={s} className={statusFilter === s ? 'active' : ''} onClick={() => setStatusFilter(s)}>
              {s === 'ALL' ? 'All' : s.replace('_', ' ')}
            </button>
          ))}
        </div>
        <select
          className="select"
          style={{ width: 150 }}
          value={priorityFilter}
          onChange={(e) => setPriorityFilter(e.target.value)}
        >
          <option value="ALL">All priorities</option>
          <option value="URGENT">Urgent</option>
          <option value="HIGH">High</option>
          <option value="MEDIUM">Medium</option>
          <option value="LOW">Low</option>
        </select>
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
          <EmptyState icon="🤝" title="No relief requests" hint="Raise a request or change the filters." />
        </div>
      ) : (
        <div className="table-wrap">
          <table className="data-table">
            <thead>
              <tr>
                <th>ID</th>
                <th>Victim</th>
                <th>Type</th>
                <th>Priority</th>
                <th>Incident</th>
                <th>Assigned</th>
                <th>Status</th>
                <th></th>
              </tr>
            </thead>
            <tbody>
              {filtered.map((r) => (
                <tr key={r.requestId}>
                  <td className="mono">#{r.requestId}</td>
                  <td>
                    <div className="row-main">{r.victimName}</div>
                    <div className="row-sub">{r.email}</div>
                  </td>
                  <td>{r.requestType.replace('_', ' ').toLowerCase()}</td>
                  <td>
                    <Badge text={r.priority} tone={priorityTone(r.priority)} />
                  </td>
                  <td className="mono">#{r.incidentId}</td>
                  <td>
                    <div className="row-sub">
                      {r.assignedVolunteerName ?? '—'}
                      {r.allocatedResourceName ? ` · ${r.allocatedResourceName}` : ''}
                      {r.allocatedShelterName ? ` · ${r.allocatedShelterName}` : ''}
                    </div>
                  </td>
                  <td>
                    <Badge text={r.status} tone={statusTone(r.status)} />
                  </td>
                  <td>
                    <div className="cell-actions">
                      {canManage && isRequestOpen(r.status) && r.assignedVolunteerId == null && (
                        <button className="btn btn-outline btn-sm" title="Assign volunteer" onClick={() => setAssignFor(r)}>
                          <IconUsers size={13} /> Assign
                        </button>
                      )}
                      {canManage && isRequestOpen(r.status) && r.allocatedResourceId == null && (
                        <button className="btn btn-outline btn-sm" title="Allocate resource" onClick={() => setAllocateFor(r)}>
                          <IconBox size={13} /> Allocate
                        </button>
                      )}
                      {canManage && isRequestOpen(r.status) && r.allocatedShelterId == null && (
                        <button className="btn btn-outline btn-sm" title="Allocate shelter" onClick={() => setAllocateShelterFor(r)}>
                          <IconTent size={13} /> Shelter
                        </button>
                      )}
                      <button className="btn btn-ghost btn-sm" onClick={() => setSelected(r)}>
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

      {showCreate ? (
        <CreateRequestModal
          presetIncidentId={contextIncidentId ?? undefined}
          citizen={user?.role === 'CITIZEN' ? user : undefined}
          onClose={() => setShowCreate(false)}
          onCreated={() => {
            setShowCreate(false)
            toast('success', 'Relief request submitted')
            bump()
          }}
        />
      ) : null}

      {selected ? (
        <RequestDetailModal request={selected} onClose={() => setSelected(null)} onChanged={() => { bump(); setSelected(null) }} />
      ) : null}

      {assignFor ? (
        <AssignVolunteerModal request={assignFor} onClose={() => setAssignFor(null)} onDone={() => { toast('success', 'Volunteer assigned'); setAssignFor(null); bump() }} />
      ) : null}

      {allocateFor ? (
        <AllocateResourceModal request={allocateFor} onClose={() => setAllocateFor(null)} onDone={() => { toast('success', 'Resource allocated'); setAllocateFor(null); bump() }} />
      ) : null}

      {allocateShelterFor ? (
        <AllocateShelterModal request={allocateShelterFor} onClose={() => setAllocateShelterFor(null)} onDone={() => { toast('success', 'Shelter allocated'); setAllocateShelterFor(null); bump() }} />
      ) : null}
    </div>
  )
}

function CreateRequestModal({
  presetIncidentId,
  citizen,
  onClose,
  onCreated,
}: {
  presetIncidentId?: number
  citizen?: AuthUser
  onClose: () => void
  onCreated: () => void
}) {
  const [form, setForm] = useState<ReliefRequestPayload>({
    incidentId: presetIncidentId ?? 0,
    victimName: citizen?.name ?? '',
    phone: citizen?.phone ?? '',
    email: citizen?.email ?? '',
    requestType: 'FOOD',
    priority: 'MEDIUM',
    description: '',
  })
  const [submitting, setSubmitting] = useState(false)
  const [error, setError] = useState<string | null>(null)
  const [incidentList, setIncidentList] = useState<Incident[] | null>(null)
  const [incidentError, setIncidentError] = useState(false)

  useEffect(() => {
    api.incidents
      .list()
      .then(setIncidentList)
      .catch(() => setIncidentError(true))
  }, [])

  const set = <K extends keyof ReliefRequestPayload>(k: K, v: ReliefRequestPayload[K]) =>
    setForm((f) => ({ ...f, [k]: v }))

  async function submit() {
    if (!form.incidentId || !form.victimName.trim() || !form.phone.trim() || !form.email.trim()) {
      setError('Incident, victim name, phone and email are required.')
      return
    }
    if (!/^[0-9]{10}$/.test(form.phone.trim())) {
      setError('Phone number must be exactly 10 digits.')
      return
    }
    setSubmitting(true)
    setError(null)
    try {
      await api.reliefRequests.create({ ...form, incidentId: Number(form.incidentId) })
      onCreated()
    } catch (err) {
      setError(api.errorMessage(err))
      setSubmitting(false)
    }
  }

  return (
    <Modal title="Raise a Relief Request" onClose={onClose} wide>
      {error ? <div className="form-error">{error}</div> : null}
      <div className="form-grid">
        <div className="field">
          <label>Linked incident *</label>
          <select
            className="select"
            value={form.incidentId}
            onChange={(e) => set('incidentId', Number(e.target.value))}
            disabled={presetIncidentId != null}
          >
            <option value={0}>Select incident…</option>
            {incidentList?.map((i) => (
              <option key={i.incidentId} value={i.incidentId}>
                #{i.incidentId} — {i.title}
              </option>
            ))}
          </select>
          {incidentError && <div className="hint tone-danger">Could not load incidents.</div>}
        </div>
        <div className="field">
          <label>Request type *</label>
          <select className="select" value={form.requestType} onChange={(e) => set('requestType', e.target.value as RequestType)}>
            {(['MEDICAL', 'FOOD', 'WATER', 'SHELTER', 'EVACUATION', 'TRANSPORTATION', 'OTHER'] as const).map((t) => (
              <option key={t}>{t}</option>
            ))}
          </select>
        </div>
        <div className="field">
          <label>Victim name *</label>
          <input className="input" value={form.victimName} onChange={(e) => set('victimName', e.target.value)} />
        </div>
        <div className="field">
          <label>Priority *</label>
          <select className="select" value={form.priority} onChange={(e) => set('priority', e.target.value as Priority)}>
            {(['URGENT', 'HIGH', 'MEDIUM', 'LOW'] as const).map((p) => (
              <option key={p}>{p}</option>
            ))}
          </select>
        </div>
        <div className="field">
          <label>Phone *</label>
          <input className="input" value={form.phone} onChange={(e) => set('phone', e.target.value.replace(/[^0-9]/g, ''))} placeholder="10-digit number" maxLength={10} />
        </div>
        <div className="field">
          <label>Email *</label>
          <input className="input" value={form.email} onChange={(e) => set('email', e.target.value)} />
        </div>
        <div className="field" style={{ gridColumn: '1 / -1' }}>
          <label>Description</label>
          <textarea className="textarea" value={form.description} onChange={(e) => set('description', e.target.value)} />
        </div>
      </div>
      <div className="form-actions">
        <button className="btn btn-ghost" onClick={onClose}>Cancel</button>
        <button className="btn btn-primary" disabled={submitting} onClick={submit}>
          {submitting ? 'Submitting…' : 'Submit Request'}
        </button>
      </div>
    </Modal>
  )
}

function RequestDetailModal({ request, onClose, onChanged }: { request: ReliefRequest; onClose: () => void; onChanged: () => void }) {
  const { toast, user } = useApp()
  const [updating, setUpdating] = useState(false)
  const next = requestNextStates(request.status)
  const canManage = canAssign(user?.role ?? 'ADMIN')

  async function changeStatus(status: string) {
    setUpdating(true)
    try {
      await api.reliefRequests.patch(request.requestId, { status })
      toast('success', `Request #${request.requestId} moved to ${status.replace('_', ' ')}`)
      onChanged()
    } catch (err) {
      toast('error', api.errorMessage(err))
      setUpdating(false)
    }
  }

  return (
    <Modal title={`Relief Request #${request.requestId}`} onClose={onClose}>
      <div className="detail-grid" style={{ marginBottom: 16 }}>
        <div className="detail-item"><div className="k">Victim</div><div className="v">{request.victimName}</div></div>
        <div className="detail-item"><div className="k">Contact</div><div className="v small">{request.phone} · {request.email}</div></div>
        <div className="detail-item"><div className="k">Type</div><div className="v">{request.requestType.replace('_', ' ').toLowerCase()}</div></div>
        <div className="detail-item"><div className="k">Priority</div><div className="v"><Badge text={request.priority} tone={priorityTone(request.priority)} /></div></div>
        <div className="detail-item"><div className="k">Status</div><div className="v"><Badge text={request.status} tone={statusTone(request.status)} /></div></div>
        <div className="detail-item"><div className="k">Incident</div><div className="v mono">#{request.incidentId}</div></div>
      </div>
      {request.description ? (
        <div className="detail-item" style={{ marginBottom: 16 }}>
          <div className="k">Description</div>
          <div className="soft" style={{ marginTop: 4 }}>{request.description}</div>
        </div>
      ) : null}
      <div className="detail-grid" style={{ marginBottom: 16 }}>
        <div className="detail-item"><div className="k">Assigned volunteer</div><div className="v">{request.assignedVolunteerName ?? 'None'}</div></div>
        <div className="detail-item"><div className="k">Allocated resource</div><div className="v">{request.allocatedResourceName ?? 'None'}</div></div>
        <div className="detail-item"><div className="k">Allocated shelter</div><div className="v">{request.allocatedShelterName ?? 'None'}</div></div>
        <div className="detail-item"><div className="k">Requested</div><div className="v small">{formatDate(request.requestDate)}</div></div>
        <div className="detail-item"><div className="k">Updated</div><div className="v small">{formatDate(request.updatedAt)}</div></div>
      </div>
      {request.status === 'FULFILLED' ? (
        <p className="small soft">
          Note: a request can only be marked Fulfilled once a volunteer or resource is assigned to it.
        </p>
      ) : null}
      {canManage ? (
        <div className="form-actions">
          {next.map((s) => (
            <button key={s} className="btn btn-outline btn-sm" disabled={updating} onClick={() => changeStatus(s)}>
              Mark {s.replace('_', ' ')}
            </button>
          ))}
        </div>
      ) : null}
    </Modal>
  )
}

function AssignVolunteerModal({ request, onClose, onDone }: { request: ReliefRequest; onClose: () => void; onDone: () => void }) {
  const [volunteers, setVolunteers] = useState<Volunteer[] | null>(null)
  const [error, setError] = useState<string | null>(null)
  const [selectedId, setSelectedId] = useState<number | null>(null)
  const [submitting, setSubmitting] = useState(false)
  const [loaded, setLoaded] = useState(false)

  useEffect(() => {
    api.volunteers
      .list()
      .then((p) => {
        const available = p.content.filter((v) => v.availabilityStatus === 'AVAILABLE')
        setVolunteers(available)
        if (available.length > 0) setSelectedId(available[0].volunteerId)
      })
      .catch((err) => setError(api.errorMessage(err)))
      .finally(() => setLoaded(true))
  }, [])

  async function assign() {
    if (!selectedId) return
    setSubmitting(true)
    setError(null)
    try {
      await api.volunteers.assign(selectedId, {
        incidentId: request.incidentId,
        assignedArea: request.address || 'Relief zone',
        reliefRequestId: request.requestId,
      })
      onDone()
    } catch (err) {
      setError(api.errorMessage(err))
      setSubmitting(false)
    }
  }

  return (
    <Modal title={`Assign Volunteer — Request #${request.requestId}`} onClose={onClose}>
      {error ? <div className="form-error">{error}</div> : null}
      {!loaded ? (
        <Spinner />
      ) : volunteers && volunteers.length > 0 ? (
        <>
          <p className="soft small" style={{ marginTop: 0 }}>Select an available volunteer to deploy:</p>
          <div style={{ display: 'grid', gap: 8 }}>
            {volunteers.map((v) => (
              <label key={v.volunteerId} className="list-item" style={{ border: '1px solid var(--border)', borderRadius: 10, cursor: 'pointer' }}>
                <input type="radio" name="vol" checked={selectedId === v.volunteerId} onChange={() => setSelectedId(v.volunteerId)} />
                <div className="avatar">{v.firstName[0]}{v.lastName[0]}</div>
                <div className="grow">
                  <div className="row-main">{v.firstName} {v.lastName}</div>
                  <div className="row-sub">{v.city ?? '—'} · {v.skills || 'No skills listed'}</div>
                </div>
              </label>
            ))}
          </div>
          <div className="form-actions">
            <button className="btn btn-ghost" onClick={onClose}>Cancel</button>
            <button className="btn btn-primary" disabled={submitting || !selectedId} onClick={assign}>
              {submitting ? 'Assigning…' : 'Assign Volunteer'}
            </button>
          </div>
        </>
      ) : (
        <EmptyState icon="👥" title="No available volunteers" hint="Register volunteers or mark existing ones as available." />
      )}
    </Modal>
  )
}

function AllocateResourceModal({ request, onClose, onDone }: { request: ReliefRequest; onClose: () => void; onDone: () => void }) {
  const [resources, setResources] = useState<Resource[] | null>(null)
  const [error, setError] = useState<string | null>(null)
  const [selectedId, setSelectedId] = useState<number | null>(null)
  const [quantity, setQuantity] = useState(1)
  const [submitting, setSubmitting] = useState(false)
  const [loaded, setLoaded] = useState(false)

  useEffect(() => {
    api.resources
      .all()
      .then((list) => {
        const usable = list.filter((r) => r.quantityAvailable > 0 && r.status !== 'EXPIRED')
        setResources(usable)
        if (usable.length > 0) setSelectedId(usable[0].resourceId)
      })
      .catch((err) => setError(api.errorMessage(err)))
      .finally(() => setLoaded(true))
  }, [])

  const selected = resources?.find((r) => r.resourceId === selectedId)

  async function allocate() {
    if (!selectedId) return
    setSubmitting(true)
    setError(null)
    try {
      await api.resources.allocate(selectedId, quantity, request.requestId)
      onDone()
    } catch (err) {
      setError(api.errorMessage(err))
      setSubmitting(false)
    }
  }

  return (
    <Modal title={`Allocate Resource — Request #${request.requestId}`} onClose={onClose}>
      {error ? <div className="form-error">{error}</div> : null}
      {!loaded ? (
        <Spinner />
      ) : resources && resources.length > 0 ? (
        <>
          <div className="field">
            <label>Resource</label>
            <select className="select" value={selectedId ?? ''} onChange={(e) => setSelectedId(Number(e.target.value))}>
              {resources.map((r) => (
                <option key={r.resourceId} value={r.resourceId}>
                  {r.resourceName} — {r.quantityAvailable} {r.unit} available
                </option>
              ))}
            </select>
          </div>
          <div className="field">
            <label>Quantity ({selected?.unit ?? 'units'})</label>
            <input
              className="input"
              type="number"
              min={1}
              max={selected?.quantityAvailable ?? 1}
              value={quantity}
              onChange={(e) => setQuantity(Number(e.target.value))}
            />
          </div>
          <div className="form-actions">
            <button className="btn btn-ghost" onClick={onClose}>Cancel</button>
            <button className="btn btn-primary" disabled={submitting || !selectedId} onClick={allocate}>
              {submitting ? 'Allocating…' : 'Allocate Resource'}
            </button>
          </div>
        </>
      ) : (
        <EmptyState icon="📦" title="No resources available" hint="Add resources with stock to allocate against requests." />
      )}
    </Modal>
  )
}

function AllocateShelterModal({ request, onClose, onDone }: { request: ReliefRequest; onClose: () => void; onDone: () => void }) {
  const [shelters, setShelters] = useState<Shelter[] | null>(null)
  const [error, setError] = useState<string | null>(null)
  const [selectedId, setSelectedId] = useState<number | null>(null)
  const [people, setPeople] = useState(1)
  const [submitting, setSubmitting] = useState(false)
  const [loaded, setLoaded] = useState(false)

  useEffect(() => {
    api.shelters
      .list()
      .then((list) => {
        const usable = list.filter((s) => s.hasCapacity)
        setShelters(usable)
        if (usable.length > 0) setSelectedId(usable[0].shelterId)
      })
      .catch((err) => setError(api.errorMessage(err)))
      .finally(() => setLoaded(true))
  }, [])

  const selected = shelters?.find((s) => s.shelterId === selectedId)
  const maxPeople = selected ? selected.capacity - selected.currentOccupancy : 1

  async function allocate() {
    if (!selectedId) return
    if (!people || people < 1) {
      setError('Enter how many people need shelter.')
      return
    }
    setSubmitting(true)
    setError(null)
    try {
      await api.shelters.allocate(selectedId, people, request.requestId)
      onDone()
    } catch (err) {
      setError(api.errorMessage(err))
      setSubmitting(false)
    }
  }

  return (
    <Modal title={`Allocate Shelter — Request #${request.requestId}`} onClose={onClose}>
      {error ? <div className="form-error">{error}</div> : null}
      {!loaded ? (
        <Spinner />
      ) : shelters && shelters.length > 0 ? (
        <>
          <p className="soft small" style={{ marginTop: 0 }}>Assign shelter beds to {request.victimName}:</p>
          <div className="field">
            <label>Shelter</label>
            <select className="select" value={selectedId ?? ''} onChange={(e) => setSelectedId(Number(e.target.value))}>
              {shelters.map((s) => (
                <option key={s.shelterId} value={s.shelterId}>
                  {s.name} — {s.location} ({s.currentOccupancy}/{s.capacity} occupied)
                </option>
              ))}
            </select>
          </div>
          <div className="field">
            <label>People</label>
            <input
              className="input"
              type="number"
              min={1}
              max={maxPeople}
              value={people}
              onChange={(e) => setPeople(Number(e.target.value))}
            />
            {selected && <div className="hint">{maxPeople} bed(s) available at this shelter.</div>}
          </div>
          <div className="form-actions">
            <button className="btn btn-ghost" onClick={onClose}>Cancel</button>
            <button className="btn btn-primary" disabled={submitting || !selectedId} onClick={allocate}>
              {submitting ? 'Allocating…' : 'Allocate Shelter'}
            </button>
          </div>
        </>
      ) : (
        <EmptyState icon="⛺" title="No shelters with capacity" hint="Register shelters or free up beds in the Shelters view." />
      )}
    </Modal>
  )
}
