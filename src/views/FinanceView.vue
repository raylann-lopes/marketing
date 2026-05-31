<script setup lang="ts">
import { ref, onMounted, onUnmounted, computed } from 'vue'
import { Plus, CheckCircle, Pencil, Trash2, ChevronLeft, ChevronRight, TrendingUp, ChevronDown } from 'lucide-vue-next'
import AppLayout from '@/components/layout/AppLayout.vue'
import Card from '@/components/ui/Card.vue'
import Badge from '@/components/ui/Badge.vue'
import Button from '@/components/ui/Button.vue'

import FinanceStats from '@/components/finance/FinanceStats.vue'
import FinanceModal from '@/components/finance/FinanceModal.vue'

import { financeService, type FinancePayload, type FinanceRecord, type ForecastData, type FinanceType } from '@/services/financeService'
import { clientService, type Client } from '@/services/clientService'
import { getCurrentUserId } from '@/lib/api'
import { getErrorMessage } from '@/lib/errors'
import { useFeedback } from '@/lib/feedback'
import { z } from 'zod'

const loading = ref(false)
const error = ref('')
const transactions = ref<FinanceRecord[]>([])
const clients = ref<Client[]>([])
const incomeTotal = ref(0)
const expenseTotal = ref(0)
const netProfit = ref(0)
const search = ref('')
const feedback = useFeedback()

const periodDropdownRef = ref<HTMLElement | null>(null)
const typeDropdownRef = ref<HTMLElement | null>(null)
const showPeriodDropdown = ref(false)
const showTypeDropdown = ref(false)

const periodOptions = [
  { key: 'open',   label: 'Em Aberto' },
  { key: 'future', label: 'Futuras' },
  { key: 'all',    label: 'Todas' },
] as const

const typeOptions = [
  { key: 'all',              label: 'Todos os Tipos' },
  { key: 'FIXED_REVENUE',    label: 'Receita Fixa' },
  { key: 'VARIABLE_REVENUE', label: 'Receita Variável' },
  { key: 'FIXED_EXPENSE',    label: 'Despesa Fixa' },
  { key: 'VARIABLE_EXPENSE', label: 'Despesa Variável' },
] as const

function handleClickOutside(event: MouseEvent) {
  if (periodDropdownRef.value && !periodDropdownRef.value.contains(event.target as Node)) {
    showPeriodDropdown.value = false
  }
  if (typeDropdownRef.value && !typeDropdownRef.value.contains(event.target as Node)) {
    showTypeDropdown.value = false
  }
}

const activeTab = ref<'transactions' | 'forecast'>('transactions')
const forecast = ref<ForecastData | null>(null)
const loadingForecast = ref(false)
const forecastError = ref('')
const forecastYear = ref(new Date().getFullYear())
const transactionFilter = ref<'open' | 'future' | 'all'>('open')
const typeFilter = ref<FinanceType | 'all'>('all')

const currentPage = ref(1)
const itemsPerPage = 10

const baseTransactions = computed(() => {
  const now = new Date()
  const startOfCurrentMonth = new Date(now.getFullYear(), now.getMonth(), 1)
  const endOfCurrentMonth = new Date(now.getFullYear(), now.getMonth() + 1, 0, 23, 59, 59)

  return transactions.value.filter(t => {
    if (t.status === 'PAY') return false
    const exp = new Date(t.expirationDate)
    const periodOk = transactionFilter.value === 'open'
      ? exp <= endOfCurrentMonth
      : transactionFilter.value === 'future'
        ? exp > endOfCurrentMonth
        : true
    const typeOk = typeFilter.value === 'all' || t.type === typeFilter.value
    return periodOk && typeOk
  }).sort((a, b) => {
    // Vencidas primeiro, depois por data
    const expA = new Date(a.expirationDate)
    const expB = new Date(b.expirationDate)
    const aOverdue = expA < startOfCurrentMonth
    const bOverdue = expB < startOfCurrentMonth
    if (aOverdue !== bOverdue) return aOverdue ? -1 : 1
    return expA.getTime() - expB.getTime()
  })
})

