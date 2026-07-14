import { apiFetch } from '@/lib/api'

export type FinanceStatus = 'PENDING' | 'PAY'
export type FinanceType = 'FIXED_EXPENSE' | 'VARIABLE_EXPENSE' | 'FIXED_REVENUE' | 'VARIABLE_REVENUE'

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

// Espelha FinanceResponseDTO do backend
export type FinanceRecord = {
  id?: string | number
  clientId: number
  description: string
  value: number
  status: FinanceStatus | string
  expirationDate: string
  paymentDate?: string | null
  type?: FinanceType | string | null
}

export type FinanceFilters = {
  status?: FinanceStatus
  from?: string // yyyy-MM-dd
  to?: string // yyyy-MM-dd
}

// Espelha FinanceRequestDTO do backend: clientId plano, datas como
// YYYY-MM-DD (LocalDate) e sem campo user (vem da autenticação)
export type FinancePayload = {
  clientId: number
  description: string
  value: number
  status: FinanceStatus | string
  expirationDate: string
  paymentDate?: string | null
  type?: FinanceType | string | null
}

export const financeService = {
  /**
   * Lista registros; filtros são aplicados no backend quando informados.
   * Hoje a tela filtra localmente (volume pequeno) — quando crescer, basta
   * passar os filtros aqui e refazer o fetch a cada mudança.
   */
  async getAll(filters?: FinanceFilters): Promise<FinanceRecord[]> {
    const params = new URLSearchParams()
    if (filters?.status) params.set('status', filters.status)
    if (filters?.from) params.set('from', filters.from)
    if (filters?.to) params.set('to', filters.to)
    const query = params.size > 0 ? `?${params}` : ''
    return apiFetch<FinanceRecord[]>(`/api/finance${query}`)
  },

  // FILTRO PELO ID DO CLIENTE (FK) - Conforme solicitado!
  async getByClientId(clientId: string | number): Promise<FinanceRecord[]> {
    return apiFetch<FinanceRecord[]>(`/api/finance/client/${clientId}`)
  },

  async create(data: FinancePayload): Promise<FinanceRecord> {
    return apiFetch<FinanceRecord>('/api/finance', {
      method: 'POST',
      body: JSON.stringify(data)
    })
  },

  async update(id: string | number, data: FinancePayload): Promise<FinanceRecord> {
    return apiFetch<FinanceRecord>(`/api/finance/${id}`, {
      method: 'PUT',
      body: JSON.stringify(data)
    })
  },

  async delete(id: string | number): Promise<void> {
    return apiFetch<void>(`/api/finance/${id}`, {
      method: 'DELETE'
    })
  },

  async getForecast(year?: number): Promise<ForecastData> {
    const params = year ? `?year=${year}` : ''
    return apiFetch<ForecastData>(`/api/finance/forecast${params}`)
  },

  async getByType(type: FinanceType): Promise<FinanceRecord[]> {
    return apiFetch<FinanceRecord[]>(`/api/finance/type/${type}`)
  }
}
