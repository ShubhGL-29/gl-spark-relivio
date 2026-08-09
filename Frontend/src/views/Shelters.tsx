import { useState } from 'react'
import { useApp } from '../AppContext'
import { api } from '../api'
import { useApi } from '../hooks'
import { canAssign } from '../auth'
import type { Shelter, ShelterPayload } from '../types'
import {
  Badge,
  EmptyState,
  Modal,
  ProgressBar,
  Spinner,
  IconPlus,
  IconTent,
} from '../components/ui'

export function Shelters() {
  const { toast, refreshTick, bump, user } = useApp()
  const [showAdd, setShowAdd] = useState(false)
  const [selected, setSelected] = useState<Shelter | null>(null)
  const canManage = canAssign(user?.role ?? 'ADMIN')

  const { data, loading, error, reload } = useApi<Shelter[]>(() => api.shelters.list(), [refreshTick])

  const totalCapacity = (data ?? []).reduce((s, x) => s + x.capacity, 0)
  const totalOccupancy = (data ?? []).reduce((s, x) => s + x.currentOccupancy, 0)
  const full = (data ?? []).filter((s) => !s.hasCapacity).length

  return (
    <div>
      <div className="page-head">
        <div>
          <h2>Shelters</h2>
          <p>Shelter locations and live occupancy</p>
        </div>
        <div className="spacer" />
        <span className="chip tone-info">{totalCapacity - totalOccupancy} beds free</span>
        {full > 0 && <span className="chip tone-danger">{full} at full capacity</span>}
        <button className="btn btn-outline" onClick={reload}>Refresh</button>
        {canManage && (
          <button className="btn btn-primary" onClick={() => setShowAdd(true)}>
            <IconPlus /> Register Shelter
          </button>
        )}
      </div>

      {error ? (
        <div className="card card-pad">
          <div className="form-error" style={{ margin: 0 }}>{error}</div>
        </div>
      ) : null}

      {loading ? (
        <Spinner />
      ) : (data ?? []).length === 0 ? (
        <div className="card">
          <EmptyState icon="⛺" title="No shelters registered" hint="Register shelters to track capacity during relief operations." />
        </div>
      ) : (
        <div className="grid grid-3">
          {(data ?? []).map((s) => {
            const pct = s.capacity ? Math.round((s.currentOccupancy / s.capacity) * 100) : 0
            return (
              <div className="card card-pad" key={s.shelterId} style={{ display: 'flex', flexDirection: 'column', gap: 12 }}>
                <div style={{ display: 'flex', alignItems: 'flex-start', gap: 12 }}>
                  <div className="avatar" style={{ background: 'var(--info-soft)', color: 'var(--info)' }}>
                    <IconTent size={18} />
                  </div>
                  <div className="grow" style={{ flex: 1 }}>
                    <div className="row-main">{s.name}</div>
                    <div className="row-sub">{s.city ?? s.location}</div>
                    <div className="row-sub">Contact: {s.contactNumber}</div>
                  </div>
                  {s.hasCapacity ? (
                    <Badge text="open" tone="tone-success" />
                  ) : (
                    <Badge text="full" tone="tone-danger" />
                  )}
                </div>
                <div>
                  <div style={{ display: 'flex', justifyContent: 'space-between', marginBottom: 6 }}>
                    <span className="small soft">Occupancy</span>
                    <span className="small mono">{s.currentOccupancy}/{s.capacity}</span>
                  </div>
                  <ProgressBar value={s.currentOccupancy} max={s.capacity} />
                </div>
                <div className="row-sub" style={{ fontSize: 12 }}>
                  {pct >= 100 ? 'Shelter is at full capacity.' : `${100 - pct}% capacity available`}
                </div>
                {canManage && (
                  <button className="btn btn-outline btn-sm btn-block" onClick={() => setSelected(s)}>
                    Manage Shelter
                  </button>
                )}
              </div>
            )
          })}
        </div>
      )}

      {showAdd ? (
        <AddShelterModal onClose={() => setShowAdd(false)} onDone={() => { setShowAdd(false); toast('success', 'Shelter registered'); bump() }} />
      ) : null}

      {selected ? (
        <ManageShelterModal shelter={selected} onClose={() => setSelected(null)} onDone={() => { setSelected(null); bump() }} />
      ) : null}
    </div>
  )
}

