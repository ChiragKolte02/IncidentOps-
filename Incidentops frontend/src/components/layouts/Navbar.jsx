
import { useState } from 'react'
import { useAuth } from '../../context/AuthContext'

export default function Navbar() {
  const { user, logout } = useAuth()
  const [loggingOut, setLoggingOut] = useState(false)

  async function handleLogout() {
    setLoggingOut(true)

    try {
      await logout()
    } catch {
      // The auth context clears local user state even if the
      // logout request fails. The server-side token may still exist.
    } finally {
      setLoggingOut(false)
    }
  }

  const displayName = user?.username || 'User'

  return (
    <header className="sticky top-0 z-30 flex h-16 items-center justify-between border-b border-slate-800 bg-slate-950 px-4 text-white sm:px-6">
      <div className="flex items-center gap-3">
        <div className="flex h-10 w-10 items-center justify-center rounded-xl bg-blue-600 font-bold">
          IO
        </div>

        <div>
          <h1 className="text-lg font-bold tracking-tight">
            IncidentOps
          </h1>
          <p className="hidden text-xs text-slate-400 sm:block">
            Incident Management Platform
          </p>
        </div>
      </div>

      <div className="flex items-center gap-3 sm:gap-5">
        <div className="hidden text-right sm:block">
          <p className="text-sm font-medium">{displayName}</p>
          <p className="text-xs text-slate-400">
            {user?.role}
            {user?.team && user.team !== 'NONE' ? ` · ${user.team}` : ''}
          </p>
        </div>

        <div
          className="flex h-9 w-9 items-center justify-center rounded-full bg-slate-800 font-semibold text-blue-300"
          title={displayName}
        >
          {displayName.charAt(0).toUpperCase()}
        </div>

        <button
          type="button"
          onClick={handleLogout}
          disabled={loggingOut}
          className="rounded-lg border border-slate-700 px-3 py-2 text-sm font-medium text-slate-200 transition hover:border-red-500/50 hover:bg-red-500/10 hover:text-red-300 disabled:opacity-50"
        >
          {loggingOut ? 'Signing out...' : 'Logout'}
        </button>
      </div>
    </header>
  )
}
