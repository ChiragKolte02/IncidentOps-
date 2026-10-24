
import axios from 'axios'

export const userApi = axios.create({
  baseURL: '/user-api',
  withCredentials: true,
  headers: {
    'Content-Type': 'application/json',
  },
})

export const incidentApi = axios.create({
  baseURL: '/incident-api',
  withCredentials: true,
  headers: {
    'Content-Type': 'application/json',
  },
})

export const alertApi = axios.create({
  baseURL: '/alert-api',
  withCredentials: true,
  headers: {
    'Content-Type': 'application/json',
  },
})
