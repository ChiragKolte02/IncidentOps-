
import IncidentWorkspace from '../components/dashboard/IncidentWorkspace'
import AnalyticsDashboard from '../components/dashboard/AnalyticsDashboard'

export default function UnixDashboard() {
  return (
    <div className="space-y-8">
      <AnalyticsDashboard />
      <IncidentWorkspace team="UNIX" showClaim />
    </div>
  )
}