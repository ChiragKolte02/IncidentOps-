
import { useCallback, useEffect, useMemo, useState } from 'react'
import incidentService from '../../services/incidentService'
import alertService from '../../services/alertService'

function toArray(data) {
  if (Array.isArray(data)) return data
  if (Array.isArray(data?.content)) return data.content
  if (Array.isArray(data?.data)) return data.data
  if (Array.isArray(data?.items)) return data.items
  return []
}

function getStatus(item) {
  return String(item?.status ?? 'UNKNOWN').toUpperCase()
}

function getTeam(item) {
  return String(item?.team ?? '').toUpperCase()
}

function displayId(item) {
  return item?.incidentNumber ?? item?.id ?? item?.fingerprint ?? '—'
}

function displaySummary(item) {
  return (
    item?.alertName ??
    item?.title ??
    item?.summary ??
    item?.description ??
    item?.name ??
    '—'
  )
}

function StatusLabel({ value }) {
  const status = String(value ?? 'UNKNOWN').toUpperCase()

  const style =
    status === 'OPEN' || status === 'FIRING'
      ? 'bg-red-500/10 text-red-300'
      : status === 'RESOLVED'
        ? 'bg-emerald-500/10 text-emerald-300'
        : 'bg-slate-700 text-slate-300'

  return (
    <span
      className={`inline-flex rounded-full px-2.5 py-1 text-xs font-medium ${style}`}
    >
      {status}
    </span>
  )
}

function DataTable({ columns, rows, emptyMessage }) {
  if (!rows.length) {
    return (
      <p className="py-6 text-sm text-slate-400">{emptyMessage}</p>
    )
  }

  return (
    <div className="overflow-x-auto">
      <table className="w-full min-w-[650px] text-left text-sm">
        <thead className="border-b border-slate-800 text-slate-400">
          <tr>
            {columns.map((column) => (
              <th key={column.key} className="px-3 py-3 font-medium">
                {column.label}
              </th>
            ))}
          </tr>
        </thead>

        <tbody>
          {rows.map((row, index) => (
            <tr
              key={row.id ?? row.incidentNumber ?? row.fingerprint ?? index}
              className="border-b border-slate-800/70 last:border-0"
            >
              {columns.map((column) => (
                <td key={column.key} className="px-3 py-4 text-slate-300">
                  {column.render
                    ? column.render(row)
                    : (row[column.key] ?? '—')}
                </td>
              ))}
            </tr>
          ))}
        </tbody>
      </table>
    </div>
  )
}

function getApiError(error, fallback) {
  return (
    error?.response?.data?.message ??
    error?.response?.data?.error ??
    (typeof error?.response?.data === 'string'
      ? error.response.data
      : null) ??
    fallback
  )
}

