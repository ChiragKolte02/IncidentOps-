
import IncidentWorkspace from '../components/dashboard/IncidentWorkspace'
import AnalyticsDashboard from '../components/dashboard/AnalyticsDashboard'

export default function DbaDashboard() {
  return (
    <div className="space-y-8">
      <AnalyticsDashboard />
      <IncidentWorkspace team="DBA" showClaim />
    </div>
  )
}