import { apiFetch } from '@/lib/api'

export type Post = {
  id?: string | number
  clientId?: string | number // Usado no Request
  userId?: string | number // Usado no Request
  title: string
  theme: string // Backend usa 'theme' e 'objective'
  objective: string
  status: 'DEMAND' | 'IN_PRODUCTION' | 'WAITING_APPROVAL' | 'FINISHED' | 'PUBLISHED' | string
  scheduledAt: string
}

export const postService = {
  async getAll(): Promise<Post[]> {
    return apiFetch<Post[]>('/api/posts')
  },

  async getByClientId(clientId: string | number): Promise<Post[]> {
    return apiFetch<Post[]>(`/api/posts/client/${clientId}`)
  },

  async getByStatus(status: string): Promise<Post[]> {
    return apiFetch<Post[]>(`/api/posts/status/${status}`)
  },

  async getBySchedule(scheduledAt: string): Promise<Post[]> {
    return apiFetch<Post[]>(`/api/posts/scheduled/${scheduledAt}`)
  },

  async create(data: Partial<Post>): Promise<Post> {
    return apiFetch<Post>('/api/posts/save', {
      method: 'POST',
      body: JSON.stringify(data)
    })
  },

  async update(id: string | number, data: Partial<Post>): Promise<Post> {
    return apiFetch<Post>(`/api/posts/update/${id}`, {
      method: 'PUT',
      body: JSON.stringify(data)
    })
  },

  async delete(id: string | number): Promise<void> {
    return apiFetch<void>(`/api/posts/delete/${id}`, {
      method: 'DELETE'
    })
  }
}