const filteredTransactions = computed(() => {
  const base = baseTransactions.value
  if (!search.value) return base
  const term = search.value.toLowerCase()
  return base.filter(t => {
    const descriptionMatch = t.description.toLowerCase().includes(term)
    const tAny = t as Record<string, unknown>
    const clientRaw = tAny.client
    const clientId = typeof clientRaw === 'object' && clientRaw !== null
      ? (clientRaw as Record<string, unknown>)?.['id'] as string | number | undefined
      : String(t.client) as string | number | undefined
    const clientName = getClientName(clientId).toLowerCase()
    return descriptionMatch || clientName.includes(term)
  })
})

const totalPages = computed(() => Math.ceil(filteredTransactions.value.length / itemsPerPage))

const paginatedTransactions = computed(() => {
  const start = (currentPage.value - 1) * itemsPerPage
  const end = start + itemsPerPage
  return filteredTransactions.value.slice(start, end)
})

function nextPage() {
  if (currentPage.value < totalPages.value) currentPage.value++
}

function prevPage() {
  if (currentPage.value > 1) currentPage.value--
}

const sortedClients = computed(() => {
  return [...clients.value].sort((a, b) => a.name.localeCompare(b.name))
})

function getClientName(clientId: string | number | undefined) {
  if (!clientId) return '—'
  const client = clients.value.find(c => String(c.id) === String(clientId))
  return client ? client.name : `ID: ${clientId}`
}

function getClientId(record: FinanceRecord): number {
  const raw = (record as { client: { id?: string | number } | string | number }).client
  if (typeof raw === 'object' && raw !== null && raw.id != null) {
    return Number(raw.id)
  }
  return Number(raw)
}

const isModalOpen = ref(false)
const isEditModalOpen = ref(false)
const transactionToEdit = ref<FinanceRecord | null>(null)
const isSubmitting = ref(false)
const fieldErrors = ref<Record<string, string>>({})
const editFieldErrors = ref<Record<string, string>>({})

const newTransaction = ref({
  client: '',
  description: '',
  value: 0,
  status: 'PENDING',
  expirationDate: new Date().toISOString().split('T')[0] || '',
  type: '' as FinanceType | ''
})

const editTransaction = ref({
  client: '',
  description: '',
  value: 0,
  status: 'PENDING',
  expirationDate: new Date().toISOString().split('T')[0] || '',
  type: '' as FinanceType | ''
})

const financeSchema = z.object({
  client: z.union([z.string(), z.number()]).refine(v => String(v).length > 0, 'Selecione um cliente'),
  description: z.string().min(3, 'Descrição muito curta'),
  value: z.number().refine(v => v !== 0, 'O valor não pode ser zero'),
  expirationDate: z.string().min(10, 'Data inválida')
})

function openFinanceModal() {
  newTransaction.value = {
    client: '',
    description: '',
    value: 0,
    status: 'PENDING',
    expirationDate: new Date().toISOString().split('T')[0] || '',
    type: ''
  }
  fieldErrors.value = {}
  isModalOpen.value = true
}

function openEditTransactionModal(t: FinanceRecord) {
  transactionToEdit.value = t
  const rawClientId = (t as unknown as { client: { id: number } }).client?.id ?? t.client
  editTransaction.value = {
    client: String(rawClientId),
    description: t.description,
    value: t.value,
    status: t.status,
    expirationDate: t.expirationDate?.split('T')[0] ?? new Date().toISOString().split('T')[0] ?? '',
    type: (t.type as FinanceType | '') ?? ''
  }
  editFieldErrors.value = {}
  isEditModalOpen.value = true
}

async function fetchTransactions() {
  loading.value = true
  error.value = ''
  try {
    const [financeData, clientsData] = await Promise.all([
      financeService.getAll(),
      clientService.getAll()
    ])
    
    interface ApiResponse<T> { data?: T[] }
    const list = Array.isArray(financeData) ? financeData : (((financeData as unknown) as ApiResponse<FinanceRecord>).data || [])
    transactions.value = list
    clients.value = Array.isArray(clientsData) ? clientsData : (((clientsData as unknown) as ApiResponse<Client>).data || [])

    incomeTotal.value = list.filter((t: FinanceRecord) => t.value > 0).reduce((acc: number, t: FinanceRecord) => acc + t.value, 0)
    expenseTotal.value = Math.abs(list.filter((t: FinanceRecord) => t.value < 0).reduce((acc: number, t: FinanceRecord) => acc + t.value, 0))
    netProfit.value = incomeTotal.value - expenseTotal.value
  } catch (e: unknown) {
    error.value = `Erro ao carregar dados: ${getErrorMessage(e)}`
  } finally {
    loading.value = false
  }
}

