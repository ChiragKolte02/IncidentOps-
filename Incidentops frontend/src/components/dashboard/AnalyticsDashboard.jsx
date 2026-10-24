
import { useEffect, useState } from 'react'
import {
  ResponsiveContainer,
  AreaChart,
  Area,
  XAxis,
  YAxis,
  CartesianGrid,
  Tooltip,
  Legend,
} from 'recharts'

import analyticsService from '../../services/analyticsService'

const ranges = [
  { value: '24h', label: 'Last 24 hours' },
  { value: '7d', label: 'Last 7 days' },
  { value: '30d', label: 'Last 30 days' },
]

function formatBucket(value, range) {
  if (!value) return ''

  const date = new Date(value)

  if (Number.isNaN(date.getTime())) return String(value)

  if (range === '24h') {
    return date.toLocaleTimeString([], {
      hour: '2-digit',
      minute: '2-digit',
    })
  }

  return date.toLocaleDateString([], {
    month: 'short',
    day: 'numeric',
  })
}

function formatTooltipLabel(value) {
  if (!value) return ''

  const date = new Date(value)

  if (Number.isNaN(date.getTime())) return String(value)

  return date.toLocaleString([], {
    month: 'short',
    day: 'numeric',
    hour: '2-digit',
    minute: '2-digit',
  })
}

function ChartCard({ title, description, children }) {
  return (
    <section className="min-w-0 rounded-2xl border border-slate-800 bg-slate-900/90 p-5 shadow-lg shadow-black/10">
      <h3 className="text-lg font-semibold text-white">
        {title}
      </h3>

      <p className="mt-1 text-sm text-slate-400">
        {description}
      </p>

      <div className="mt-5 h-72 w-full">
        {children}
      </div>
    </section>
  )
}

function StockStyleChart({
  data,
  range,
  firstKey,
  secondKey,
  firstLabel,
  secondLabel,
  firstColor,
  secondColor,
  gradientPrefix,
}) {
  const interval = range === '24h' ? 2 : range === '7d' ? 0 : 4

  return (
    <ResponsiveContainer width="100%" height="100%">
      <AreaChart
        data={data}
        margin={{ top: 10, right: 12, left: -18, bottom: 2 }}
      >
        <defs>
          <linearGradient
            id={`${gradientPrefix}-first`}
            x1="0"
            y1="0"
            x2="0"
            y2="1"
          >
            <stop
              offset="0%"
              stopColor={firstColor}
              stopOpacity={0.35}
            />
            <stop
              offset="95%"
              stopColor={firstColor}
              stopOpacity={0.01}
            />
          </linearGradient>

          <linearGradient
            id={`${gradientPrefix}-second`}
            x1="0"
            y1="0"
            x2="0"
            y2="1"
          >
            <stop
              offset="0%"
              stopColor={secondColor}
              stopOpacity={0.28}
            />
            <stop
              offset="95%"
              stopColor={secondColor}
              stopOpacity={0.01}
            />
          </linearGradient>
        </defs>

        <CartesianGrid
          stroke="#334155"
          strokeDasharray="3 5"
          vertical={false}
        />

        <XAxis
          dataKey="time"
          tickFormatter={(value) => formatBucket(value, range)}
          interval={interval}
          minTickGap={12}
          tick={{ fill: '#94a3b8', fontSize: 11 }}
          axisLine={false}
          tickLine={false}
          tickMargin={10}
        />

        <YAxis
          allowDecimals={false}
          width={38}
          tick={{ fill: '#94a3b8', fontSize: 11 }}
          axisLine={false}
          tickLine={false}
        />

        <Tooltip
          labelFormatter={formatTooltipLabel}
          formatter={(value, name) => [value, name]}
          contentStyle={{
            backgroundColor: '#020617',
            border: '1px solid #334155',
            borderRadius: '12px',
            padding: '10px 14px',
            color: '#f8fafc',
            boxShadow: '0 10px 30px rgba(0,0,0,0.25)',
          }}
          labelStyle={{
            color: '#cbd5e1',
            marginBottom: '6px',
          }}
          itemStyle={{ paddingTop: '2px', paddingBottom: '2px' }}
          cursor={{
            stroke: '#64748b',
            strokeDasharray: '4 4',
          }}
        />

        <Legend
          verticalAlign="bottom"
          height={32}
          iconType="circle"
          wrapperStyle={{
            color: '#cbd5e1',
            fontSize: '12px',
            paddingTop: '8px',
          }}
        />

        <Area
          type="monotone"
          dataKey={firstKey}
          name={firstLabel}
          stroke={firstColor}
          strokeWidth={2.5}
          fill={`url(#${gradientPrefix}-first)`}
          fillOpacity={1}
          activeDot={{
            r: 5,
            stroke: firstColor,
            strokeWidth: 2,
            fill: '#0f172a',
          }}
          dot={false}
          connectNulls
          animationDuration={700}
        />

        <Area
          type="monotone"
          dataKey={secondKey}
          name={secondLabel}
          stroke={secondColor}
          strokeWidth={2.5}
          fill={`url(#${gradientPrefix}-second)`}
          fillOpacity={1}
          activeDot={{
            r: 5,
            stroke: secondColor,
            strokeWidth: 2,
            fill: '#0f172a',
          }}
          dot={false}
          connectNulls
          animationDuration={900}
        />
      </AreaChart>
    </ResponsiveContainer>
  )
}

