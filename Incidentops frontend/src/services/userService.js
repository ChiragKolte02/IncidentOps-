
import { userApi } from './apiClient'

const userService = {
  async getUsers() {
    const response = await userApi.get('/api/admin/users')
    return response.data
  },

  async createUser(userData) {
    const response = await userApi.post(
      '/api/admin/users',
      userData,
    )

    return response.data
  },

  async deleteUser(id) {
    const response = await userApi.delete(
      `/api/admin/users/${id}`,
    )

    return response.data
  },
}

export default userService