async function handleCreateTransaction() {
  fieldErrors.value = {}
  const result = financeSchema.safeParse(newTransaction.value)
  
  if (!result.success) {
    result.error.issues.forEach(issue => {
      const key = issue.path[0] as string
      fieldErrors.value[key] = issue.message
    })
    return
  }

  isSubmitting.value = true
  try {
    const payload = {
      client: { id: Number(newTransaction.value.client) },
      user: { id: getCurrentUserId() },
      description: newTransaction.value.description,
      value: newTransaction.value.value,
      status: newTransaction.value.status,
      expirationDate: newTransaction.value.expirationDate + "T00:00:00",
      paymentDate: newTransaction.value.expirationDate + "T00:00:00",
      type: newTransaction.value.type || null
    }
    await financeService.create(payload as FinancePayload)
    await fetchTransactions()
    isModalOpen.value = false
    feedback.success('Transação registrada com sucesso.')
  } catch (e: unknown) {
    feedback.error(`Erro ao salvar: ${getErrorMessage(e)}`)
  } finally {
    isSubmitting.value = false
  }
}

async function handleEditTransaction() {
  editFieldErrors.value = {}
  const result = financeSchema.safeParse(editTransaction.value)

  if (!result.success) {
    result.error.issues.forEach(issue => {
      const key = issue.path[0] as string
      editFieldErrors.value[key] = issue.message
    })
    return
  }

  isSubmitting.value = true
  try {
    const payload = {
      id: transactionToEdit.value!.id,
      client: { id: Number(editTransaction.value.client) },
      user: { id: getCurrentUserId() },
      description: editTransaction.value.description,
      value: editTransaction.value.value,
      status: editTransaction.value.status,
      expirationDate: editTransaction.value.expirationDate + "T00:00:00",
      paymentDate: editTransaction.value.expirationDate + "T00:00:00",
      type: editTransaction.value.type || null
    }
    await financeService.update(transactionToEdit.value!.id!, payload as FinancePayload)
    await fetchTransactions()
    isEditModalOpen.value = false
    feedback.success('Transação atualizada com sucesso.')
  } catch (e: unknown) {
    feedback.error(`Erro ao atualizar: ${getErrorMessage(e)}`)
  } finally {
    isSubmitting.value = false
  }
}

async function handleMarkAsPaid(transaction: FinanceRecord) {
  const confirmed = await feedback.confirm({
    title: 'Confirmar pagamento',
    message: 'Deseja marcar esta conta como paga?',
    confirmText: 'Marcar como paga',
  })
  if (!confirmed) return

  try {
    const payload = {
      ...transaction,
      status: 'PAY',
      client: { id: getClientId(transaction) },
      user: { id: getCurrentUserId() }
    }
    await financeService.update(transaction.id!, payload as FinancePayload)
    await fetchTransactions()
    feedback.success('Conta marcada como paga.')
  } catch (e: unknown) {
    feedback.error(`Erro ao atualizar: ${getErrorMessage(e)}`)
  }
}

async function handleDelete(id: string | number) {
  const confirmed = await feedback.confirm({
    title: 'Excluir registro',
    message: 'Tem certeza que deseja excluir este registro? Esta ação não pode ser desfeita.',
    confirmText: 'Excluir',
    tone: 'danger',
  })
  if (!confirmed) return

  try {
    await financeService.delete(id)
    await fetchTransactions()
    feedback.success('Registro excluído com sucesso.')
  } catch (e: unknown) {
    feedback.error(`Erro ao excluir: ${getErrorMessage(e)}`)
  }
}

async function fetchForecast(year?: number) {
  loadingForecast.value = true
  forecastError.value = ''
  try {
    forecast.value = await financeService.getForecast(year ?? forecastYear.value)
  } catch (e: unknown) {
    forecastError.value = `Erro ao carregar previsão: ${getErrorMessage(e)}`
  } finally {
    loadingForecast.value = false
  }
}