function MetricCard({ label, value, description, accent }) {
  return (
    <div className="relative overflow-hidden rounded-2xl border border-slate-800 bg-slate-900 p-5">
      <div
        className={`absolute inset-x-0 top-0 h-0.5 ${accent}`}
      />

      <p className="text-sm font-medium text-slate-400">
        {label}
      </p>

      <p className="mt-3 text-3xl font-bold tracking-tight text-white">
        {value}
      </p>

      <p className="mt-2 text-xs text-slate-500">
        {description}
      </p>
    </div>
  )
}

export default function AnalyticsDashboard() {
  const [range, setRange] = useState('24h')
  const [overview, setOverview] = useState(null)
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState('')

  useEffect(() => {
    let active = true

    async function loadAnalytics() {
      setLoading(true)
      setError('')

      try {
        const result = await analyticsService.getOverview(range)

        if (active) {
          setOverview(result)
        }
      } catch (err) {
        if (active) {
          setError(
            err?.response?.data?.message ??
              err?.response?.data?.error ??
              'Unable to load analytics. Check the Incident Service and analytics endpoint.',
          )
        }
      } finally {
        if (active) {
          setLoading(false)
        }
      }
    }

    loadAnalytics()

    return () => {
      active = false
    }
  }, [range])

  return (
    <section className="space-y-5">
      <div className="flex flex-col justify-between gap-4 sm:flex-row sm:items-center">
        <div>
          <h2 className="text-2xl font-bold tracking-tight text-white">
            Monitoring Analytics
          </h2>

          <p className="mt-1 text-sm text-slate-400">
            Visualize alert activity and incident trends over time.
          </p>
        </div>

        <div
          className="flex flex-wrap gap-2"
          aria-label="Analytics time range"
        >
          {ranges.map((item) => (
            <button
              key={item.value}
              type="button"
              onClick={() => setRange(item.value)}
              aria-pressed={range === item.value}
              className={`rounded-lg border px-3 py-2 text-sm font-medium transition-all ${
                range === item.value
                  ? 'border-blue-500 bg-blue-600 text-white shadow-lg shadow-blue-950/40'
                  : 'border-slate-700 bg-slate-900 text-slate-300 hover:border-slate-600 hover:bg-slate-800'
              }`}
            >
              {item.label}
            </button>
          ))}
        </div>
      </div>

      {error && (
        <div
          role="alert"
          className="rounded-xl border border-red-800 bg-red-950/40 p-4 text-sm text-red-300"
        >
          {error}
        </div>
      )}

      {loading ? (
        <div className="flex items-center gap-3 rounded-xl border border-slate-800 bg-slate-900 p-6 text-sm text-slate-400">
          <div className="h-5 w-5 animate-spin rounded-full border-2 border-slate-700 border-t-blue-500" />
          Loading monitoring analytics…
        </div>
      ) : !error && overview ? (
        <>
          <div className="grid grid-cols-1 gap-4 sm:grid-cols-2 xl:grid-cols-4">
            <MetricCard
              label="Open Incidents"
              value={overview.openIncidents ?? 0}
              description="Currently open"
              accent="bg-amber-400"
            />

            <MetricCard
              label="Alert Time Buckets"
              value={overview.alerts?.length ?? 0}
              description="Points in selected range"
              accent="bg-blue-400"
            />

            <MetricCard
              label="Incident Time Buckets"
              value={overview.incidents?.length ?? 0}
              description="Points in selected range"
              accent="bg-emerald-400"
            />

            <MetricCard
              label="Time Range"
              value={range.toUpperCase()}
              description={
                ranges.find((item) => item.value === range)?.label
              }
              accent="bg-violet-400"
            />
          </div>

          <div className="grid grid-cols-1 gap-5 2xl:grid-cols-2">
            <ChartCard
              title="Alert Activity"
              description="Firing and resolved alerts · trend view"
            >
              <StockStyleChart
                data={overview.alerts ?? []}
                range={range}
                firstKey="firing"
                secondKey="resolved"
                firstLabel="Firing"
                secondLabel="Resolved"
                firstColor="#60a5fa"
                secondColor="#34d399"
                gradientPrefix="alert-activity"
              />
            </ChartCard>

            <ChartCard
              title="Incident Lifecycle"
              description="Created and resolved incidents · trend view"
            >
              <StockStyleChart
                data={overview.incidents ?? []}
                range={range}
                firstKey="created"
                secondKey="resolved"
                firstLabel="Created"
                secondLabel="Resolved"
                firstColor="#a78bfa"
                secondColor="#34d399"
                gradientPrefix="incident-lifecycle"
              />
            </ChartCard>
          </div>
        </>
      ) : null}
    </section>
  )
}
