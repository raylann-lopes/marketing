import { apiFetch } from '@/lib/api'

export type FinanceRecord = {
  id?: string | number
  client: string | number // FK (Backend usa 'client', não 'clientId')
  description: string
  value: number // Backend usa 'value', não 'amount'
  status: string // 'PAID', 'PENDING', etc.
  expirationDate: string // Backend usa 'expirationDate', não 'date'
}

export const financeService = {
  async getAll(): Promise<FinanceRecord[]> {
    return apiFetch<FinanceRecord[]>('/api/finance')
  },

  // FILTRO PELO ID DO CLIENTE (FK) - Conforme solicitado!
  async getByClientId(clientId: string | number): Promise<FinanceRecord[]> {
    return apiFetch<FinanceRecord[]>(`/api/finance/client/${clientId}`)
  },

  async create(data: Partial<FinanceRecord>): Promise<FinanceRecord> {
    return apiFetch<FinanceRecord>('/api/finance/create', {
      method: 'POST',
      body: JSON.stringify(data)
    })
  },

  async update(data: Partial<FinanceRecord>): Promise<FinanceRecord> {
    // Note que sua rota é /api/finance/update (PATCH)
    return apiFetch<FinanceRecord>('/api/finance/update', {
      method: 'PATCH',
      body: JSON.stringify(data)
    })
  },

  async delete(id: string | number): Promise<void> {
    // Sua rota /api/finance/delete não tem o ID na URL, 
    // assumirei que ele vai como query parameter ou body.
    // Se for body, precisará ser transformado para POST ou alterado na API.
    return apiFetch<void>(`/api/finance/delete?id=${id}`, {
      method: 'DELETE'
    })
  }
}
