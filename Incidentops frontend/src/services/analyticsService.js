
import { incidentApi } from './apiClient'

const analyticsService = {
  async getOverview(range = '24h') {
    const response = await incidentApi.get(
      '/api/analytics/overview',
      {
        params: { range },
      },
    )

    return response.data
  },
}

export default analyticsService
