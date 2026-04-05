<script setup lang="ts">
import { ref, onMounted, computed } from 'vue'
import { TrendingUp, TrendingDown, DollarSign, Plus, CheckCircle, Pencil, Trash2, X, ChevronDown, ChevronLeft, ChevronRight } from 'lucide-vue-next'
import AppLayout from '@/components/layout/AppLayout.vue'
import Card from '@/components/ui/Card.vue'
import Badge from '@/components/ui/Badge.vue'
import Button from '@/components/ui/Button.vue'
import { financeService, type FinanceRecord } from '@/services/financeService'
import { clientService, type Client } from '@/services/clientService'
import { getCurrentUserId } from '@/lib/api'
import { z } from 'zod'

const loading = ref(false)
const error = ref('')
const transactions = ref<FinanceRecord[]>([])
const clients = ref<Client[]>([])
const incomeTotal = ref(0)
const expenseTotal = ref(0)
const netProfit = ref(0)
const search = ref('')

// Pagination state
const currentPage = ref(1)
const itemsPerPage = 10

// Filtered transactions based on search
const filteredTransactions = computed(() => {
  if (!search.value) return transactions.value
  const term = search.value.toLowerCase()
  return transactions.value.filter(t => {
    const descriptionMatch = t.description.toLowerCase().includes(term)

    // Get client name for this transaction
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
  expirationDate: new Date().toISOString().split('T')[0]
})

const editTransaction = ref({
  client: '',
  description: '',
  value: 0,
  status: 'PENDING',
  expirationDate: new Date().toISOString().split('T')[0]
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
    expirationDate: new Date().toISOString().split('T')[0]
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
    expirationDate: t.expirationDate?.split('T')[0] ?? new Date().toISOString().split('T')[0]
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
    error.value = 'Erro ao carregar dados: ' + (e instanceof Error ? e.message : 'Erro desconhecido')
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
    await financeService.create(payload as unknown as FinanceRecord)
    await fetchTransactions()
    isModalOpen.value = false
  } catch (e: unknown) {
    alert('Erro ao salvar: ' + (e instanceof Error ? e.message : 'Erro desconhecido'))
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
    await financeService.update(payload as unknown as FinanceRecord)
    await fetchTransactions()
    isEditModalOpen.value = false
  } catch (e: unknown) {
    alert('Erro ao atualizar: ' + (e instanceof Error ? e.message : 'Erro desconhecido'))
  } finally {
    isSubmitting.value = false
  }
}

async function handleMarkAsPaid(transaction: FinanceRecord) {
  if (confirm('Marcar esta conta como PAGA?')) {
    try {
      const payload = {
        ...transaction,
        status: 'PAID',
        client: { id: transaction.client },
        user: { id: getCurrentUserId() }
      }
      await financeService.update(payload as unknown as FinanceRecord)
      await fetchTransactions()
    } catch (e: unknown) {
      alert('Erro ao atualizar: ' + (e instanceof Error ? e.message : 'Erro desconhecido'))
    }
  }
}

async function handleDelete(id: string | number) {
  if (confirm('Tem certeza que deseja excluir este registro?')) {
    try {
      await financeService.delete(id)
      await fetchTransactions()
    } catch (e: unknown) {
      alert('Erro ao excluir: ' + (e instanceof Error ? e.message : 'Erro desconhecido'))
    }
  }
}

onMounted(() => {
  fetchTransactions()
})

const stats = computed(() => [
  { label: 'Receita do Mês', value: formatCurrency(incomeTotal.value), sub: 'Total bruto', icon: TrendingUp, color: 'text-green-600', bg: 'bg-green-50' },
  { label: 'Despesas', value: formatCurrency(expenseTotal.value), sub: 'Total de saídas', icon: TrendingDown, color: 'text-red-500', bg: 'bg-red-50' },
  { label: 'Lucro Líquido', value: formatCurrency(netProfit.value), sub: 'Margem real', icon: DollarSign, color: 'text-purple-600', bg: 'bg-purple-50' },
])

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

    <div class="grid grid-cols-3 gap-4 mb-6">
      <Card v-for="stat in stats" :key="stat.label" class="p-5 flex items-center gap-4">
        <div :class="['w-12 h-12 rounded-xl flex items-center justify-center', stat.bg]">
          <component :is="stat.icon" :class="['w-6 h-6', stat.color]" />
        </div>
        <div>
          <p class="text-xs font-semibold text-gray-400 uppercase tracking-wide">{{ stat.label }}</p>
          <p class="text-2xl font-bold text-gray-900 mt-0.5">{{ stat.value }}</p>
          <p class="text-xs text-gray-400 mt-0.5">{{ stat.sub }}</p>
        </div>
      </Card>
    </div>

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
              <Badge :variant="t.status === 'PAID' ? 'success' : (t.status === 'OVERDUE' ? 'destructive' : 'warning')">
                {{ t.status }}
              </Badge>
            </td>
            <td :class="['px-5 py-4 text-sm font-semibold text-right', t.value >= 0 ? 'text-green-600' : 'text-red-500']">
              {{ t.value >= 0 ? '+' : '' }}{{ formatCurrency(t.value) }}
            </td>
            <td class="px-5 py-4">
              <div class="flex items-center justify-center gap-2">
                <button 
                  v-if="t.status !== 'PAID'"
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
      <!-- Pagination Footer -->
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

    <div v-if="isModalOpen" class="fixed inset-0 z-50 flex items-center justify-center p-4 bg-black/50 backdrop-blur-sm">
      <div class="bg-white rounded-2xl shadow-xl w-full max-w-lg overflow-hidden animate-in fade-in zoom-in duration-200">
        <div class="flex items-center justify-between p-6 border-b border-gray-100">
          <h2 class="text-xl font-bold text-gray-900">Nova Transação</h2>
          <button @click="isModalOpen = false" class="p-2 hover:bg-gray-100 rounded-lg text-gray-400"><X class="w-5 h-5" /></button>
        </div>
        <div class="p-6 space-y-4">
          <div class="space-y-1.5">
            <label class="text-xs font-semibold text-gray-500 uppercase">Cliente</label>
            <div class="relative">
              <select 
                v-model="newTransaction.client" 
                :class="['w-full p-2.5 pr-10 rounded-lg border bg-gray-50 text-sm focus:outline-none focus:ring-2 focus:ring-primary/20 focus:border-primary appearance-none cursor-pointer', fieldErrors.client ? 'border-red-500' : 'border-gray-200']"
              >
                <option value="">Selecione um cliente</option>
                <option v-for="c in sortedClients" :key="c.id" :value="c.id">{{ c.name }}</option>
              </select>
              <ChevronDown class="absolute right-3 top-1/2 -translate-y-1/2 w-4 h-4 text-gray-400 pointer-events-none" />
            </div>
            <p v-if="fieldErrors.client" class="text-[10px] text-red-500 font-medium">{{ fieldErrors.client }}</p>
          </div>
          <div class="space-y-1.5">
            <label class="text-xs font-semibold text-gray-500 uppercase">Descrição</label>
            <input v-model="newTransaction.description" type="text" :class="['w-full p-2.5 rounded-lg border bg-gray-50 focus:outline-none focus:ring-2 focus:ring-primary/20 focus:border-primary text-sm', fieldErrors.description ? 'border-red-500' : 'border-gray-200']" placeholder="Ex: Mensalidade Abril" />
            <p v-if="fieldErrors.description" class="text-[10px] text-red-500 font-medium">{{ fieldErrors.description }}</p>
          </div>
          <div class="grid grid-cols-2 gap-4">
            <div class="space-y-1.5">
              <label class="text-xs font-semibold text-gray-500 uppercase">Valor (R$)</label>
              <input v-model.number="newTransaction.value" type="number" step="0.01" :class="['w-full p-2.5 rounded-lg border bg-gray-50 focus:outline-none focus:ring-2 focus:ring-primary/20 focus:border-primary text-sm', fieldErrors.value ? 'border-red-500' : 'border-gray-200']" placeholder="0.00" />
              <p v-if="fieldErrors.value" class="text-[10px] text-red-500 font-medium">{{ fieldErrors.value }}</p>
            </div>
            <div class="space-y-1.5">
              <label class="text-xs font-semibold text-gray-500 uppercase">Vencimento</label>
              <input v-model="newTransaction.expirationDate" type="date" :class="['w-full p-2.5 rounded-lg border bg-gray-50 focus:outline-none focus:ring-2 focus:ring-primary/20 focus:border-primary text-sm', fieldErrors.expirationDate ? 'border-red-500' : 'border-gray-200']" />
              <p v-if="fieldErrors.expirationDate" class="text-[10px] text-red-500 font-medium">{{ fieldErrors.expirationDate }}</p>
            </div>
          </div>
          <div class="space-y-1.5">
            <label class="text-xs font-semibold text-gray-500 uppercase">Status Inicial</label>
            <div class="flex gap-2">
              <button 
                v-for="s in ['PENDING', 'PAID']" 
                :key="s"
                type="button"
                @click="newTransaction.status = s"
                :class="['flex-1 py-2 rounded-lg text-xs font-bold border transition-all', newTransaction.status === s ? 'bg-primary text-white border-primary' : 'bg-gray-50 text-gray-500 border-gray-200 hover:bg-gray-100']"
              >
                {{ s === 'PENDING' ? 'PENDENTE' : 'PAGO' }}
              </button>
            </div>
          </div>
        </div>
        <div class="p-6 bg-gray-50 border-t border-gray-100 flex gap-3">
          <Button variant="outline" class="flex-1" @click="isModalOpen = false">Cancelar</Button>
          <Button class="flex-1" :disabled="isSubmitting" @click="handleCreateTransaction">
            {{ isSubmitting ? 'Salvando...' : 'Salvar Transação' }}
          </Button>
        </div>
      </div>
    </div>

    <!-- Modal Editar Transação -->
    <div v-if="isEditModalOpen" class="fixed inset-0 z-50 flex items-center justify-center p-4 bg-black/50 backdrop-blur-sm">
      <div class="bg-white rounded-2xl shadow-xl w-full max-w-lg overflow-hidden animate-in fade-in zoom-in duration-200">
        <div class="flex items-center justify-between p-6 border-b border-gray-100">
          <h2 class="text-xl font-bold text-gray-900">Editar Transação</h2>
          <button @click="isEditModalOpen = false" class="p-2 hover:bg-gray-100 rounded-lg text-gray-400"><X class="w-5 h-5" /></button>
        </div>
        <div class="p-6 space-y-4">
          <div class="space-y-1.5">
            <label class="text-xs font-semibold text-gray-500 uppercase">Cliente</label>
            <div class="relative">
              <select
                v-model="editTransaction.client"
                :class="['w-full p-2.5 pr-10 rounded-lg border bg-gray-50 text-sm focus:outline-none focus:ring-2 focus:ring-primary/20 focus:border-primary appearance-none cursor-pointer', editFieldErrors.client ? 'border-red-500' : 'border-gray-200']"
              >
                <option value="">Selecione um cliente</option>
                <option v-for="c in sortedClients" :key="c.id" :value="c.id">{{ c.name }}</option>
              </select>
              <ChevronDown class="absolute right-3 top-1/2 -translate-y-1/2 w-4 h-4 text-gray-400 pointer-events-none" />
            </div>
            <p v-if="editFieldErrors.client" class="text-[10px] text-red-500 font-medium">{{ editFieldErrors.client }}</p>
          </div>
          <div class="space-y-1.5">
            <label class="text-xs font-semibold text-gray-500 uppercase">Descrição</label>
            <input v-model="editTransaction.description" type="text" :class="['w-full p-2.5 rounded-lg border bg-gray-50 focus:outline-none focus:ring-2 focus:ring-primary/20 focus:border-primary text-sm', editFieldErrors.description ? 'border-red-500' : 'border-gray-200']" placeholder="Ex: Mensalidade Abril" />
            <p v-if="editFieldErrors.description" class="text-[10px] text-red-500 font-medium">{{ editFieldErrors.description }}</p>
          </div>
          <div class="grid grid-cols-2 gap-4">
            <div class="space-y-1.5">
              <label class="text-xs font-semibold text-gray-500 uppercase">Valor (R$)</label>
              <input v-model.number="editTransaction.value" type="number" step="0.01" :class="['w-full p-2.5 rounded-lg border bg-gray-50 focus:outline-none focus:ring-2 focus:ring-primary/20 focus:border-primary text-sm', editFieldErrors.value ? 'border-red-500' : 'border-gray-200']" placeholder="0.00" />
              <p v-if="editFieldErrors.value" class="text-[10px] text-red-500 font-medium">{{ editFieldErrors.value }}</p>
            </div>
            <div class="space-y-1.5">
              <label class="text-xs font-semibold text-gray-500 uppercase">Vencimento</label>
              <input v-model="editTransaction.expirationDate" type="date" :class="['w-full p-2.5 rounded-lg border bg-gray-50 focus:outline-none focus:ring-2 focus:ring-primary/20 focus:border-primary text-sm', editFieldErrors.expirationDate ? 'border-red-500' : 'border-gray-200']" />
              <p v-if="editFieldErrors.expirationDate" class="text-[10px] text-red-500 font-medium">{{ editFieldErrors.expirationDate }}</p>
            </div>
          </div>
          <div class="space-y-1.5">
            <label class="text-xs font-semibold text-gray-500 uppercase">Status</label>
            <div class="flex gap-2">
              <button
                v-for="s in ['PENDING', 'PAID', 'OVERDUE']"
                :key="s"
                type="button"
                @click="editTransaction.status = s"
                :class="['flex-1 py-2 rounded-lg text-xs font-bold border transition-all', editTransaction.status === s ? 'bg-primary text-white border-primary' : 'bg-gray-50 text-gray-500 border-gray-200 hover:bg-gray-100']"
              >
                {{ s === 'PENDING' ? 'PENDENTE' : s === 'PAID' ? 'PAGO' : 'VENCIDO' }}
              </button>
            </div>
          </div>
        </div>
        <div class="p-6 bg-gray-50 border-t border-gray-100 flex gap-3">
          <Button variant="outline" class="flex-1" @click="isEditModalOpen = false">Cancelar</Button>
          <Button class="flex-1" :disabled="isSubmitting" @click="handleEditTransaction">
            {{ isSubmitting ? 'Salvando...' : 'Atualizar Transação' }}
          </Button>
        </div>
      </div>
    </div>
  </AppLayout>
</template>
