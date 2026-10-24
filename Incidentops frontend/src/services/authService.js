
import { userApi } from './apiClient'

const authService = {
  async login(username, password) {
    const response = await userApi.post('/api/auth/login', {
      username,
      password,
    })

    // Do not store the JWT in localStorage.
    // The backend sets the HttpOnly authentication cookie.
    return response.data
  },

  async logout() {
    await userApi.post('/api/auth/logout')
  },

  async getCurrentUser() {
    const response = await userApi.get('/api/users/me')
    return response.data
  },
}

export default authService
