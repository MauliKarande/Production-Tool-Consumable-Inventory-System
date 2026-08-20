import axios from 'axios'

export const AUTH_STORAGE_KEY = 'ameya-inventory-auth'

export interface StoredAuth {
  token: string
  username: string
  role: 'ADMIN' | 'ISSUER' | 'VIEWER'
  userId: number
}

export function getStoredAuth(): StoredAuth | null {
  const raw = localStorage.getItem(AUTH_STORAGE_KEY)
  if (!raw) return null
  try {
    return JSON.parse(raw) as StoredAuth
  } catch {
    return null
  }
}

export function setStoredAuth(auth: StoredAuth | null) {
  if (auth) {
    localStorage.setItem(AUTH_STORAGE_KEY, JSON.stringify(auth))
  } else {
    localStorage.removeItem(AUTH_STORAGE_KEY)
  }
}

export const apiClient = axios.create({
  baseURL: import.meta.env.VITE_API_BASE_URL ?? 'http://localhost:7000',
})

apiClient.interceptors.request.use((config) => {
  const auth = getStoredAuth()
  if (auth?.token) {
    config.headers.Authorization = `Bearer ${auth.token}`
  }
  return config
})

apiClient.interceptors.response.use(
  (response) => response,
  (error) => {
    if (error.response?.status === 401) {
      setStoredAuth(null)
      if (!window.location.pathname.startsWith('/login')) {
        window.location.href = '/login'
      }
    }
    return Promise.reject(error)
  },
)

export function apiErrorMessage(error: unknown): string {
  if (axios.isAxiosError(error)) {
    const data = error.response?.data as { message?: string; fieldErrors?: { field: string; message: string }[] } | undefined
    if (data?.fieldErrors?.length) {
      return data.fieldErrors.map((f) => `${f.field}: ${f.message}`).join('; ')
    }
    if (data?.message) return data.message
    if (error.message) return error.message
  }
  return 'Something went wrong. Please try again.'
}
