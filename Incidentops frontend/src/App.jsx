
import { useAuth } from './context/AuthContext'

import Login from './pages/Login'
import AdminDashboard from './pages/AdminDashboard'
import UnixDashboard from './pages/UnixDashboard'
import DbaDashboard from './pages/DbaDashboard'

import DashboardLayout from './components/layouts/DashboardLayout'

export default function App() {
  const { user, loading, isAuthenticated } = useAuth()  

  // 1. Restore/check the current login session
  if (loading) {
    return (
      <div className="flex min-h-screen items-center justify-center bg-slate-950 text-white">
        <div className="text-center">
          <div className="mx-auto mb-4 h-10 w-10 animate-spin rounded-full border-4 border-slate-700 border-t-blue-500" />
          <p className="text-sm text-slate-400">
            Checking your session...
          </p>
        </div>
      </div>
    )
  }

  // 2. Show login if the user is not authenticated
  if (!isAuthenticated || !user) {
    return <Login />
  }

  // 3. Select the dashboard using the logged-in user's role and team
  let title
  let description
  let dashboard

  if (user.role === 'ADMIN' && user.team === 'NONE') {
    title = 'Admin Dashboard'
    description =
      'Manage users and monitor incidents, open incidents, and alerts across all teams.'

    dashboard = <AdminDashboard />
  } else if (
    user.role === 'TEAM_MEMBER' &&
    user.team === 'UNIX'
  ) {
    title = 'UNIX Operations'
    description =
      'Monitor UNIX incidents, review alert history, and claim eligible incidents.'

    dashboard = <UnixDashboard />
  } else if (
    user.role === 'TEAM_MEMBER' &&
    user.team === 'DBA'
  ) {
    title = 'Database Operations'
    description =
      'Monitor DBA incidents, review alert history, and claim eligible incidents.'

    dashboard = <DbaDashboard />
  } else {
    // 4. Handle an unexpected role/team combination
    return (
      <div className="flex min-h-screen items-center justify-center bg-slate-950 p-6 text-white">
        <div className="max-w-md rounded-2xl border border-slate-800 bg-slate-900 p-8 text-center">
          <h1 className="text-xl font-bold">
            Account Configuration Error
          </h1>

          <p className="mt-3 text-sm text-slate-400">
            Your account role or team is not recognized.
            Please contact the administrator.
          </p>

          <p className="mt-4 text-xs text-slate-500">
            Role: {user.role ?? 'Unknown'} | Team:{' '}
            {user.team ?? 'Unknown'}
          </p>
        </div>
      </div>
    )
  }

  // 5. Render the selected dashboard inside the common layout
  return (
    <DashboardLayout
      title={title}
      description={description}
    >
      {dashboard}
    </DashboardLayout>
  )
}
