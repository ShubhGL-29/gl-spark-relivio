import { useState } from 'react'
import { useApp } from '../AppContext'
import { api } from '../api'
import { SEEDED_USERS, registerCitizen, registerVolunteer, loginCitizen } from '../auth'

type Portal = 'staff' | 'citizen'

export function Login() {
  const { login, toast } = useApp()
  const [portal, setPortal] = useState<Portal>('staff')
  const [role, setRole] = useState<'admin' | 'volunteer'>('admin')
  const [mode, setMode] = useState<'login' | 'register'>('login')
  const [phone, setPhone] = useState('')
  const [password, setPassword] = useState('')
  const [showPassword, setShowPassword] = useState(false)
  const [name, setName] = useState('')
  const [email, setEmail] = useState('')
  const [error, setError] = useState<string | null>(null)
  const [submitting, setSubmitting] = useState(false)

  function switchPortal(next: Portal) {
    setPortal(next)
    setError(null)
    setSubmitting(false)
    setMode('login')
    if (next === 'staff') {
      const seeded = SEEDED_USERS[role]
      setPhone(seeded?.phone ?? '')
      setPassword('')
    } else {
      setPhone('')
      setPassword('')
    }
  }

  function switchRole(next: 'admin' | 'volunteer') {
    setRole(next)
    setPassword('')
    setPhone(SEEDED_USERS[next]?.phone ?? '')
    setError(null)
  }

  function switchMode(next: 'login' | 'register') {
    setMode(next)
    setError(null)
    setSubmitting(false)
    if (next === 'register') {
      setName('')
      setEmail('')
      setPhone('')
      setPassword('')
    } else if (portal === 'staff') {
      setPassword('')
      setPhone(SEEDED_USERS[role]?.phone ?? '')
    }
  }

  async function signIn() {
    if (!phone.trim() || !password) {
      setError('Please enter your phone number and password.')
      return
    }
    setSubmitting(true)
    setError(null)
    try {
      const user = await loginCitizen(phone.trim(), password)
      login(user)
      toast('success', `Welcome back, ${user.name}`)
    } catch (err) {
      setError(api.errorMessage(err))
      setSubmitting(false)
    }
  }

  async function register() {
    if (!name.trim() || !email.trim() || !phone.trim() || !password) {
      setError('Name, email, phone and password are required to register.')
      return
    }
    if (!/^[0-9]{10}$/.test(phone.trim())) {
      setError('Phone number must be exactly 10 digits.')
      return
    }
    if (password.length < 4) {
      setError('Password must be at least 4 characters.')
      return
    }
    setSubmitting(true)
    setError(null)
    try {
      const user =
        portal === 'staff'
          ? await registerVolunteer(name.trim(), email.trim(), phone.trim(), password)
          : await registerCitizen(name.trim(), email.trim(), phone.trim(), password)
      login(user)
      toast('success', `Welcome to Relivio, ${user.name}!`)
    } catch (err) {
      setError(api.errorMessage(err))
      setSubmitting(false)
    }
  }

  function handleSubmit(e: React.FormEvent) {
    e.preventDefault()
    if (mode === 'login') signIn()
    else register()
  }

  const seeded = SEEDED_USERS[role]

  return (
    <div className="login-wrap">
      <div className="login-shell">
        <aside className="login-side">
          <div className="login-side-inner">
            <div className="login-brand">
              <div className="brand-mark">
                <svg width="28" height="28" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
                  <path d="M12 22s8-4 8-10V5l-8-3-8 3v7c0 6 8 10 8 10" />
                  <path d="m9 12 2 2 4-4" />
                </svg>
              </div>
              <div>
                <div className="brand-name">Relivio</div>
                <div className="brand-tagline">Disaster Relief Management Platform</div>
              </div>
            </div>

            <div className="login-side-copy">
              <h1>One platform for every rescue.</h1>
              <p>
                Report incidents, request relief, coordinate volunteers and track resources —
                all in real time from a single command center.
              </p>
            </div>

            <ul className="login-features">
              <li>
                <span className="feat-icon">
                  <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
                    <path d="M13 2 3 14h9l-1 8 10-12h-9l1-8z" />
                  </svg>
                </span>
                Instant incident reporting
              </li>
              <li>
                <span className="feat-icon">
                  <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
                    <path d="M16 21v-2a4 4 0 0 0-4-4H6a4 4 0 0 0-4 4v2" />
                    <circle cx="9" cy="7" r="4" />
                    <path d="M22 21v-2a4 4 0 0 0-3-3.87" />
                    <path d="M16 3.13a4 4 0 0 1 0 7.75" />
                  </svg>
                </span>
                Volunteer coordination
              </li>
              <li>
                <span className="feat-icon">
                  <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
                    <path d="M3 3v18h18" />
                    <path d="M7 15v-3" />
                    <path d="M12 15V9" />
                    <path d="M17 15V6" />
                  </svg>
                </span>
                Live resource & shelter tracking
              </li>
            </ul>

            <div className="login-side-foot">
              <span className="chip"><span className="dot" /> All systems operational</span>
            </div>
          </div>
        </aside>

        <main className="login-main">
          <div className="login-card">
            <div className="portal-switch" role="tablist">
              <button
                className={portal === 'staff' ? 'active' : ''}
                onClick={() => switchPortal('staff')}
                role="tab"
              >
                <svg width="15" height="15" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
                  <path d="M17 21v-2a4 4 0 0 0-4-4H5a4 4 0 0 0-4 4v2" />
                  <circle cx="9" cy="7" r="4" />
                  <path d="M23 21v-2a4 4 0 0 0-3-3.87" />
                  <path d="M16 3.13a4 4 0 0 1 0 7.75" />
                </svg>
                Staff
              </button>
              <button
                className={portal === 'citizen' ? 'active' : ''}
                onClick={() => switchPortal('citizen')}
                role="tab"
              >
                <svg width="15" height="15" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
                  <circle cx="12" cy="8" r="5" />
                  <path d="M20 21a8 8 0 0 0-16 0" />
                </svg>
                Citizen
              </button>
            </div>

            <div className="login-card-head">
              <h2>
                {portal === 'staff'
                  ? mode === 'login'
                    ? 'Staff sign in'
                    : 'Create your account'
                  : mode === 'login'
                    ? 'Citizen sign in'
                    : 'Create your account'}
              </h2>
              <p>
                {portal === 'staff' && mode === 'login'
                  ? 'Sign in as an administrator or a field volunteer.'
                  : portal === 'staff' && mode === 'register'
                    ? 'Join as a volunteer to help with relief efforts.'
                    : portal === 'citizen' && mode === 'login'
                      ? 'Use your registered phone number and password.'
                      : 'Register with your details to start reporting and requesting relief.'}
              </p>
            </div>

            <form className="login-form" onSubmit={handleSubmit}>
              {portal === 'staff' && mode === 'login' ? (
                <div className="field">
                  <label htmlFor="login-role">Sign in as</label>
                  <div className="role-picker">
                    <button
                      type="button"
                      className={role === 'admin' ? 'active' : ''}
                      onClick={() => switchRole('admin')}
                    >
                      <span className="role-dot admin" />
                      Administrator
                    </button>
                    <button
                      type="button"
                      className={role === 'volunteer' ? 'active' : ''}
                      onClick={() => switchRole('volunteer')}
                    >
                      <span className="role-dot volunteer" />
                      Volunteer
                    </button>
                  </div>
                </div>
              ) : null}

              {mode === 'register' ? (
                <>
                  <div className="field">
                    <label htmlFor="login-name">Full name</label>
                    <input
                      id="login-name"
                      className="input"
                      value={name}
                      onChange={(e) => setName(e.target.value)}
                      placeholder="e.g. Priya Sharma"
                      autoComplete="name"
                    />
                  </div>
                  <div className="field">
                    <label htmlFor="login-email">Email</label>
                    <input
                      id="login-email"
                      className="input"
                      type="email"
                      value={email}
                      onChange={(e) => setEmail(e.target.value)}
                      placeholder="you@example.com"
                      autoComplete="email"
                    />
                  </div>
                </>
              ) : null}

              <div className="field">
                <label htmlFor="login-phone">
                  Phone number{portal === 'staff' && mode === 'login' ? ' (staff account)' : ''}
                </label>
                <div className="input-with-icon">
                  <span className="input-icon">
                    <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
                      <rect x="5" y="2" width="14" height="20" rx="2" ry="2" />
                      <path d="M12 18h.01" />
                    </svg>
                  </span>
                  <input
                    id="login-phone"
                    className="input"
                    value={phone}
                    onChange={(e) => setPhone(e.target.value.replace(/[^0-9]/g, ''))}
                    maxLength={10}
                    placeholder="10-digit number"
                    autoComplete="tel"
                    inputMode="numeric"
                  />
                </div>
              </div>

              <div className="field">
                <label htmlFor="login-password">Password</label>
                <div className="input-with-icon">
                  <span className="input-icon">
                    <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
                      <rect x="3" y="11" width="18" height="11" rx="2" ry="2" />
                      <path d="M7 11V7a5 5 0 0 1 10 0v4" />
                    </svg>
                  </span>
                  <input
                    id="login-password"
                    className="input input-password"
                    type={showPassword ? 'text' : 'password'}
                    value={password}
                    onChange={(e) => setPassword(e.target.value)}
                    placeholder={mode === 'register' ? 'At least 4 characters' : 'Your password'}
                    autoComplete={mode === 'register' ? 'new-password' : 'current-password'}
                  />
                  <button
                    type="button"
                    className="password-toggle"
                    onClick={() => setShowPassword((s) => !s)}
                    aria-label={showPassword ? 'Hide password' : 'Show password'}
                  >
                    {showPassword ? (
                      <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
                        <path d="M17.94 17.94A10.07 10.07 0 0 1 12 20c-7 0-11-8-11-8a18.45 18.45 0 0 1 5.06-5.94" />
                        <path d="M9.9 4.24A9.12 9.12 0 0 1 12 4c7 0 11 8 11 8a18.5 18.5 0 0 1-2.16 3.19" />
                        <path d="M14.12 14.12a3 3 0 1 1-4.24-4.24" />
                        <line x1="1" y1="1" x2="23" y2="23" />
                      </svg>
                    ) : (
                      <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
                        <path d="M1 12s4-8 11-8 11 8 11 8-4 8-11 8-11-8-11-8z" />
                        <circle cx="12" cy="12" r="3" />
                      </svg>
                    )}
                  </button>
                </div>
              </div>

              {error ? (
                <div className="form-error" role="alert">
                  <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
                    <circle cx="12" cy="12" r="10" />
                    <line x1="12" y1="8" x2="12" y2="12" />
                    <line x1="12" y1="16" x2="12.01" y2="16" />
                  </svg>
                  {error}
                </div>
              ) : null}

              <button className="btn btn-primary btn-block" type="submit" disabled={submitting}>
                {submitting
                  ? mode === 'login'
                    ? 'Signing in…'
                    : 'Creating account…'
                  : mode === 'login'
                    ? portal === 'staff'
                      ? `Sign in as ${seeded?.name ?? role}`
                      : 'Sign in'
                    : portal === 'staff'
                      ? 'Register as volunteer'
                      : 'Create account'}
              </button>
            </form>

            {portal === 'staff' ? (
              <div className="login-switch">
                {mode === 'login' ? (
                  <>
                    <span>Want to volunteer?</span>
                    <button className="link-btn" onClick={() => switchMode('register')}>
                      Register as a volunteer
                    </button>
                  </>
                ) : (
                  <>
                    <span>Already a volunteer?</span>
                    <button className="link-btn" onClick={() => switchMode('login')}>
                      Sign in
                    </button>
                  </>
                )}
              </div>
            ) : (
              <div className="login-switch">
                {mode === 'login' ? (
                  <>
                    <span>New to Relivio?</span>
                    <button className="link-btn" onClick={() => switchMode('register')}>
                      Create an account
                    </button>
                  </>
                ) : (
                  <>
                    <span>Already have an account?</span>
                    <button className="link-btn" onClick={() => switchMode('login')}>
                      Sign in
                    </button>
                  </>
                )}
              </div>
            )}
          </div>
        </main>
      </div>
    </div>
  )
}
