import { apiFetch } from '@/lib/api'

export type PostStatus =
  | 'DEMAND'
  | 'IN_PRODUCTION'
  | 'REJECTED'
  | 'FINISHED'
  | 'WAITING_APPROVAL'
  | 'SCHEDULE'
  | 'PUBLISHED'

export type Post = {
  id?: string | number
  clientId?: string | number // Usado no Request
  userId?: string | number // Usado no Request
  title: string
  theme: string // Backend usa 'theme' e 'objective'
  objective: string
  status: PostStatus | string
  isUrgent?: boolean
  referenceImageS3Key?: string
  scheduledAt: string
}

export type CaptionResponse = {
  caption: string
  [key: string]: unknown
}

export function getPostId(post: Post): string {
  return String(post.id ?? '')
}

export function getPostClientId(post: Post): number | null {
  const rawClient = (post as unknown as { client?: unknown }).client
  if (typeof rawClient === 'number' || typeof rawClient === 'string') return Number(rawClient)
  if (rawClient && typeof rawClient === 'object') {
    const rawClientId = (rawClient as { id?: unknown }).id
    if (rawClientId !== undefined && rawClientId !== null) return Number(rawClientId)
  }
  if (post.clientId !== undefined && post.clientId !== null) return Number(post.clientId)
  return null
}

export const postService = {
  async getAll(): Promise<Post[]> {
    return apiFetch<Post[]>('/api/posts')
  },

  async getByClientId(clientId: string | number): Promise<Post[]> {
    return apiFetch<Post[]>(`/api/posts/client/${clientId}`)
  },

  async getByStatus(status: PostStatus): Promise<Post[]> {
    return apiFetch<Post[]>(`/api/posts/status/${status}`)
  },

  async getBySchedule(scheduledAt: string, scheduledAtBefore: string): Promise<Post[]> {
    const params = new URLSearchParams({ scheduledAtBefore })
    return apiFetch<Post[]>(`/api/posts/scheduled/${scheduledAt}?${params}`)
  },

  async create(data: Partial<Post>): Promise<Post> {
    return apiFetch<Post>('/api/posts/save', {
      method: 'POST',
      body: JSON.stringify(data),
    })
  },

  async update(id: string | number, data: Partial<Post>): Promise<Post> {
    return apiFetch<Post>(`/api/posts/update/${id}`, {
      method: 'PUT',
      body: JSON.stringify(data),
    })
  },

  async delete(id: string | number): Promise<void> {
    return apiFetch<void>(`/api/posts/delete/${id}`, {
      method: 'DELETE',
    })
  },

  async updateReference(id: string | number, s3Key: string): Promise<Post> {
    return apiFetch<Post>(`/api/posts/update-reference/${id}?s3Key=${encodeURIComponent(s3Key)}`, {
      method: 'PATCH',
    })
  },

  async generateCaption(id: string | number, artS3Key?: string): Promise<CaptionResponse> {
    return apiFetch<CaptionResponse>(`/api/posts/${id}/generate-caption`, {
      method: 'POST',
      body: JSON.stringify({ artS3Key }),
    })
  },
}