export default function IncidentWorkspace({
  team = 'ALL',
  showClaim = false,
}) {
  const [incidents, setIncidents] = useState([])
  const [alerts, setAlerts] = useState([])
  const [loading, setLoading] = useState(true)
  const [refreshing, setRefreshing] = useState(false)
  const [claimingId, setClaimingId] = useState(null)
  const [error, setError] = useState('')
  const [notice, setNotice] = useState('')

  const loadData = useCallback(async (manual = false) => {
    if (manual) {
      setRefreshing(true)
    } else {
      setLoading(true)
    }

    setError('')

    try {
      const [incidentResult, alertResult] = await Promise.allSettled([
        incidentService.getIncidents(),
        alertService.getAlerts(),
      ])

      const errors = []

      if (incidentResult.status === 'fulfilled') {
        setIncidents(toArray(incidentResult.value))
      } else {
        errors.push(
          getApiError(
            incidentResult.reason,
            'Unable to load incidents. Check the Incident Service and your session.'
          )
        )
      }

      if (alertResult.status === 'fulfilled') {
        setAlerts(toArray(alertResult.value))
      } else {
        errors.push(
          getApiError(
            alertResult.reason,
            'Unable to load alerts. Check the alerts API and your permissions.'
          )
        )
      }

      if (errors.length) {
        setError(errors.join(' '))
      }
    } catch {
      setError('An unexpected error occurred while loading dashboard data.')
    } finally {
      setLoading(false)
      setRefreshing(false)
    }
  }, [])

  useEffect(() => {
    loadData()
  }, [loadData])

  const matchesTeam = useCallback(
    (item) => team === 'ALL' || getTeam(item) === team,
    [team]
  )

  const visibleIncidents = useMemo(
    () => incidents.filter(matchesTeam),
    [incidents, matchesTeam]
  )

  const openIncidents = useMemo(
    () =>
      visibleIncidents.filter(
        (incident) => getStatus(incident) === 'OPEN'
      ),
    [visibleIncidents]
  )

  const resolvedIncidents = useMemo(
    () =>
      visibleIncidents.filter(
        (incident) => getStatus(incident) === 'RESOLVED'
      ),
    [visibleIncidents]
  )

  const visibleAlerts = useMemo(
    () => alerts.filter(matchesTeam),
    [alerts, matchesTeam]
  )

  async function handleClaim(incident) {
    const id = incident?.id

    if (id == null) {
      setError(
        'This incident response has no id field. Check the incident JSON and use its actual database ID for claiming.'
      )
      return
    }

    setClaimingId(id)
    setError('')
    setNotice('')

    try {
      await incidentService.claimIncident(id)

      setNotice(`Incident ${displayId(incident)} claimed successfully.`)

      // Refresh both lists so the latest backend state is shown.
      await loadData(true)
    } catch (err) {
      setError(
        getApiError(
          err,
          'Claim failed. The incident may already be claimed, or your account may not have permission.'
        )
      )
    } finally {
      setClaimingId(null)
    }
  }

  const incidentColumns = useMemo(() => {
    const columns = [
      {
        key: 'number',
        label: 'Incident',
        render: (item) => displayId(item),
      },
      {
        key: 'summary',
        label: 'Alert / Summary',
        render: (item) => displaySummary(item),
      },
      {
        key: 'status',
        label: 'Status',
        render: (item) => <StatusLabel value={item.status} />,
      },
      {
        key: 'severity',
        label: 'Severity',
        render: (item) => item.severity ?? '—',
      },
      {
        key: 'team',
        label: 'Team',
        render: (item) => item.team ?? '—',
      },
    ]

    if (showClaim) {
      columns.push({
        key: 'actions',
        label: 'Action',
        render: (item) => {
          const id = item.id
          const canClaim =
            getStatus(item) === 'OPEN' &&
            id != null

          if (!canClaim) {
            return (
              <span className="text-slate-500">
                {getStatus(item) !== 'OPEN' ? '—' : 'Missing ID'}
              </span>
            )
          }

          return (
            <button
              type="button"
              onClick={() => handleClaim(item)}
              disabled={claimingId !== null}
              className="rounded-lg bg-blue-600 px-3 py-2 text-xs font-semibold text-white hover:bg-blue-500 disabled:cursor-not-allowed disabled:opacity-50"
            >
              {claimingId === id ? 'Claiming…' : 'Claim'}
            </button>
          )
        },
      })
    }

    return columns
  }, [showClaim, claimingId])

  const alertColumns = [
    {
      key: 'name',
      label: 'Alert',
      render: (item) => displaySummary(item),
    },
    {
      key: 'status',
      label: 'Status',
      render: (item) => <StatusLabel value={item.status} />,
    },
    {
      key: 'severity',
      label: 'Severity',
      render: (item) => item.severity ?? '—',
    },
    {
      key: 'instance',
      label: 'Source',
      render: (item) => item.instance ?? item.source ?? '—',
    },
    {
      key: 'team',
      label: 'Team',
      render: (item) => item.team ?? '—',
    },
  ]

  if (loading) {
    return (
      <div className="flex items-center gap-3 py-8 text-sm text-slate-400">
        <div className="h-5 w-5 animate-spin rounded-full border-2 border-slate-700 border-t-blue-500" />
        Loading incidents and alerts…
      </div>
    )
  }

  return (
    <div className="space-y-6">
      <div className="flex flex-wrap items-center justify-between gap-3">
        <p className="text-sm text-slate-400">
          {team === 'ALL' ? 'All teams' : `${team} team`} workspace
        </p>

        <button
          type="button"
          onClick={() => loadData(true)}
          disabled={refreshing || claimingId !== null}
          className="rounded-lg border border-slate-700 px-4 py-2 text-sm text-white hover:bg-slate-800 disabled:cursor-not-allowed disabled:opacity-50"
        >
          {refreshing ? 'Refreshing…' : 'Refresh data'}
        </button>
      </div>

      {error && (
        <div
          role="alert"
          className="rounded-lg border border-red-800 bg-red-950/40 p-4 text-sm text-red-300"
        >
          {error}
        </div>
      )}

      {notice && (
        <div
          role="status"
          className="rounded-lg border border-emerald-800 bg-emerald-950/30 p-4 text-sm text-emerald-300"
        >
          {notice}
        </div>
      )}

      <div className="grid grid-cols-2 gap-4 xl:grid-cols-4">
        {[
          ['Total incidents', visibleIncidents.length],
          ['Open incidents', openIncidents.length],
          ['Resolved incidents', resolvedIncidents.length],
          ['Alerts', visibleAlerts.length],
        ].map(([label, value]) => (
          <div
            key={label}
            className="rounded-xl border border-slate-800 bg-slate-900 p-5"
          >
            <p className="text-sm text-slate-400">{label}</p>
            <p className="mt-2 text-3xl font-bold text-white">{value}</p>
          </div>
        ))}
      </div>

      <section
        id="incidents"
        className="scroll-mt-6 rounded-xl border border-slate-800 bg-slate-900 p-5"
      >
        <h3 className="text-lg font-semibold text-white">Open Incidents</h3>
        <p className="mt-1 text-sm text-slate-400">
          Review active incidents and claim eligible incidents.
        </p>

        <div className="mt-4">
          <DataTable
            columns={incidentColumns}
            rows={openIncidents}
            emptyMessage="There are no open incidents for this workspace."
          />
        </div>
      </section>

      <section
        id="all-incidents"
        className="scroll-mt-6 rounded-xl border border-slate-800 bg-slate-900 p-5"
      >
        <h3 className="text-lg font-semibold text-white">All Incidents</h3>
        <p className="mt-1 text-sm text-slate-400">
          View incident history, including resolved incidents.
        </p>

        <div className="mt-4">
          <DataTable
            columns={incidentColumns}
            rows={visibleIncidents}
            emptyMessage="No incidents found."
          />
        </div>
      </section>

      <section
        id="alerts"
        className="scroll-mt-6 rounded-xl border border-slate-800 bg-slate-900 p-5"
      >
        <h3 className="text-lg font-semibold text-white">Alert History</h3>

        <div className="mt-4">
          <DataTable
            columns={alertColumns}
            rows={visibleAlerts}
            emptyMessage="No alerts found."
          />
        </div>
      </section>
    </div>
  )
}
