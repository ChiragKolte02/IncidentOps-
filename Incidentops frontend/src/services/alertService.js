
import { incidentApi } from './apiClient'

const alertService = {
  async getAlerts() {
    const response = await incidentApi.get('/api/alerts')
    return response.data
  },
}

export default alertService
