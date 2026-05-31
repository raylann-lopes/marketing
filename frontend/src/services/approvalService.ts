import { apiFetch } from '@/lib/api'
import type { Post } from './postService'

// Status espelha o backend: ApproveStatusEnum
export type ApproveStatus = 'PENDING' | 'APPROVE' | 'REJECT'

export type PostApproval = {
  id?: string | number
  postId: string | number
  artS3Key: string   // Chave do objeto no S3 (não a URL)
  artName: string
  caption: string
  status: ApproveStatus
  approvedAt?: string
  approvedUser?: string
  whatsappStanzaId?: string
  whatsappSentAt?: string
  whatsappResponseText?: string
  rejectionReason?: string
  internalRevisionNotes?: string
  post?: Post
}

export type CreateApprovalDTO = {
  postId: string | number
  artS3Key: string
  artName: string
  caption: string
}

export const approvalService = {
  async getAll(): Promise<PostApproval[]> {
    return apiFetch<PostApproval[]>('/api/post-approvals/all')
  },

  async getByPostId(postId: string | number): Promise<PostApproval> {
    return apiFetch<PostApproval>(`/api/post-approvals/${postId}`)
  },

  async getByStatus(status: ApproveStatus): Promise<PostApproval[]> {
    return apiFetch<PostApproval[]>(`/api/post-approvals/status/${status}`)
  },

  async create(data: CreateApprovalDTO): Promise<PostApproval> {
    return apiFetch<PostApproval>('/api/post-approvals/save', {
      method: 'POST',
      body: JSON.stringify(data)
    })
  },

  async update(id: string | number, data: CreateApprovalDTO): Promise<PostApproval> {
    return apiFetch<PostApproval>(`/api/post-approvals/update/${id}`, {
      method: 'PUT',
      body: JSON.stringify(data)
    })
  },

  // ADMIN only
  async approve(postId: string | number): Promise<PostApproval> {
    return apiFetch<PostApproval>(`/api/post-approvals/approve/${postId}`, {
      method: 'POST'
    })
  },

  // ADMIN only
  async reject(postId: string | number): Promise<PostApproval> {
    return apiFetch<PostApproval>(`/api/post-approvals/reject/${postId}`, {
      method: 'POST'
    })
  },

  async rejectInternalApproval(postId: string | number, data: { rejectionReason: string }): Promise<PostApproval> {
    return apiFetch<PostApproval>(`/api/internal/approvals/post/${postId}/reject`, {
      method: 'PATCH',
      body: JSON.stringify(data)
    })
  },

  async internalApprove(postId: string | number, data?: { scheduledAt?: string, internalRevisionNotes?: string }): Promise<PostApproval> {
    return apiFetch<PostApproval>(`/api/internal/approvals/post/${postId}/approve`, {
      method: 'PATCH',
      body: data ? JSON.stringify(data) : undefined
    })
  },

  // ADMIN only
  async delete(id: string | number): Promise<void> {
    return apiFetch<void>(`/api/post-approvals/delete/${id}`, {
      method: 'DELETE'
    })
  }
}
