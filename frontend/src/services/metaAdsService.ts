import { apiFetch } from '@/lib/api'

export type MetaAdsAccount = {
  id: string
  accountId: string
  name: string | null
}

export type AdsReportSnapshot = {
  clientId: number
  clientName: string
  accountId: string
  accountName: string
  dateStart: string
  dateStop: string
  summary: {
    spend: number
    reach: number
    impressions: number
    clicks: number
    ctr: number
    cpc: number
    cpm: number
  }
  actions: {
    linkClicks: number
    postEngagement: number
    postReactions: number
    videoViews: number
    conversationsStarted: number
    messagingConnections: number
  }
}

// Endpoint protegido por ADMIN no backend
export const metaAdsService = {
  async getAdAccounts(): Promise<MetaAdsAccount[]> {
    return apiFetch<MetaAdsAccount[]>('/api/admin/meta/ad-account')
  },

  async getReportSnapshot(
    clientId: string | number,
    dateStart: string,
    dateStop: string
  ): Promise<AdsReportSnapshot> {
    return apiFetch<AdsReportSnapshot>(
      `/api/admin/meta/ad-account/clients/${clientId}/report-snapshot?dateStart=${dateStart}&dateStop=${dateStop}`
    )
  }
}
