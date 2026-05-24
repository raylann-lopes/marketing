import { apiFetch } from '@/lib/api'

export type FinanceStatus = 'PENDING' | 'PAY'

export type ForecastClientMonth = { expected: number; received: number; pending: number }
export type ForecastClient = {
  clientId: number
  clientName: string
  niche: string
  monthlyValue: number
  status: 'ACTIVE' | 'INACTIVE'
  monthData: Record<string, ForecastClientMonth>
}
export type ForecastMonth = { monthKey: string; monthLabel: string; expected: number; received: number; pending: number }
export type ForecastData = { clients: ForecastClient[]; months: ForecastMonth[]; monthlyTotal: number; annualTotal: number; year: number }

export type FinanceRecord = {
  id?: string | number
  client: string | number
  description: string
  value: number
  status: FinanceStatus | string
  expirationDate: string
}

export type FinancePayload = {
  id?: string | number
  client: { id: number }
  user: { id: number | null }
  description: string
  value: number
  status: FinanceStatus | string
  expirationDate: string
  paymentDate: string
}

export const financeService = {
  async getAll(): Promise<FinanceRecord[]> {
    return apiFetch<FinanceRecord[]>('/api/finance')
  },

  // FILTRO PELO ID DO CLIENTE (FK) - Conforme solicitado!
  async getByClientId(clientId: string | number): Promise<FinanceRecord[]> {
    return apiFetch<FinanceRecord[]>(`/api/finance/client/${clientId}`)
  },

  async create(data: FinancePayload): Promise<FinanceRecord> {
    return apiFetch<FinanceRecord>('/api/finance/create', {
      method: 'POST',
      body: JSON.stringify(data)
    })
  },

  async update(id: string | number, data: FinancePayload): Promise<FinanceRecord> {
    return apiFetch<FinanceRecord>(`/api/finance/update/${id}`, {
      method: 'PATCH',
      body: JSON.stringify(data)
    })
  },

  async delete(id: string | number): Promise<void> {
    return apiFetch<void>(`/api/finance/delete/${id}`, {
      method: 'DELETE'
    })
  },

  async getForecast(year?: number): Promise<ForecastData> {
    const params = year ? `?year=${year}` : ''
    return apiFetch<ForecastData>(`/api/finance/forecast${params}`)
  }
}
