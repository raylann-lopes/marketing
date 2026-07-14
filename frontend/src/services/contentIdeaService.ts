import { apiFetch } from '@/lib/api'

export type ContentIdeaStatus = 'SUGGESTED' | 'SAVED' | 'DISMISSED' | 'CONVERTED'
export type ContentIdeaPriority = 'HIGH' | 'MEDIUM' | 'LOW'
export type ContentIdeaFormat = 'REELS' | 'CAROUSEL' | 'STORIES' | 'FEED'

export type ContentIdea = {
  id: number
  clientId: number
  clientName: string
  clientNiche: string
  title: string
  hook: string
  theme: string
  objective: string
  format: ContentIdeaFormat
  reason: string
  sourceTerms: string | null
  signalSummary: string | null
  engagementScore: number | null
  status: ContentIdeaStatus
  priority: ContentIdeaPriority
  createdAt: string
}

export const contentIdeaService = {
  async getAll(): Promise<ContentIdea[]> {
    return apiFetch<ContentIdea[]>('/api/content-ideas/all')
  },

  async updateStatus(id: number, status: ContentIdeaStatus): Promise<ContentIdea> {
    return apiFetch<ContentIdea>(`/api/content-ideas/${id}/status`, {
      method: 'PATCH',
      body: JSON.stringify({ status })
    })
  },

  /**
   * Dispara a coleta completa: gera termos (se necessário), busca sinais
   * virais na Apify e persiste as ideias geradas pela IA.
   * Operação demorada (polling na Apify) — pode levar alguns minutos.
   */
  async collect(clientId: number | string): Promise<ContentIdea[]> {
    return apiFetch<ContentIdea[]>(`/api/content-ideas/collect/${clientId}`, {
      method: 'POST'
    })
  }
}
