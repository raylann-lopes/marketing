import { apiFetch } from '@/lib/api'

export type AccountConfig = {
  id: number
  clientId: number
  instagramAccountId: string
  igUserId: string
  accessTokenMasked: string
  configuredBy: string
  configuredAt: string
}

export type AccountConfigPayload = {
  clientId: number
  igUserId: string
  instagramAccountId?: string
  accessToken?: string
}

// Todos os endpoints requerem role ADMIN
export const accountConfigService = {
  async configure(data: AccountConfigPayload): Promise<AccountConfig> {
    return apiFetch<AccountConfig>('/api/admin/account-config', {
      method: 'POST',
      body: JSON.stringify(data)
    })
  },

  async getByClientId(clientId: number): Promise<AccountConfig> {
    return apiFetch<AccountConfig>(`/api/admin/account-config/client/${clientId}`)
  }
}
