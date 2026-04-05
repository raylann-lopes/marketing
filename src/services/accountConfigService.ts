import { apiFetch } from '@/lib/api'

export type AccountConfig = {
  id: number
  clientId: number
  instagramAccountId: string
  configuredBy: string
  configuredAt: string
}

// Todos os endpoints requerem role ADMIN
export const accountConfigService = {
  async configure(clientId: number, instagramAccountId: string): Promise<AccountConfig> {
    return apiFetch<AccountConfig>('/api/admin/account-config', {
      method: 'POST',
      body: JSON.stringify({ clientId, instagramAccountId })
    })
  },

  async getByClientId(clientId: number): Promise<AccountConfig> {
    return apiFetch<AccountConfig>(`/api/admin/account-config/client/${clientId}`)
  }
}
