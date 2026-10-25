
function formatNumber(value) {
  if (value == null || !Number.isFinite(Number(value))) {
    return '—'
  }

  return Number(value).toFixed(2)
}

function formatMetricName(name) {
  return String(name ?? '')
    .replaceAll('_', ' ')
    .replace(/\b\w/g, (letter) => letter.toUpperCase())
}

function Section({ title, children }) {
  return (
    <section className="space-y-3">
      <h4 className="text-base font-semibold text-white">
        {title}
      </h4>
      {children}
    </section>
  )
}

export default function AiInvestigationReport({ data, onClose }) {
  if (!data) return null

  const report = data.report ?? {}
  const evidence = data.evidence ?? {}
  const metrics = evidence.metrics ?? {}

  const observations = report.observations ?? []
  const possibleCauses = report.possible_causes ?? []
  const actions = report.recommended_actions ?? []
  const limitations = report.limitations ?? []

  return (
    <div
      className="space-y-6 rounded-2xl border border-violet-700/50 bg-slate-950 p-5 shadow-xl"
      aria-label="AI investigation report"
    >
      <div className="flex flex-wrap items-start justify-between gap-4">
        <div>
          <p className="text-xs font-semibold uppercase tracking-widest text-violet-400">
            AI Investigation
          </p>

          <h3 className="mt-2 text-xl font-bold text-white">
            {data.incident_number ?? 'Incident Investigation'}
          </h3>

          <p className="mt-1 text-sm text-slate-400">
            {data.team ?? 'Unknown team'} · {data.instance ?? 'Unknown instance'}
          </p>
        </div>

        {onClose && (
          <button
            type="button"
            onClick={onClose}
            className="rounded-lg border border-slate-700 px-3 py-2 text-sm text-slate-300 hover:bg-slate-800"
          >
            Close report
          </button>
        )}
      </div>

      <div className="rounded-xl border border-slate-800 bg-slate-900 p-4">
        <p className="text-xs font-semibold uppercase tracking-wide text-slate-400">
          Root Cause Status
        </p>

        <p className={`mt-2 font-semibold ${
          report.root_cause_confirmed === true
            ? 'text-emerald-400'
            : 'text-amber-400'
        }`}>
          {report.root_cause_confirmed === true
            ? 'Root cause reported as confirmed'
            : 'Root cause not confirmed'}
        </p>

        <p className="mt-2 text-xs text-slate-500">
          AI findings are advisory and should be verified by an engineer.
        </p>
      </div>

      <Section title="Investigation Summary">
        <p className="whitespace-pre-wrap rounded-xl bg-slate-900 p-4 text-sm leading-7 text-slate-300">
          {report.summary ?? 'No summary was returned.'}
        </p>
      </Section>

      <Section title="Prometheus Metric Evidence">
        {Object.keys(metrics).length === 0 ? (
          <p className="text-sm text-slate-400">
            No metric evidence was returned.
          </p>
        ) : (
          <div className="grid gap-4 md:grid-cols-2">
            {Object.entries(metrics).map(([name, series]) => (
              <div
                key={name}
                className="rounded-xl border border-slate-800 bg-slate-900 p-4"
              >
                <h5 className="font-semibold text-white">
                  {formatMetricName(name)}
                </h5>

                {!Array.isArray(series) || series.length === 0 ? (
                  <p className="mt-3 text-sm text-slate-400">
                    No data available.
                  </p>
                ) : (
                  <div className="mt-3 space-y-3">
                    {series.map((item, index) => (
                      <div
                        key={`${name}-${index}`}
                        className="rounded-lg border border-slate-800 bg-slate-950 p-3"
                      >
                        {Object.keys(item.labels ?? {}).length > 0 && (
                          <p className="mb-3 break-words text-xs text-slate-400">
                            {Object.entries(item.labels)
                              .map(([key, value]) => `${key}: ${value}`)
                              .join(' · ')}
                          </p>
                        )}

                        <div className="grid grid-cols-3 gap-2 text-sm">
                          <div>
                            <p className="text-xs text-slate-500">Minimum</p>
                            <p className="mt-1 font-semibold text-white">
                              {formatNumber(item.minimum)}
                            </p>
                          </div>

                          <div>
                            <p className="text-xs text-slate-500">Average</p>
                            <p className="mt-1 font-semibold text-blue-300">
                              {formatNumber(item.average)}
                            </p>
                          </div>

                          <div>
                            <p className="text-xs text-slate-500">Maximum</p>
                            <p className="mt-1 font-semibold text-white">
                              {formatNumber(item.maximum)}
                            </p>
                          </div>
                        </div>

                        <p className="mt-3 text-xs text-slate-500">
                          {item.sample_count ?? 0} samples
                        </p>
                      </div>
                    ))}
                  </div>
                )}
              </div>
            ))}
          </div>
        )}
      </Section>

      <Section title="AI Observations">
        {observations.length === 0 ? (
          <p className="text-sm text-slate-400">No observations available.</p>
        ) : (
          <div className="space-y-3">
            {observations.map((item, index) => (
              <div
                key={index}
                className="rounded-xl border border-slate-800 bg-slate-900 p-4"
              >
                <p className="text-sm font-semibold text-blue-300">
                  {formatMetricName(item.metric)}
                </p>
                <p className="mt-2 text-sm leading-6 text-slate-300">
                  {item.finding}
                </p>
              </div>
            ))}
          </div>
        )}
      </Section>

      <Section title="Possible Causes">
        {possibleCauses.length === 0 ? (
          <p className="text-sm text-slate-400">
            No possible causes were identified.
          </p>
        ) : (
          <div className="space-y-3">
            {possibleCauses.map((item, index) => (
              <div
                key={index}
                className="rounded-xl border border-amber-900/40 bg-slate-900 p-4"
              >
                <div className="flex flex-wrap items-center justify-between gap-2">
                  <p className="font-medium text-white">
                    {item.cause}
                  </p>

                  <span className="rounded-full bg-amber-500/10 px-2.5 py-1 text-xs text-amber-300">
                    Confidence: {item.confidence ?? 'unknown'}
                  </span>
                </div>

                <p className="mt-2 text-sm leading-6 text-slate-400">
                  {item.reason}
                </p>
              </div>
            ))}
          </div>
        )}
      </Section>

      <Section title="Recommended Actions">
        {actions.length === 0 ? (
          <p className="text-sm text-slate-400">
            No recommended actions available.
          </p>
        ) : (
          <ol className="space-y-3">
            {actions.map((item, index) => (
              <li
                key={index}
                className="rounded-xl border border-emerald-900/40 bg-slate-900 p-4"
              >
                <p className="font-medium text-emerald-300">
                  {index + 1}. {item.action}
                </p>

                <p className="mt-2 text-sm leading-6 text-slate-400">
                  {item.reason}
                </p>
              </li>
            ))}
          </ol>
        )}
      </Section>

      <Section title="Investigation Limitations">
        {limitations.length === 0 ? (
          <p className="text-sm text-slate-400">
            No limitations were provided.
          </p>
        ) : (
          <ul className="list-disc space-y-2 pl-5 text-sm leading-6 text-slate-400">
            {limitations.map((item, index) => (
              <li key={index}>{item}</li>
            ))}
          </ul>
        )}
      </Section>

      <p className="border-t border-slate-800 pt-4 text-xs text-slate-500">
        AI-generated investigation based on available monitoring evidence.
        This report does not automatically resolve or modify the incident.
      </p>
    </div>
  )
}
