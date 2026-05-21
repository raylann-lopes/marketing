<script setup lang="ts">
import { ref, onMounted, computed } from 'vue'
import { Plus, CheckCircle, Pencil, Trash2, ChevronLeft, ChevronRight } from 'lucide-vue-next'
import AppLayout from '@/components/layout/AppLayout.vue'
import Card from '@/components/ui/Card.vue'
import Badge from '@/components/ui/Badge.vue'
import Button from '@/components/ui/Button.vue'

import FinanceStats from '@/components/finance/FinanceStats.vue'
import FinanceModal from '@/components/finance/FinanceModal.vue'

import { financeService, type FinancePayload, type FinanceRecord } from '@/services/financeService'
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

const currentPage = ref(1)
const itemsPerPage = 10

const filteredTransactions = computed(() => {
  if (!search.value) return transactions.value
  const term = search.value.toLowerCase()
  return transactions.value.filter(t => {
    const descriptionMatch = t.description.toLowerCase().includes(term)

    const tAny = t as Record<string, unknown>
    const clientRaw = tAny.client
    const clientId = typeof clientRaw === 'object' && clientRaw !== null
      ? (clientRaw as Record<string, unknown>)?.['id'] as string | number | undefined
      : String(t.client) as string | number | undefined
    const clientName = getClientName(clientId).toLowerCase()
    const clientMatch = clientName.includes(term)
    return descriptionMatch || clientMatch
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
  expirationDate: new Date().toISOString().split('T')[0] || ''
})

const editTransaction = ref({
  client: '',
  description: '',
  value: 0,
  status: 'PENDING',
  expirationDate: new Date().toISOString().split('T')[0] || ''
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
    expirationDate: new Date().toISOString().split('T')[0] || ''
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
    expirationDate: t.expirationDate?.split('T')[0] ?? new Date().toISOString().split('T')[0] ?? ''
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
      paymentDate: newTransaction.value.expirationDate + "T00:00:00"
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
      paymentDate: editTransaction.value.expirationDate + "T00:00:00"
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

onMounted(() => {
  fetchTransactions()
})

function formatCurrency(value: number): string {
  return new Intl.NumberFormat('pt-BR', { style: 'currency', currency: 'BRL' }).format(value)
}

function formatDate(dateStr: string) {
  if (!dateStr) return '-'
  const d = new Date(dateStr)
  return d.toLocaleDateString('pt-BR', { day: '2-digit', month: 'short' })
}
</script>

<template>
  <AppLayout v-model:search="search" topbar-placeholder="Buscar transações por descrição ou cliente...">
    <div class="flex items-start justify-between mb-6">
      <div>
        <h1 class="text-3xl font-bold text-gray-900">Financeiro</h1>
        <p class="text-gray-500 mt-1">Controle as receitas e despesas do ateliê.</p>
      </div>
      <Button class="gap-2" @click="openFinanceModal">
        <Plus class="w-4 h-4" />
        Nova Transação
      </Button>
    </div>

    <FinanceStats
      :income-total="formatCurrency(incomeTotal)"
      :expense-total="formatCurrency(expenseTotal)"
      :net-profit="formatCurrency(netProfit)"
    />

    <Card class="overflow-hidden">
      <div class="px-5 py-4 border-b border-gray-100 flex items-center justify-between">
        <h2 class="font-semibold text-gray-900">Transações Recentes</h2>
        <p v-if="error" class="text-xs text-red-500 font-medium">{{ error }}</p>
      </div>
      <table class="w-full">
        <thead>
          <tr class="border-b border-gray-100">
            <th class="text-left px-5 py-3 text-xs font-semibold text-gray-400 uppercase">Descrição</th>
            <th class="text-left px-5 py-3 text-xs font-semibold text-gray-400 uppercase">Cliente</th>
            <th class="text-left px-5 py-3 text-xs font-semibold text-gray-400 uppercase">Vencimento</th>
            <th class="text-left px-5 py-3 text-xs font-semibold text-gray-400 uppercase">Status</th>
            <th class="text-right px-5 py-3 text-xs font-semibold text-gray-400 uppercase">Valor</th>
            <th class="text-center px-5 py-3 text-xs font-semibold text-gray-400 uppercase">Ações</th>
          </tr>
        </thead>
        <tbody>
          <tr v-if="loading"><td colspan="6" class="text-center py-8">
            <div class="animate-spin rounded-full h-6 w-6 border-b-2 border-primary mx-auto"></div>
          </td></tr>
          <tr v-else-if="paginatedTransactions.length === 0"><td colspan="6" class="text-center py-8 text-gray-500 text-sm italic">Nenhuma transação encontrada.</td></tr>
          <tr v-else v-for="t in paginatedTransactions" :key="t.id" class="border-b border-gray-50 hover:bg-gray-50 transition-colors">
            <td class="px-5 py-4 text-sm font-medium text-gray-800">{{ t.description }}</td>
            <td class="px-5 py-4 text-sm text-gray-500">
              {{ getClientName((t as unknown as { client: { id: number } }).client?.id ?? t.client) }}
            </td>
            <td class="px-5 py-4 text-sm text-gray-500">{{ formatDate(t.expirationDate) }}</td>
            <td class="px-5 py-4">
              <Badge :variant="t.status === 'PAY' ? 'success' : 'warning'">
                {{ t.status === 'PAY' ? 'PAGO' : 'PENDENTE' }}
              </Badge>
            </td>
            <td :class="['px-5 py-4 text-sm font-semibold text-right', t.value >= 0 ? 'text-green-600' : 'text-red-500']">
              {{ t.value >= 0 ? '+' : '' }}{{ formatCurrency(t.value) }}
            </td>
            <td class="px-5 py-4">
              <div class="flex items-center justify-center gap-2">
                <button 
                  v-if="t.status !== 'PAY'"
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
