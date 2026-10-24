
import { useCallback, useEffect, useState } from 'react'
import { useAuth } from '../../context/AuthContext'
import userService from '../../services/userService'

function normalizeUsers(data) {
  if (Array.isArray(data)) return data
  if (Array.isArray(data?.content)) return data.content
  if (Array.isArray(data?.data)) return data.data
  return []
}

export default function AdminUserManagement() {
  const { user: currentUser } = useAuth()

  const [users, setUsers] = useState([])
  const [username, setUsername] = useState('')
  const [password, setPassword] = useState('')
  const [role, setRole] = useState('TEAM_MEMBER')
  const [team, setTeam] = useState('UNIX')
  const [loading, setLoading] = useState(true)
  const [saving, setSaving] = useState(false)
  const [deletingId, setDeletingId] = useState(null)
  const [error, setError] = useState('')
  const [notice, setNotice] = useState('')

  const loadUsers = useCallback(async () => {
    setLoading(true)
    setError('')

    try {
      const data = await userService.getUsers()
      setUsers(normalizeUsers(data))
    } catch (err) {
      setError(
        err.response?.data?.message ??
        err.response?.data?.error ??
        'Could not load users. Check that your account has admin permission.'
      )
    } finally {
      setLoading(false)
    }
  }, [])

  useEffect(() => {
    loadUsers()
  }, [loadUsers])

  async function handleCreate(event) {
    event.preventDefault()
    setSaving(true)
    setError('')
    setNotice('')

    const newUser = {
      username: username.trim(),
      password,
      role,
      team: role === 'ADMIN' ? 'NONE' : team,
    }

    try {
      await userService.createUser(newUser)
      setUsername('')
      setPassword('')
      setRole('TEAM_MEMBER')
      setTeam('UNIX')
      setNotice('User created successfully.')
      await loadUsers()
    } catch (err) {
      setError(
        err.response?.data?.message ??
        err.response?.data?.error ??
        'Could not create user. Verify the required fields in the backend request DTO.'
      )
    } finally {
      setSaving(false)
    }
  }

  async function handleDelete(targetUser) {
    if (targetUser.id == null) {
      setError('This user record has no ID, so it cannot be deleted.')
      return
    }

    if (String(targetUser.id) === String(currentUser?.id)) {
      setError('You cannot delete the account you are currently using.')
      return
    }

    const confirmed = window.confirm(
      `Delete user "${targetUser.username}"? This action cannot be undone.`
    )

    if (!confirmed) return

    setDeletingId(targetUser.id)
    setError('')
    setNotice('')

    try {
      await userService.deleteUser(targetUser.id)
      setNotice(`User ${targetUser.username} deleted successfully.`)
      await loadUsers()
    } catch (err) {
      setError(
        err.response?.data?.message ??
        err.response?.data?.error ??
        'Could not delete user. Check backend permissions and constraints.'
      )
    } finally {
      setDeletingId(null)
    }
  }

  return (
    <section
      id="users"
      className="scroll-mt-6 space-y-6 rounded-xl border border-slate-800 bg-slate-900 p-5"
    >
      <div>
        <h3 className="text-lg font-semibold text-white">
          User Management
        </h3>
        <p className="mt-1 text-sm text-slate-400">
          Create team accounts and manage existing users.
        </p>
      </div>

      {error && (
        <div role="alert" className="rounded-lg border border-red-800 bg-red-950/40 p-3 text-sm text-red-300">
          {error}
        </div>
      )}

      {notice && (
        <div role="status" className="rounded-lg border border-emerald-800 bg-emerald-950/30 p-3 text-sm text-emerald-300">
          {notice}
        </div>
      )}

      <form onSubmit={handleCreate} className="grid gap-4 sm:grid-cols-2">
        <label className="text-sm text-slate-300">
          Username
          <input
            value={username}
            onChange={(event) => setUsername(event.target.value)}
            required
            autoComplete="off"
            className="mt-2 w-full rounded-lg border border-slate-700 bg-slate-950 px-3 py-2.5 text-white outline-none focus:border-blue-500"
            placeholder="Enter username"
          />
        </label>

        <label className="text-sm text-slate-300">
          Initial password
          <input
            type="password"
            value={password}
            onChange={(event) => setPassword(event.target.value)}
            required
            minLength={6}
            autoComplete="new-password"
            className="mt-2 w-full rounded-lg border border-slate-700 bg-slate-950 px-3 py-2.5 text-white outline-none focus:border-blue-500"
            placeholder="Enter initial password"
          />
        </label>

        <label className="text-sm text-slate-300">
          Role
          <select
            value={role}
            onChange={(event) => setRole(event.target.value)}
            className="mt-2 w-full rounded-lg border border-slate-700 bg-slate-950 px-3 py-2.5 text-white"
          >
            <option value="TEAM_MEMBER">Team Member</option>
            <option value="ADMIN">Admin</option>
          </select>
        </label>

        <label className="text-sm text-slate-300">
          Team
          <select
            value={role === 'ADMIN' ? 'NONE' : team}
            disabled={role === 'ADMIN'}
            onChange={(event) => setTeam(event.target.value)}
            className="mt-2 w-full rounded-lg border border-slate-700 bg-slate-950 px-3 py-2.5 text-white disabled:opacity-60"
          >
            <option value="UNIX">UNIX</option>
            <option value="DBA">DBA</option>
            <option value="NONE">NONE</option>
          </select>
        </label>

        <div className="sm:col-span-2">
          <button
            type="submit"
            disabled={saving}
            className="rounded-lg bg-blue-600 px-5 py-2.5 text-sm font-semibold text-white hover:bg-blue-500 disabled:opacity-50"
          >
            {saving ? 'Creating user…' : 'Create User'}
          </button>
        </div>
      </form>

      <div className="border-t border-slate-800 pt-5">
        <div className="mb-3 flex items-center justify-between gap-3">
          <h4 className="font-semibold text-white">Existing Users</h4>
          <button
            type="button"
            onClick={loadUsers}
            disabled={loading}
            className="rounded-lg border border-slate-700 px-3 py-2 text-sm hover:bg-slate-800 disabled:opacity-50"
          >
            {loading ? 'Loading…' : 'Refresh'}
          </button>
        </div>

        {loading ? (
          <p className="py-5 text-sm text-slate-400">Loading users…</p>
        ) : users.length === 0 ? (
          <p className="py-5 text-sm text-slate-400">No users found.</p>
        ) : (
          <div className="overflow-x-auto">
            <table className="w-full min-w-[600px] text-left text-sm">
              <thead className="border-b border-slate-800 text-slate-400">
                <tr>
                  <th className="px-3 py-3">Username</th>
                  <th className="px-3 py-3">Role</th>
                  <th className="px-3 py-3">Team</th>
                  <th className="px-3 py-3">Status</th>
                  <th className="px-3 py-3">Action</th>
                </tr>
              </thead>
              <tbody>
                {users.map((item, index) => (
                  <tr
                    key={item.id ?? item.username ?? index}
                    className="border-b border-slate-800/70 last:border-0"
                  >
                    <td className="px-3 py-4">{item.username ?? '—'}</td>
                    <td className="px-3 py-4">{item.role ?? '—'}</td>
                    <td className="px-3 py-4">{item.team ?? '—'}</td>
                    <td className="px-3 py-4">
                      {item.enabled === false ? 'Disabled' : 'Enabled'}
                    </td>
                    <td className="px-3 py-4">
                      <button
                        type="button"
                        onClick={() => handleDelete(item)}
                        disabled={
                          deletingId !== null ||
                          String(item.id) === String(currentUser?.id)
                        }
                        className="rounded-lg border border-red-900 px-3 py-2 text-xs font-semibold text-red-300 hover:bg-red-950 disabled:cursor-not-allowed disabled:opacity-40"
                      >
                        {deletingId === item.id ? 'Deleting…' : 'Delete'}
                      </button>
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        )}
      </div>
    </section>
  )
}
