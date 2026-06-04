import { apiFetch } from '@/lib/api'

export interface UserProfile {
  id: number
  name: string
  email: string
  role: string
  active: boolean
}

export interface UpdateProfilePayload {
  name: string
  email: string
  password?: string
}

export interface ChangePasswordPayload {
  currentPassword: string
  newPassword: string
}

export interface CreateUserPayload {
  name: string
  email: string
  password: string
}

export const userService = {
  async getMe(): Promise<UserProfile> {
    return apiFetch<UserProfile>('/api/users/me')
  },

  async updateProfile(data: UpdateProfilePayload): Promise<UserProfile> {
    return apiFetch<UserProfile>('/api/users/me', {
      method: 'PUT',
      body: JSON.stringify(data)
    })
  },

  async changePassword(data: ChangePasswordPayload): Promise<void> {
    return apiFetch<void>('/api/users/me/password', {
      method: 'PUT',
      body: JSON.stringify(data)
    })
  },

  async listAll(page = 0, size = 50): Promise<UserProfile[]> {
    return apiFetch<UserProfile[]>(`/api/users?page=${page}&size=${size}`)
  },

  async create(data: CreateUserPayload): Promise<UserProfile> {
    return apiFetch<UserProfile>('/api/users', {
      method: 'POST',
      body: JSON.stringify(data)
    })
  },

  async updateRole(id: number, role: 'ADMIN' | 'USER'): Promise<UserProfile> {
    return apiFetch<UserProfile>(`/api/users/id/${id}/role`, {
      method: 'PATCH',
      body: JSON.stringify({ role })
    })
  },

  async activateById(id: number): Promise<UserProfile> {
    return apiFetch<UserProfile>(`/api/users/id/${id}/activate`, { method: 'PATCH' })
  },

  async updateById(id: number, data: { name: string; email: string }): Promise<UserProfile> {
    return apiFetch<UserProfile>(`/api/users/id/${id}`, {
      method: 'PUT',
      body: JSON.stringify(data)
    })
  },

  async deleteById(id: number): Promise<void> {
    return apiFetch<void>(`/api/users/id/${id}`, { method: 'DELETE' })
  }
}
