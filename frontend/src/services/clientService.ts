import { apiFetch } from '@/lib/api'

export type Client = {
  id?: string | number
  name: string
  email: string
  number?: string
  driveLink?: string
  voiceTone?: string
  niche?: string
  whatsappGroupId?: string | null
  whatsappGroupName?: string | null
  status: 'ACTIVE' | 'INACTIVE' | string
  monthlyValue?: number
  createdAt?: string
}

export const clientService = {
  async getAll(): Promise<Client[]> {
    return apiFetch<Client[]>('/api/clients')
  },

  async getByEmail(email: string): Promise<Client> {
    return apiFetch<Client>(`/api/clients/email/${email}`)
  },

  async getByStatus(status: string): Promise<Client[]> {
    return apiFetch<Client[]>(`/api/clients/status/${status}`)
  },

  async create(data: Client): Promise<Client> {
    return apiFetch<Client>('/api/clients', {
      method: 'POST',
      body: JSON.stringify(data)
    })
  },

  async update(id: string | number, data: Partial<Client>): Promise<Client> {
    return apiFetch<Client>(`/api/clients/${id}`, {
      method: 'PUT',
      body: JSON.stringify(data)
    })
  },

  async delete(id: string | number): Promise<void> {
    return apiFetch<void>(`/api/clients/${id}`, {
      method: 'DELETE'
    })
  },

  async updateStatus(id: string | number, status: 'ACTIVE' | 'INACTIVE'): Promise<Client> {
    return apiFetch<Client>(`/api/clients/${id}/status`, {
      method: 'PATCH',
      body: JSON.stringify({ status })
    })
  }
}
