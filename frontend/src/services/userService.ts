import { apiFetch } from '@/lib/api'

export interface UserProfile {
  id: number
  name: string
  email: string
  role: string
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
  }
}
