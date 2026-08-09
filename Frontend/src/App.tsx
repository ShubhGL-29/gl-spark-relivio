import { useEffect, useState } from 'react'
import { AppProvider, useApp } from './AppContext'
import { api } from './api'
import { useApi } from './hooks'
import { canAccess } from './auth'
import type { Route } from './types'
import { ToastStack, IconGrid, IconAlert, IconHandHeart, IconUsers, IconBox, IconTent, IconBell, IconShield } from './components/ui'
import { Login } from './views/Login'
import { Dashboard } from './views/Dashboard'
import { Incidents } from './views/Incidents'
import { IncidentDetail } from './views/IncidentDetail'
import { ReliefRequests } from './views/ReliefRequests'
import { Volunteers } from './views/Volunteers'
import { Resources } from './views/Resources'
import { Shelters } from './views/Shelters'
import { Notifications } from './views/Notifications'
import { ROLE_LABELS } from './auth'

interface NavItem {
  route: Route
  label: string
  icon: React.ReactNode
}

const NAV: Array<{ section: string; items: NavItem[] }> = [
  {
    section: 'Overview',
    items: [{ route: 'dashboard', label: 'Dashboard', icon: <IconGrid /> }],
  },
  {
    section: 'Operations',
    items: [
      { route: 'incidents', label: 'Incidents', icon: <IconAlert /> },
      { route: 'relief', label: 'Relief Requests', icon: <IconHandHeart /> },
      { route: 'volunteers', label: 'Volunteers', icon: <IconUsers /> },
      { route: 'resources', label: 'Resources', icon: <IconBox /> },
      { route: 'shelters', label: 'Shelters', icon: <IconTent /> },
    ],
  },
  {
    section: 'Alerts',
    items: [{ route: 'notifications', label: 'Notifications', icon: <IconBell /> }],
  },
]

const TITLES: Record<Route, { title: string; sub: string }> = {
  dashboard: { title: 'Dashboard', sub: 'Real-time overview of relief operations' },
  incidents: { title: 'Incidents', sub: 'Reported disaster events' },
  relief: { title: 'Relief Requests', sub: 'Citizen needs and assignments' },
  volunteers: { title: 'Volunteers', sub: 'Response team management' },
  resources: { title: 'Resources', sub: 'Supply inventory and allocation' },
  shelters: { title: 'Shelters', sub: 'Capacity and occupancy tracking' },
  notifications: { title: 'Notifications', sub: 'Status alerts for any user' },
}

function Shell() {
  const { route, incidentId, user } = useApp()
  const role = user?.role ?? 'ADMIN'

  if (!canAccess(role, route)) {
    return <Denied />
  }

  return (
    <div className="app">
      <Sidebar />
      <main className="app-main">
        <div className="topbar">
          <div>
            <div className="topbar-title">{TITLES[route].title}</div>
            <div className="topbar-sub">{TITLES[route].sub}</div>
          </div>
          <div className="topbar-spacer" />
          <div className="topbar-pill">
            <span className="dot" />
            API Gateway online
          </div>
          <UserMenu />
        </div>
        <div className="content">
          {route === 'dashboard' && <Dashboard />}
          {route === 'incidents' && (incidentId != null ? <IncidentDetail incidentId={incidentId} /> : <Incidents />)}
          {route === 'relief' && <ReliefRequests />}
          {route === 'volunteers' && <Volunteers />}
          {route === 'resources' && <Resources />}
          {route === 'shelters' && <Shelters />}
          {route === 'notifications' && <Notifications />}
        </div>
      </main>
      <ToastStack />
    </div>
  )
}

function Denied() {
  const { navigate } = useApp()
  return (
    <div className="app-main" style={{ minHeight: '100vh' }}>
      <div className="content" style={{ maxWidth: 560, margin: '0 auto', paddingTop: 80 }}>
        <div className="card card-pad" style={{ textAlign: 'center' }}>
          <div className="avatar" style={{ width: 48, height: 48, margin: '0 auto 12px', background: 'var(--warning-soft)', color: 'var(--warning)' }}>
            <IconShield />
          </div>
          <h3>Access denied</h3>
          <p className="soft">Your role does not have access to this page.</p>
          <button className="btn btn-primary" onClick={() => navigate('dashboard')}>
            Go to Dashboard
          </button>
        </div>
      </div>
    </div>
  )
}

