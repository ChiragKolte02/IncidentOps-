
import { useEffect, useState } from 'react'
import { useAuth } from '../../context/AuthContext'

export default function Sidebar({ onNavigate }) {
  const { user } = useAuth()
  const [activeHash, setActiveHash] = useState(
    window.location.hash || '#overview'
  )

  useEffect(() => {
    const updateActive = () => {
      setActiveHash(window.location.hash || '#overview')
    }

    window.addEventListener('hashchange', updateActive)
    return () => window.removeEventListener('hashchange', updateActive)
  }, [])

  const isAdmin = user?.role === 'ADMIN'

  const items = [
    { label: 'Overview', href: '#overview' },
    { label: 'Open Incidents', href: '#incidents' },
    { label: 'All Incidents', href: '#all-incidents' },
    { label: 'Alerts', href: '#alerts' },
    ...(isAdmin ? [{ label: 'User Management', href: '#users' }] : []),
  ]

  const teamLabel = isAdmin
    ? 'Administration'
    : user?.team === 'UNIX'
      ? 'UNIX Operations'
      : user?.team === 'DBA'
        ? 'Database Operations'
        : 'Team Workspace'

  return (
    <aside className="flex w-64 shrink-0 flex-col border-r border-slate-800 bg-slate-950 p-4 text-white">
      <div className="mb-6 rounded-xl border border-slate-800 bg-slate-900 p-4">
        <p className="text-xs font-medium uppercase tracking-wider text-slate-500">
          Workspace
        </p>
        <p className="mt-2 font-semibold">{teamLabel}</p>
        <p className="mt-1 text-xs text-slate-400">
          {user?.team || 'NONE'} · {user?.role || 'Unknown role'}
        </p>
      </div>

      <p className="mb-3 px-3 text-xs font-semibold uppercase tracking-wider text-slate-500">
        Menu
      </p>

      <nav aria-label="Dashboard navigation" className="space-y-1">
        {items.map((item) => {
          const active = activeHash === item.href

          return (
            <a
              key={item.href}
              href={item.href}
              onClick={onNavigate}
              aria-current={active ? 'page' : undefined}
              className={`flex items-center rounded-lg px-3 py-3 text-sm font-medium transition ${
                active
                  ? 'bg-blue-600/15 text-blue-300 ring-1 ring-inset ring-blue-500/20'
                  : 'text-slate-400 hover:bg-slate-900 hover:text-white'
              }`}
            >
              <span
                className={`mr-3 h-2 w-2 rounded-full ${
                  active ? 'bg-blue-400' : 'bg-slate-700'
                }`}
              />
              {item.label}
            </a>
          )
        })}
      </nav>

      <div className="mt-auto pt-6">
        <div className="rounded-lg bg-slate-900 p-3">
          <p className="text-xs font-medium text-slate-300">IncidentOps</p>
          <p className="mt-1 text-xs text-slate-500">
            Self-hosted monitoring
          </p>
        </div>
      </div>
    </aside>
  )
}
