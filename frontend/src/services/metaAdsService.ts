import { apiFetch } from '@/lib/api'

export type MetaAdsAccount = {
  id: string
  accountId: string
  name: string | null
}

// Endpoint protegido por ADMIN no backend
export const metaAdsService = {
  async getAdAccounts(): Promise<MetaAdsAccount[]> {
    return apiFetch<MetaAdsAccount[]>('/api/meta/ad-account')
  }
}