function Sidebar() {
  const { route, navigate, userId, user, refreshTick } = useApp()
  const unread = useApi<number>(() => api.notifications.unreadCount(userId), [userId, refreshTick])
  const unreadCount = unread.data ?? 0
  const role = user?.role ?? 'ADMIN'

  return (
    <aside className="sidebar">
      <div className="sidebar-brand">
        <div className="brand-mark">
          <svg width="22" height="22" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
            <path d="M12 22s8-4 8-10V5l-8-3-8 3v7c0 6 8 10 8 10" />
            <path d="m9 12 2 2 4-4" />
          </svg>
        </div>
        <div>
          <div className="brand-name">Relivio</div>
          <div className="brand-tagline">Disaster Relief</div>
        </div>
      </div>

      <nav className="sidebar-nav">
        {NAV.map((group) => {
          const items = group.items.filter((item) => canAccess(role, item.route))
          if (items.length === 0) return null
          return (
            <div key={group.section}>
              <div className="nav-section">{group.section}</div>
              {items.map((item) => (
                <button
                  key={item.route}
                  className={`nav-item ${route === item.route ? 'active' : ''}`}
                  onClick={() => navigate(item.route)}
                >
                  {item.icon}
                  <span>{item.label}</span>
                  {item.route === 'notifications' && unreadCount > 0 ? (
                    <span className="nav-badge">{unreadCount}</span>
                  ) : null}
                </button>
              ))}
            </div>
          )
        })}
      </nav>

      <div className="sidebar-footer">
        Relivio · Release 1.0
        <br />
        Microservice-based relief coordination
      </div>
    </aside>
  )
}

function UserMenu() {
  const { user, logout, navigate } = useApp()
  const [open, setOpen] = useState(false)

  useEffect(() => {
    const onDoc = () => setOpen(false)
    window.addEventListener('click', onDoc)
    return () => window.removeEventListener('click', onDoc)
  }, [])

  return (
    <div style={{ position: 'relative' }}>
      <button
        className="btn btn-outline"
        onClick={(e) => {
          e.stopPropagation()
          setOpen((o) => !o)
        }}
      >
        <span className="avatar" style={{ width: 24, height: 24, fontSize: 11 }}>
          {user?.name?.[0]?.toUpperCase() ?? 'U'}
        </span>
        {user?.name ?? 'Signed out'}
      </button>
      {open && (
        <div
          className="card"
          style={{ position: 'absolute', right: 0, top: 'calc(100% + 8px)', width: 240, padding: 8, zIndex: 50 }}
          onClick={(e) => e.stopPropagation()}
        >
          <div style={{ padding: '8px 10px' }}>
            <div className="row-main">{user?.name}</div>
            <div className="row-sub">
              {user?.role ? ROLE_LABELS[user.role] : ''} · #{user?.id}
            </div>
          </div>
          <div style={{ borderTop: '1px solid var(--border)', margin: '4px 0', paddingTop: 6 }}>
            <button
              className="nav-item"
              style={{ color: 'var(--text)', width: '100%', padding: '8px 10px' }}
              onClick={() => {
                setOpen(false)
                navigate('notifications')
              }}
            >
              <span style={{ fontSize: 13 }}>Notifications</span>
            </button>
            <button
              className="nav-item"
              style={{ color: 'var(--danger)', width: '100%', padding: '8px 10px' }}
              onClick={() => {
                setOpen(false)
                logout()
              }}
            >
              <span style={{ fontSize: 13 }}>Sign out</span>
            </button>
          </div>
        </div>
      )}
    </div>
  )
}

function App() {
  return (
    <AppProvider>
      <Gate />
    </AppProvider>
  )
}

function Gate() {
  const { user } = useApp()
  if (!user) return <Login />
  return <Shell />
}

export default App
