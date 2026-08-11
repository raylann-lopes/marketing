import { apiFetch } from '@/lib/api'

export type AccountConfig = {
  id: number
  clientId: number
  metaAdAccountId: string | null
  igUserId: string
  accessTokenMasked: string
  configuredBy: string
  configuredAt: string
}

export type MetaInstagramAccount = {
  pageId: string
  pageName: string
  igUserId: string
  igUsername: string | null
  igName: string | null
  alreadyLinked: boolean
  linkedClientId: number | null
  linkedClientName: string | null
}

export type MetaInstagramAccountLinkPayload = {
  clientId: number
  pageId: string
  igUserId: string
}

export type MetaAdsAccountLinkPayload = {
  metaAdAccountId: string
}

// Todos os endpoints requerem role ADMIN
export const accountConfigService = {
  async getByClientId(clientId: number): Promise<AccountConfig> {
    return apiFetch<AccountConfig>(`/api/admin/account-config/client/${clientId}`)
  },

  async getMetaInstagramAccounts(): Promise<MetaInstagramAccount[]> {
    return apiFetch<MetaInstagramAccount[]>('/api/admin/meta/instagram-accounts')
  },

  async linkMetaInstagramAccount(data: MetaInstagramAccountLinkPayload): Promise<AccountConfig> {
    return apiFetch<AccountConfig>('/api/admin/meta/instagram-accounts/link', {
      method: 'POST',
      body: JSON.stringify(data)
    })
  },

  async linkMetaAdsAccount(
    clientId: number,
    data: MetaAdsAccountLinkPayload
  ): Promise<AccountConfig> {
    return apiFetch<AccountConfig>(`/api/admin/account-config/client/${clientId}/meta-ad-account`, {
      method: 'PATCH',
      body: JSON.stringify(data)
    })
  }
}
