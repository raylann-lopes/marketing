import { apiFetch } from '@/lib/api'

export type TaskType = 'TAREFA' | 'REUNIAO' | 'LEMBRETE' | 'COBRANCA' | 'PRAZO' | 'FOLLOW_UP'
export type TaskPriority = 'LOW' | 'NORMAL' | 'HIGH' | 'URGENT'
export type TaskStatus = 'PENDING' | 'DONE' | 'CANCELED'
export type TaskSource = 'MANUAL' | 'WHATSAPP'

export type TaskRecord = {
  id: number
  clientId?: number | null
  clientName?: string | null
  title: string
  description?: string | null
  dateExpires: string
  timeExpires?: string | null
  type: TaskType
  priority: TaskPriority
  status: TaskStatus
  source: TaskSource
  createdAt?: string
  updatedAt?: string
}

export type TaskPayload = {
  clientId?: number | null
  clientName?: string | null
  title: string
  description?: string | null
  dateExpires: string
  timeExpires?: string | null
  type?: TaskType | null
  priority?: TaskPriority | null
}

export type TaskStatusPayload = {
  status: TaskStatus
}

export type PageResponse<T> = {
  content: T[]
  totalElements: number
  totalPages: number
  size: number
  number: number
  first: boolean
  last: boolean
}

export type TaskFilters = {
  status?: TaskStatus
  date?: string
  startDate?: string
  endDate?: string
}

export const taskService = {
  async getMine(page = 0, size = 20, filters: TaskFilters = {}): Promise<PageResponse<TaskRecord>> {
    const params = new URLSearchParams({ page: String(page), size: String(size) })
    if (filters.status) params.set('status', filters.status)
    if (filters.date) params.set('date', filters.date)
    if (filters.startDate) params.set('startDate', filters.startDate)
    if (filters.endDate) params.set('endDate', filters.endDate)

    return apiFetch<PageResponse<TaskRecord>>(`/api/task?${params.toString()}`)
  },

  async create(data: TaskPayload): Promise<TaskRecord> {
    return apiFetch<TaskRecord>('/api/task/create', {
      method: 'POST',
      body: JSON.stringify(data),
    })
  },

  async updateStatus(taskId: number, data: TaskStatusPayload): Promise<TaskRecord> {
    return apiFetch<TaskRecord>(`/api/task/update/status/${taskId}`, {
      method: 'PATCH',
      body: JSON.stringify(data),
    })
  },

  async remove(taskId: number): Promise<void> {
    return apiFetch<void>(`/api/task/delete/${taskId}`, {
      method: 'DELETE',
    })
  },
}