function AddShelterModal({ onClose, onDone }: { onClose: () => void; onDone: () => void }) {
  const [form, setForm] = useState<ShelterPayload>({
    name: '',
    location: '',
    city: '',
    state: '',
    capacity: 50,
    currentOccupancy: 0,
    amenities: '',
    contactNumber: '',
  })
  const [submitting, setSubmitting] = useState(false)
  const [error, setError] = useState<string | null>(null)

  const set = <K extends keyof ShelterPayload>(k: K, v: ShelterPayload[K]) =>
    setForm((f) => ({ ...f, [k]: v }))

  async function submit() {
    if (!form.name.trim() || !form.location.trim() || !form.contactNumber.trim()) {
      setError('Shelter name, location and contact number are required.')
      return
    }
    setSubmitting(true)
    setError(null)
    try {
      await api.shelters.create(form)
      onDone()
    } catch (err) {
      setError(api.errorMessage(err))
      setSubmitting(false)
    }
  }

  return (
    <Modal title="Register a Shelter" onClose={onClose} wide>
      {error ? <div className="form-error">{error}</div> : null}
      <div className="form-grid">
        <div className="field">
          <label>Shelter name *</label>
          <input className="input" value={form.name} onChange={(e) => set('name', e.target.value)} />
        </div>
        <div className="field">
          <label>Location *</label>
          <input className="input" value={form.location} onChange={(e) => set('location', e.target.value)} placeholder="Area / landmark" />
        </div>
        <div className="field">
          <label>City</label>
          <input className="input" value={form.city} onChange={(e) => set('city', e.target.value)} />
        </div>
        <div className="field">
          <label>State</label>
          <input className="input" value={form.state} onChange={(e) => set('state', e.target.value)} />
        </div>
        <div className="field">
          <label>Capacity *</label>
          <input className="input" type="number" min={1} value={form.capacity} onChange={(e) => set('capacity', Number(e.target.value))} />
        </div>
        <div className="field">
          <label>Current occupancy</label>
          <input className="input" type="number" min={0} value={form.currentOccupancy} onChange={(e) => set('currentOccupancy', Number(e.target.value))} />
        </div>
        <div className="field">
          <label>Contact number *</label>
          <input className="input" value={form.contactNumber} onChange={(e) => set('contactNumber', e.target.value)} />
        </div>
        <div className="field">
          <label>Amenities</label>
          <input className="input" value={form.amenities} onChange={(e) => set('amenities', e.target.value)} placeholder="e.g. water, toilets, power, kitchen" />
        </div>
      </div>
      <div className="form-actions">
        <button className="btn btn-ghost" onClick={onClose}>Cancel</button>
        <button className="btn btn-primary" disabled={submitting} onClick={submit}>
          {submitting ? 'Registering…' : 'Register Shelter'}
        </button>
      </div>
    </Modal>
  )
}

function ManageShelterModal({ shelter, onClose, onDone }: { shelter: Shelter; onClose: () => void; onDone: () => void }) {
  const { toast } = useApp()
  const [people, setPeople] = useState(1)
  const [occupancy, setOccupancy] = useState(shelter.currentOccupancy)
  const [submitting, setSubmitting] = useState(false)
  const [error, setError] = useState<string | null>(null)

  async function allocate() {
    setSubmitting(true)
    setError(null)
    try {
      await api.shelters.allocate(shelter.shelterId, people)
      toast('success', `${people} person(s) allocated to ${shelter.name}`)
      onDone()
    } catch (err) {
      setError(api.errorMessage(err))
      setSubmitting(false)
    }
  }

  async function applyOccupancy() {
    setSubmitting(true)
    setError(null)
    try {
      await api.shelters.updateOccupancy(shelter.shelterId, occupancy)
      toast('success', 'Occupancy updated')
      onDone()
    } catch (err) {
      setError(api.errorMessage(err))
      setSubmitting(false)
    }
  }

  return (
    <Modal title={shelter.name} onClose={onClose}>
      {error ? <div className="form-error">{error}</div> : null}
      <div className="detail-grid" style={{ marginBottom: 18 }}>
        <div className="detail-item"><div className="k">Location</div><div className="v small">{shelter.location}</div></div>
        <div className="detail-item"><div className="k">Occupancy</div><div className="v mono">{shelter.currentOccupancy}/{shelter.capacity}</div></div>
        <div className="detail-item"><div className="k">Contact</div><div className="v small">{shelter.contactNumber}</div></div>
        <div className="detail-item"><div className="k">Amenities</div><div className="v small">{shelter.amenities || '—'}</div></div>
      </div>

      {!shelter.hasCapacity ? (
        <div className="form-error" style={{ marginBottom: 16 }}>
          This shelter is at full capacity and is excluded from nearby matching until space frees up.
        </div>
      ) : null}

      <div className="field">
        <label>Allocate people</label>
        <input className="input" type="number" min={1} max={shelter.capacity - shelter.currentOccupancy} value={people} onChange={(e) => setPeople(Number(e.target.value))} />
      </div>
      <button className="btn btn-primary btn-block" disabled={submitting || !shelter.hasCapacity} onClick={allocate}>
        Allocate Shelter Beds
      </button>

      <hr style={{ border: 'none', borderTop: '1px solid var(--border)', margin: '18px 0' }} />

      <div className="field">
        <label>Set occupancy manually</label>
        <input className="input" type="number" min={0} max={shelter.capacity} value={occupancy} onChange={(e) => setOccupancy(Number(e.target.value))} />
      </div>
      <button className="btn btn-outline btn-block" disabled={submitting} onClick={applyOccupancy}>
        Update Occupancy
      </button>
    </Modal>
  )
}
