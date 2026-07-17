import { apiFetch } from '@/lib/api'

// Espelha CommentResponseDTO do backend
export type Comment = {
  id: number
  text: string
  authorName: string
  authorId: number
  createdAt: string
}

export type CommentCount = { postId: number; count: number }

export const commentService = {
  async getByPostId(postId: string | number): Promise<Comment[]> {
    return apiFetch<Comment[]>(`/api/comments/post/${postId}`)
  },

  async getCounts(): Promise<CommentCount[]> {
    return apiFetch<CommentCount[]>('/api/comments/counts')
  },

  async create(postId: string | number, text: string): Promise<Comment> {
    return apiFetch<Comment>('/api/comments/save', {
      method: 'POST',
      body: JSON.stringify({ postId, text })
    })
  }
}
