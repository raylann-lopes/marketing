import { apiFetch } from '@/lib/api'

export type EvolutionGroup = {
  groupId: string
  groupName: string
  pictureUrl: string | null
  participantsCount: number | null
  alreadyLinked: boolean
  linkedClientId: number | null
  linkedClientName: string | null
}

export type EvolutionGroupLinkPayload = {
  clientId: number
  groupId: string
}

// Todos os endpoints requerem role ADMIN
export const evolutionGroupService = {
  async getGroups(): Promise<EvolutionGroup[]> {
    return apiFetch<EvolutionGroup[]>('/api/admin/evolution/groups')
  },

  async linkGroupToClient(data: EvolutionGroupLinkPayload): Promise<EvolutionGroup> {
    return apiFetch<EvolutionGroup>('/api/admin/evolution/groups/link', {
      method: 'POST',
      body: JSON.stringify(data)
    })
  }
}
