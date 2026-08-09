import { useCallback, useEffect, useRef, useState } from 'react'
import { api } from './api'

export function useApi<T>(
  fn: () => Promise<T>,
  deps: readonly unknown[] = [],
  initial?: T,
): { data: T | undefined; error: string | null; loading: boolean; reload: () => void } {
  const [data, setData] = useState<T | undefined>(initial)
  const [error, setError] = useState<string | null>(null)
  const [loading, setLoading] = useState(true)
  const [tick, setTick] = useState(0)
  const fnRef = useRef(fn)
  fnRef.current = fn

  useEffect(() => {
    let cancelled = false
    setLoading(true)
    setError(null)
    fnRef
      .current()
      .then((res) => {
        if (!cancelled) setData(res)
      })
      .catch((err) => {
        if (!cancelled) setError(api.errorMessage(err))
      })
      .finally(() => {
        if (!cancelled) setLoading(false)
      })
    return () => {
      cancelled = true
    }
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [...deps, tick])

  const reload = useCallback(() => setTick((t) => t + 1), [])

  return { data, error, loading, reload }
}

export function formatDate(value?: string | null): string {
  if (!value) return '—'
  const d = new Date(value)
  if (Number.isNaN(d.getTime())) return '—'
  return d.toLocaleString(undefined, {
    month: 'short',
    day: 'numeric',
    hour: '2-digit',
    minute: '2-digit',
  })
}
