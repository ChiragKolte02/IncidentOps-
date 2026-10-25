
import { incidentApi } from './apiClient'

const incidentService = {
  async getIncidents() {
    const response = await incidentApi.get('/api/incidents')
    return response.data
  },

  async getIncidentById(id) {
    const response = await incidentApi.get(
      `/api/incidents/${id}`,
    )

    return response.data
  },

  async getOpenIncidents() {
    const response = await incidentApi.get(
      '/api/incidents/open',
    )

    return response.data
  },

  async claimIncident(id) {
    const response = await incidentApi.post(
      `/api/incidents/${id}/claim`,
    )

    return response.data
  },

  async investigateIncident(id) {
    if (id == null || id === '') {
      throw new Error('A valid incident ID is required for AI investigation.')
    }

    const response = await incidentApi.post(
      `/api/incidents/${encodeURIComponent(id)}/investigate`,
      {},
      {
        timeout: 120000,
      },
    )

    return response.data
  },
}

export default incidentService