function switchTab(tab: 'transactions' | 'forecast') {
  activeTab.value = tab
  if (tab === 'forecast' && !forecast.value) {
    fetchForecast()
  }
}

async function changeYear(delta: number) {
  forecastYear.value += delta
  await fetchForecast(forecastYear.value)
}

function cellClass(monthKey: string, client: import('@/services/financeService').ForecastClient): string {
  const m = client.monthData?.[monthKey]
  if (!m) return 'bg-gray-50 text-gray-300'
  if (m.received > 0 && m.pending === 0) return 'bg-green-50 text-green-700 font-semibold'
  if (m.pending > 0) return 'bg-yellow-50 text-yellow-700 font-semibold'
  return 'text-gray-400'
}

function cellValue(monthKey: string, client: import('@/services/financeService').ForecastClient): string {
  const m = client.monthData?.[monthKey]
  if (!m) return '—'
  const val = m.received > 0 ? m.received : m.pending > 0 ? m.pending : m.expected
  return formatCurrencyCompact(val)
}

function formatCurrencyCompact(value: number): string {
  return new Intl.NumberFormat('pt-BR', { style: 'currency', currency: 'BRL', maximumFractionDigits: 0 }).format(value)
}

function clientTotal(client: import('@/services/financeService').ForecastClient): number {
  if (!client.monthData) return 0
  return Object.values(client.monthData).reduce((sum, m) => {
    return sum + (m.received > 0 ? m.received : m.pending > 0 ? m.pending : m.expected)
  }, 0)
}

function monthColTotal(monthKey: string): number {
  if (!forecast.value) return 0
  return forecast.value.clients.reduce((sum, c) => {
    const m = c.monthData?.[monthKey]
    if (!m) return sum
    return sum + (m.received > 0 ? m.received : m.pending > 0 ? m.pending : m.expected)
  }, 0)
}

const grandTotal = computed(() => {
  if (!forecast.value) return 0
  return forecast.value.clients.reduce((sum, c) => sum + clientTotal(c), 0)
})

onMounted(() => {
  fetchTransactions()
  document.addEventListener('click', handleClickOutside)
})

onUnmounted(() => {
  document.removeEventListener('click', handleClickOutside)
})

function formatCurrency(value: number): string {
  return new Intl.NumberFormat('pt-BR', { style: 'currency', currency: 'BRL' }).format(value)
}

function capitalizeFirst(str: string): string {
  if (!str) return str
  return str.charAt(0).toUpperCase() + str.slice(1)
}

function formatDate(dateStr: string) {
  if (!dateStr) return '-'
  const d = new Date(dateStr)
  return d.toLocaleDateString('pt-BR', { day: '2-digit', month: 'short' })
}

const TYPE_LABELS: Record<string, { label: string; class: string }> = {
  FIXED_REVENUE:    { label: 'Receita Fixa',     class: 'bg-green-100 text-green-700' },
  VARIABLE_REVENUE: { label: 'Receita Variável', class: 'bg-emerald-100 text-emerald-700' },
  FIXED_EXPENSE:    { label: 'Despesa Fixa',     class: 'bg-red-100 text-red-700' },
  VARIABLE_EXPENSE: { label: 'Despesa Variável', class: 'bg-orange-100 text-orange-700' },
}

function getTypeLabel(type?: string | null) {
  return type ? TYPE_LABELS[type] : null
}

function isOverdue(t: FinanceRecord): boolean {
  const now = new Date()
  const startOfCurrentMonth = new Date(now.getFullYear(), now.getMonth(), 1)
  const exp = new Date(t.expirationDate)
  return exp < startOfCurrentMonth
}
</script>

