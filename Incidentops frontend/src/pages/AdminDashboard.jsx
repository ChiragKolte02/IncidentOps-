
import IncidentWorkspace from '../components/dashboard/IncidentWorkspace'
import AdminUserManagement from '../components/dashboard/AdminUserManagement'
import AnalyticsDashboard from '../components/dashboard/AnalyticsDashboard'

export default function AdminDashboard() {
  return (
    <div className="space-y-8">
      <AnalyticsDashboard />

      <IncidentWorkspace team="ALL" showClaim />

      <AdminUserManagement />
    </div>
  )
}
