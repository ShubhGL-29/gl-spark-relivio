import { useEffect, useMemo, useState } from 'react'
import { useApp } from '../AppContext'
import { api } from '../api'
import { useApi, formatDate } from '../hooks'
import type {
  ReliefRequest,
  Resource,
  ResourceCategory,
  ResourcePayload,
  ResourceStatus,
} from '../types'
import {
  Badge,
  resourceTone,
  EmptyState,
  Modal,
  Spinner,
  IconPlus,
  IconBox,
} from '../components/ui'

const RES_STATUSES: ResourceStatus[] = ['AVAILABLE', 'LOW_STOCK', 'OUT_OF_STOCK', 'EXPIRED']

export function Resources() {
  const { toast, refreshTick, bump } = useApp()
  const [filter, setFilter] = useState('ALL')
  const [showAdd, setShowAdd] = useState(false)
  const [selected, setSelected] = useState<Resource | null>(null)

  const { data, loading, error, reload } = useApi<Resource[]>(() => api.resources.all(), [refreshTick])
  const lowStock = useApi<Resource[]>(() => api.resources.lowStock(), [refreshTick])

  const filtered = useMemo(() => {
    let list = data ?? []
    if (filter !== 'ALL') list = list.filter((r) => r.status === filter)
    return list
  }, [data, filter])

  return (
    <div>
      <div className="page-head">
        <div>
          <h2>Resources</h2>
          <p>Supply inventory across warehouses</p>
        </div>
        <div className="spacer" />
        {(lowStock.data ?? []).length > 0 && (
          <span className="chip tone-danger">{lowStock.data?.length} low stock alerts</span>
        )}
        <button className="btn btn-outline" onClick={reload}>Refresh</button>
        <button className="btn btn-primary" onClick={() => setShowAdd(true)}>
          <IconPlus /> Add Resource
        </button>
      </div>

      <div className="toolbar">
        <div className="seg">
          {['ALL', ...RES_STATUSES].map((s) => (
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
          <EmptyState icon="📦" title="No resources found" hint="Add supplies to track stock and allocation." />
        </div>
      ) : (
        <div className="table-wrap">
          <table className="data-table">
            <thead>
              <tr>
                <th>Resource</th>
                <th>Category</th>
                <th>Available</th>
                <th>Allocated</th>
                <th>Warehouse</th>
                <th>Expiry</th>
                <th>Status</th>
                <th></th>
              </tr>
            </thead>
            <tbody>
              {filtered.map((r) => (
                <tr key={r.resourceId}>
                  <td>
                    <div className="row-main">{r.resourceName}</div>
                    <div className="row-sub">{r.supplierName ?? '—'}</div>
                  </td>
                  <td>
                    <span className="chip">{r.category.replace('_', ' ').toLowerCase()}</span>
                  </td>
                  <td className="mono" style={{ fontWeight: 700 }}>
                    {r.quantityAvailable} {r.unit}
                  </td>
                  <td className="mono">{r.quantityAllocated} {r.unit}</td>
                  <td className="row-sub">{r.warehouseLocation}</td>
                  <td className="row-sub">{formatDate(r.expiryDate)}</td>
                  <td>
                    <Badge text={r.status} tone={resourceTone(r.status)} />
                  </td>
                  <td>
                    <div className="cell-actions">
                      {r.quantityAvailable > 0 && r.status !== 'EXPIRED' && (
                        <button className="btn btn-outline btn-sm" onClick={() => setSelected(r)}>
                          Allocate
                        </button>
                      )}
                      <button className="btn btn-ghost btn-sm" onClick={() => setSelected(r)}>
                        Manage
                      </button>
                    </div>
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      )}

      {showAdd ? (
        <AddResourceModal onClose={() => setShowAdd(false)} onDone={() => { setShowAdd(false); toast('success', 'Resource added'); bump() }} />
      ) : null}

      {selected ? (
        <ManageResourceModal resource={selected} onClose={() => setSelected(null)} onDone={() => { setSelected(null); bump() }} />
      ) : null}
    </div>
  )
}

function AddResourceModal({ onClose, onDone }: { onClose: () => void; onDone: () => void }) {
  const [form, setForm] = useState<ResourcePayload>({
    resourceName: '',
    category: 'FOOD',
    quantity: 100,
    unit: 'packets',
    warehouseLocation: '',
    expiryDate: null,
    supplierName: '',
  })
  const [submitting, setSubmitting] = useState(false)
  const [error, setError] = useState<string | null>(null)

  const set = <K extends keyof ResourcePayload>(k: K, v: ResourcePayload[K]) =>
    setForm((f) => ({ ...f, [k]: v }))

  async function submit() {
    if (!form.resourceName.trim() || !form.warehouseLocation.trim()) {
      setError('Resource name and warehouse location are required.')
      return
    }
    setSubmitting(true)
    setError(null)
    try {
      await api.resources.create({
        ...form,
        quantity: Number(form.quantity),
        expiryDate: form.expiryDate || null,
      })
      onDone()
    } catch (err) {
      setError(api.errorMessage(err))
      setSubmitting(false)
    }
  }

  return (
    <Modal title="Add a Resource" onClose={onClose} wide>
      {error ? <div className="form-error">{error}</div> : null}
      <div className="form-grid">
        <div className="field">
          <label>Resource name *</label>
          <input className="input" value={form.resourceName} onChange={(e) => set('resourceName', e.target.value)} placeholder="e.g. Rice packets" />
        </div>
        <div className="field">
          <label>Category *</label>
          <select className="select" value={form.category} onChange={(e) => set('category', e.target.value as ResourceCategory)}>
            {(['FOOD', 'WATER', 'MEDICINE', 'BLANKET', 'TENT', 'CLOTHING', 'HYGIENE_KIT', 'MEDICAL_EQUIPMENT', 'OTHER'] as const).map((c) => (
              <option key={c}>{c}</option>
            ))}
          </select>
        </div>
        <div className="field">
          <label>Quantity *</label>
          <input className="input" type="number" min={0} value={form.quantity} onChange={(e) => set('quantity', Number(e.target.value))} />
        </div>
        <div className="field">
          <label>Unit *</label>
          <input className="input" value={form.unit} onChange={(e) => set('unit', e.target.value)} placeholder="packets, litres, kits…" />
        </div>
        <div className="field" style={{ gridColumn: '1 / -1' }}>
          <label>Warehouse location *</label>
          <input className="input" value={form.warehouseLocation} onChange={(e) => set('warehouseLocation', e.target.value)} />
        </div>
        <div className="field">
          <label>Supplier</label>
          <input className="input" value={form.supplierName} onChange={(e) => set('supplierName', e.target.value)} />
        </div>
        <div className="field">
          <label>Expiry date</label>
          <input className="input" type="date" value={form.expiryDate ?? ''} onChange={(e) => set('expiryDate', e.target.value || null)} />
        </div>
      </div>
      <div className="form-actions">
        <button className="btn btn-ghost" onClick={onClose}>Cancel</button>
        <button className="btn btn-primary" disabled={submitting} onClick={submit}>
          {submitting ? 'Adding…' : 'Add Resource'}
        </button>
      </div>
    </Modal>
  )
}

function ManageResourceModal({ resource, onClose, onDone }: { resource: Resource; onClose: () => void; onDone: () => void }) {
  const { toast } = useApp()
  const [quantity, setQuantity] = useState(1)
  const [reliefRequestId, setReliefRequestId] = useState(0)
  const [requests, setRequests] = useState<ReliefRequest[]>([])
  const [restockQty, setRestockQty] = useState(10)
  const [submitting, setSubmitting] = useState(false)
  const [error, setError] = useState<string | null>(null)

  useEffect(() => {
    api.reliefRequests
      .list()
      .then((list) =>
        setRequests(
          list.filter((r) => ['PENDING', 'ASSIGNED', 'IN_PROGRESS'].includes(r.status)),
        ),
      )
      .catch(() => setRequests([]))
  }, [])

  async function allocate() {
    if (quantity <= 0 || quantity > resource.quantityAvailable) {
      setError(`Quantity must be between 1 and ${resource.quantityAvailable}.`)
      return
    }
    setSubmitting(true)
    setError(null)
    try {
      await api.resources.allocate(resource.resourceId, quantity, reliefRequestId || null)
      toast('success', `Allocated ${quantity} ${resource.unit} of ${resource.resourceName}`)
      onDone()
    } catch (err) {
      setError(api.errorMessage(err))
      setSubmitting(false)
    }
  }

  async function restock() {
    if (restockQty <= 0) {
      setError('Restock quantity must be positive.')
      return
    }
    setSubmitting(true)
    setError(null)
    try {
      await api.resources.restock(resource.resourceId, restockQty)
      toast('success', `Restocked ${restockQty} ${resource.unit}`)
      onDone()
    } catch (err) {
      setError(api.errorMessage(err))
      setSubmitting(false)
    }
  }

  return (
    <Modal title={`${resource.resourceName}`} onClose={onClose}>
      {error ? <div className="form-error">{error}</div> : null}
      <div className="detail-grid" style={{ marginBottom: 18 }}>
        <div className="detail-item"><div className="k">Available</div><div className="v mono">{resource.quantityAvailable} {resource.unit}</div></div>
        <div className="detail-item"><div className="k">Allocated</div><div className="v mono">{resource.quantityAllocated} {resource.unit}</div></div>
        <div className="detail-item"><div className="k">Status</div><div className="v"><Badge text={resource.status} tone={resourceTone(resource.status)} /></div></div>
        <div className="detail-item"><div className="k">Warehouse</div><div className="v small">{resource.warehouseLocation}</div></div>
      </div>

      <div className="field">
        <label>Allocate quantity ({resource.unit})</label>
        <input className="input" type="number" min={1} max={resource.quantityAvailable} value={quantity} onChange={(e) => setQuantity(Number(e.target.value))} />
      </div>
      <div className="field">
        <label>Link to relief request (optional)</label>
        <select className="select" value={reliefRequestId} onChange={(e) => setReliefRequestId(Number(e.target.value))}>
          <option value={0}>— None —</option>
          {requests.map((r) => (
            <option key={r.requestId} value={r.requestId}>
              #{r.requestId} — {r.requestType.toLowerCase()} ({r.victimName})
            </option>
          ))}
        </select>
      </div>
      <button className="btn btn-primary btn-block" disabled={submitting} onClick={allocate}>
        <IconBox size={15} /> Allocate
      </button>

      <hr style={{ border: 'none', borderTop: '1px solid var(--border)', margin: '18px 0' }} />

      <div className="field">
        <label>Restock quantity ({resource.unit})</label>
        <input className="input" type="number" min={1} value={restockQty} onChange={(e) => setRestockQty(Number(e.target.value))} />
      </div>
      <button className="btn btn-outline btn-block" disabled={submitting} onClick={restock}>
        Restock
      </button>
    </Modal>
  )
}