<template>
  <AppLayout v-model:search="search" topbar-placeholder="Buscar transações por descrição ou cliente...">
    <div class="flex items-start justify-between mb-6">
      <div>
        <h1 class="text-3xl font-bold text-gray-900">Financeiro</h1>
        <p class="text-gray-500 mt-1">Controle as receitas e despesas do ateliê.</p>
      </div>
      <Button v-if="activeTab === 'transactions'" class="gap-2" @click="openFinanceModal">
        <Plus class="w-4 h-4" />
        Nova Transação
      </Button>
    </div>

    <FinanceStats
      :income-total="formatCurrency(incomeTotal)"
      :expense-total="formatCurrency(expenseTotal)"
      :net-profit="formatCurrency(netProfit)"
    />

    <!-- Tabs -->
    <div class="flex gap-1 mb-4 border-b border-gray-100">
      <button
        :class="['px-4 py-2 text-sm font-semibold transition-colors border-b-2', activeTab === 'transactions' ? 'border-primary text-primary' : 'border-transparent text-gray-500 hover:text-gray-700']"
        @click="switchTab('transactions')"
      >
        Transações
      </button>
      <button
        :class="['px-4 py-2 text-sm font-semibold transition-colors border-b-2 flex items-center gap-1.5', activeTab === 'forecast' ? 'border-primary text-primary' : 'border-transparent text-gray-500 hover:text-gray-700']"
        @click="switchTab('forecast')"
      >
        <TrendingUp class="w-4 h-4" />
        Previsibilidade
      </button>
    </div>

    <!-- Forecast / Previsibilidade Tab -->
    <div v-if="activeTab === 'forecast'">
      <div v-if="loadingForecast" class="flex items-center justify-center py-16">
        <div class="animate-spin rounded-full h-8 w-8 border-b-2 border-primary"></div>
      </div>
      <p v-else-if="forecastError" class="text-red-500 text-sm py-4">{{ forecastError }}</p>
      <div v-else-if="forecast">

        <!-- Header: year nav + summary cards -->
        <div class="flex flex-col sm:flex-row sm:items-center gap-4 mb-6">
          <div class="flex items-center gap-2">
            <button
              class="p-1.5 rounded-lg border border-gray-200 bg-white text-gray-500 hover:text-primary hover:border-primary/30 transition-all"
              @click="changeYear(-1)"
            >
              <ChevronLeft class="w-4 h-4" />
            </button>
            <span class="text-lg font-bold text-gray-900 w-16 text-center">{{ forecastYear }}</span>
            <button
              class="p-1.5 rounded-lg border border-gray-200 bg-white text-gray-500 hover:text-primary hover:border-primary/30 transition-all"
              @click="changeYear(1)"
            >
              <ChevronRight class="w-4 h-4" />
            </button>
          </div>
          <div class="flex gap-3 flex-1">
            <Card class="p-4 flex-1">
              <p class="text-xs font-semibold text-gray-400 uppercase tracking-wide mb-0.5">Receita Mensal</p>
              <p class="text-xl font-bold text-gray-900">{{ formatCurrency(forecast.monthlyTotal) }}</p>
            </Card>
            <Card class="p-4 flex-1">
              <p class="text-xs font-semibold text-gray-400 uppercase tracking-wide mb-0.5">Receita Anual Prevista</p>
              <p class="text-xl font-bold text-green-600">{{ formatCurrency(forecast.annualTotal) }}</p>
            </Card>
            <Card class="p-4 flex-1">
              <p class="text-xs font-semibold text-gray-400 uppercase tracking-wide mb-0.5">Clientes Ativos</p>
              <p class="text-xl font-bold text-primary">{{ forecast.clients.length }}</p>
            </Card>
          </div>
        </div>

        <!-- Legend -->
        <div class="flex items-center gap-4 mb-3 text-xs text-gray-500">
          <span class="flex items-center gap-1.5">
            <span class="w-3 h-3 rounded-sm bg-green-100 border border-green-300 inline-block"></span> Pago
          </span>
          <span class="flex items-center gap-1.5">
            <span class="w-3 h-3 rounded-sm bg-yellow-100 border border-yellow-300 inline-block"></span> Pendente
          </span>
          <span class="flex items-center gap-1.5">
            <span class="w-3 h-3 rounded-sm bg-gray-100 border border-gray-200 inline-block"></span> Previsto
          </span>
        </div>

        <!-- Spreadsheet table -->
        <Card class="overflow-hidden">
          <div class="overflow-x-auto">
            <table class="w-full text-xs border-collapse min-w-[900px]">
              <thead>
                <tr class="bg-[#EEF2FF]">
                  <th class="text-left px-3 py-2.5 font-semibold text-gray-600 whitespace-nowrap border-b border-indigo-100 sticky left-0 bg-[#EEF2FF] z-10 min-w-[160px]">Cliente</th>
                  <th class="text-left px-3 py-2.5 font-semibold text-gray-600 whitespace-nowrap border-b border-indigo-100 min-w-[110px]">Segmento</th>
                  <th
                    v-for="m in forecast.months"
                    :key="m.monthKey"
                    class="text-center px-2 py-2.5 font-semibold text-gray-600 whitespace-nowrap border-b border-indigo-100 min-w-[72px]"
                  >
                    {{ capitalizeFirst(m.monthLabel) }}
                  </th>
                  <th class="text-right px-3 py-2.5 font-semibold text-gray-600 whitespace-nowrap border-b border-indigo-100 min-w-[90px] bg-indigo-50">Total</th>
                  <th class="text-right px-3 py-2.5 font-semibold text-gray-600 whitespace-nowrap border-b border-indigo-100 min-w-[80px] bg-indigo-50">Média</th>
                </tr>
              </thead>
              <tbody>
                <tr v-if="forecast.clients.length === 0">
                  <td :colspan="16" class="text-center py-10 text-gray-400 italic">Nenhum cliente ativo com valor mensal.</td>
                </tr>
                <tr
                  v-else
                  v-for="c in forecast.clients"
                  :key="c.clientId"
                  :class="['transition-colors border-b border-gray-100', c.status === 'INACTIVE' ? 'bg-gray-50/70 opacity-70' : 'hover:bg-indigo-50/30']"
                >
                  <td :class="['px-3 py-2 whitespace-nowrap sticky z-10', c.status === 'INACTIVE' ? 'left-0 bg-gray-50 text-gray-400 italic' : 'left-0 bg-white font-medium text-gray-800']">
                    {{ c.clientName }}
                    <span v-if="c.status === 'INACTIVE'" class="ml-1.5 text-[10px] font-semibold bg-gray-200 text-gray-500 px-1.5 py-0.5 rounded-full uppercase not-italic">inativo</span>
                  </td>
                  <td :class="['px-3 py-2 whitespace-nowrap', c.status === 'INACTIVE' ? 'text-gray-400 italic' : 'text-gray-500']">{{ c.niche }}</td>
                  <td
                    v-for="m in forecast.months"
                    :key="m.monthKey"
                    :class="['px-2 py-2 text-center rounded-sm', cellClass(m.monthKey, c)]"
                  >
                    {{ cellValue(m.monthKey, c) }}
                  </td>
                  <td class="px-3 py-2 text-right font-bold text-gray-800 bg-indigo-50/50">
                    {{ formatCurrencyCompact(clientTotal(c)) }}
                  </td>
                  <td class="px-3 py-2 text-right text-gray-600 bg-indigo-50/50">
                    {{ formatCurrencyCompact(clientTotal(c) / 12) }}
                  </td>
                </tr>
              </tbody>
              <tfoot>
                <tr class="bg-[#EEF2FF] font-bold border-t-2 border-indigo-200">
                  <td class="px-3 py-2.5 text-gray-700 uppercase text-xs sticky left-0 bg-[#EEF2FF] z-10" colspan="2">Total Receita de Clientes</td>
                  <td
                    v-for="m in forecast.months"
                    :key="m.monthKey"
                    class="px-2 py-2.5 text-center text-primary"
                  >
                    {{ formatCurrencyCompact(monthColTotal(m.monthKey)) }}
                  </td>
                  <td class="px-3 py-2.5 text-right text-primary">{{ formatCurrencyCompact(grandTotal) }}</td>
                  <td class="px-3 py-2.5 text-right text-primary">{{ formatCurrencyCompact(grandTotal / 12) }}</td>
                </tr>
              </tfoot>
            </table>
          </div>
        </Card>

      </div>
      <div v-else class="text-center py-12 text-gray-500 text-sm">Nenhum dado de previsão disponível.</div>
    </div>

    <!-- Transactions Tab -->
    <Card v-if="activeTab === 'transactions'" class="overflow-hidden">
      <div class="px-5 py-4 border-b border-gray-100 flex flex-wrap items-center justify-between gap-3">
        <div>
          <h2 class="font-semibold text-gray-900">Contas a Receber</h2>
          <p class="text-xs text-gray-400 mt-0.5">Mensalidades pendentes dos clientes ativos</p>
        </div>
        <div class="flex items-center gap-2">
          <!-- Filtro de período -->
          <div class="relative" ref="periodDropdownRef">
            <button
              class="flex items-center gap-1.5 px-3 py-1.5 text-xs font-semibold bg-gray-100 hover:bg-gray-200 rounded-lg transition-colors"
              @click.stop="showPeriodDropdown = !showPeriodDropdown; showTypeDropdown = false"
            >
              {{ periodOptions.find(o => o.key === transactionFilter)?.label }}
              <ChevronDown class="w-3 h-3" />
            </button>
            <div
              v-if="showPeriodDropdown"
              class="absolute right-0 top-full mt-1 bg-white border border-gray-200 rounded-lg shadow-lg z-20 min-w-[140px] py-1"
            >
              <button
                v-for="opt in periodOptions"
                :key="opt.key"
                :class="['w-full text-left px-4 py-2 text-xs font-semibold transition-colors', transactionFilter === opt.key ? 'text-primary bg-indigo-50' : 'text-gray-600 hover:bg-gray-50']"
                @click="transactionFilter = opt.key as 'open' | 'future' | 'all'; showPeriodDropdown = false; currentPage = 1"
              >
                {{ opt.label }}
              </button>
            </div>
          </div>

          <!-- Filtro de tipo -->
          <div class="relative" ref="typeDropdownRef">
            <button
              class="flex items-center gap-1.5 px-3 py-1.5 text-xs font-semibold bg-gray-100 hover:bg-gray-200 rounded-lg transition-colors"
              @click.stop="showTypeDropdown = !showTypeDropdown; showPeriodDropdown = false"
            >
              {{ typeOptions.find(o => o.key === typeFilter)?.label }}
              <ChevronDown class="w-3 h-3" />
            </button>
            <div
              v-if="showTypeDropdown"
              class="absolute right-0 top-full mt-1 bg-white border border-gray-200 rounded-lg shadow-lg z-20 min-w-[160px] py-1"
            >
              <button
                v-for="opt in typeOptions"
                :key="opt.key"
                :class="['w-full text-left px-4 py-2 text-xs font-semibold transition-colors', typeFilter === opt.key ? 'text-primary bg-indigo-50' : 'text-gray-600 hover:bg-gray-50']"
                @click="typeFilter = opt.key as FinanceType | 'all'; showTypeDropdown = false; currentPage = 1"
              >
                {{ opt.label }}
              </button>
            </div>
          </div>
        </div>
        <p v-if="error" class="text-xs text-red-500 font-medium">{{ error }}</p>
      </div>
      <table class="w-full">
        <thead>
          <tr class="border-b border-gray-100">
            <th class="text-left px-5 py-3 text-xs font-semibold text-gray-400 uppercase">Descrição</th>
            <th class="text-left px-5 py-3 text-xs font-semibold text-gray-400 uppercase">Cliente</th>
            <th class="text-left px-5 py-3 text-xs font-semibold text-gray-400 uppercase">Tipo</th>
            <th class="text-left px-5 py-3 text-xs font-semibold text-gray-400 uppercase">Vencimento</th>
            <th class="text-left px-5 py-3 text-xs font-semibold text-gray-400 uppercase">Status</th>
            <th class="text-right px-5 py-3 text-xs font-semibold text-gray-400 uppercase">Valor</th>
            <th class="text-center px-5 py-3 text-xs font-semibold text-gray-400 uppercase">Ações</th>
          </tr>
        </thead>
        <tbody>
          <tr v-if="loading"><td colspan="7" class="text-center py-8">
            <div class="animate-spin rounded-full h-6 w-6 border-b-2 border-primary mx-auto"></div>
          </td></tr>
          <tr v-else-if="paginatedTransactions.length === 0"><td colspan="7" class="text-center py-8 text-gray-500 text-sm italic">
            {{ transactionFilter === 'open' ? 'Nenhuma conta em aberto.' : transactionFilter === 'future' ? 'Nenhuma conta futura.' : 'Nenhuma transação encontrada.' }}
          </td></tr>
          <tr v-else v-for="t in paginatedTransactions" :key="t.id" :class="['border-b border-gray-50 transition-colors', isOverdue(t) ? 'bg-red-50/40 hover:bg-red-50/60' : 'hover:bg-gray-50']">
            <td class="px-5 py-4 text-sm font-medium text-gray-800">{{ t.description }}</td>
            <td class="px-5 py-4 text-sm text-gray-500">
              {{ getClientName((t as unknown as { client: { id: number } }).client?.id ?? t.client) }}
            </td>
            <td class="px-5 py-4">
              <span
                v-if="getTypeLabel(t.type)"
                :class="['text-[10px] font-semibold px-2 py-0.5 rounded-full', getTypeLabel(t.type)!.class]"
              >
                {{ getTypeLabel(t.type)!.label }}
              </span>
              <span v-else class="text-xs text-gray-300">—</span>
            </td>
            <td :class="['px-5 py-4 text-sm', isOverdue(t) ? 'text-red-600 font-semibold' : 'text-gray-500']">
              {{ formatDate(t.expirationDate) }}
            </td>
            <td class="px-5 py-4">
              <Badge :variant="isOverdue(t) ? 'destructive' : 'warning'">
                {{ isOverdue(t) ? 'VENCIDA' : 'PENDENTE' }}
              </Badge>
            </td>
            <td :class="['px-5 py-4 text-sm font-semibold text-right', t.value >= 0 ? 'text-green-600' : 'text-red-500']">
              {{ t.value >= 0 ? '+' : '' }}{{ formatCurrency(t.value) }}
            </td>
            <td class="px-5 py-4">
              <div class="flex items-center justify-center gap-2">
                <button
                  class="p-1.5 hover:bg-green-50 rounded-lg text-green-600 transition-colors"
                  title="Receber"
                  @click="handleMarkAsPaid(t)"
                >
                  <CheckCircle class="w-4 h-4" />
                </button>
                <button
                  class="p-1.5 hover:bg-blue-50 rounded-lg text-blue-600 transition-colors"
                  title="Editar"
                  @click="openEditTransactionModal(t)"
                >
                  <Pencil class="w-4 h-4" />
                </button>
                <button
                  class="p-1.5 hover:bg-red-50 rounded-lg text-red-500 transition-colors"
                  title="Excluir"
                  @click="handleDelete(t.id!)"
                >
                  <Trash2 class="w-4 h-4" />
                </button>
              </div>
            </td>
          </tr>
        </tbody>
      </table>
      <div v-if="filteredTransactions.length > itemsPerPage" class="p-4 border-t border-gray-100 flex items-center justify-between bg-gray-50/50">
        <p class="text-xs text-gray-500">
          Mostrando <span class="font-semibold">{{ (currentPage - 1) * itemsPerPage + 1 }}</span> a 
          <span class="font-semibold">{{ Math.min(currentPage * itemsPerPage, filteredTransactions.length) }}</span> de 
          <span class="font-semibold">{{ filteredTransactions.length }}</span> transações
        </p>
        <div class="flex items-center gap-2">
          <button 
            @click="prevPage" 
            :disabled="currentPage === 1"
            class="p-1 rounded-lg border border-gray-200 bg-white text-gray-400 hover:text-primary hover:border-primary/30 disabled:opacity-50 disabled:cursor-not-allowed transition-all"
          >
            <ChevronLeft class="w-4 h-4" />
          </button>
          <span class="text-xs font-bold text-gray-700 mx-2">Página {{ currentPage }} de {{ totalPages }}</span>
          <button 
            @click="nextPage" 
            :disabled="currentPage === totalPages"
            class="p-1 rounded-lg border border-gray-200 bg-white text-gray-400 hover:text-primary hover:border-primary/30 disabled:opacity-50 disabled:cursor-not-allowed transition-all"
          >
            <ChevronRight class="w-4 h-4" />
          </button>
        </div>
      </div>
    </Card>

    <FinanceModal
      :is-open="isModalOpen"
      :is-edit="false"
      v-model:transaction="newTransaction"
      :clients="sortedClients"
      :is-submitting="isSubmitting"
      :field-errors="fieldErrors"
      @close="isModalOpen = false"
      @save="handleCreateTransaction"
    />

    <FinanceModal
      :is-open="isEditModalOpen"
      :is-edit="true"
      v-model:transaction="editTransaction"
      :clients="sortedClients"
      :is-submitting="isSubmitting"
      :field-errors="editFieldErrors"
      @close="isEditModalOpen = false"
      @save="handleEditTransaction"
    />
  </AppLayout>
</template>
