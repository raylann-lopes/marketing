<script setup lang="ts">
import { ref, computed, onMounted, watch } from 'vue'
import { X, Pencil, Trash2, MessageCircle, HardDrive, Sparkles, ChevronLeft, ChevronRight } from 'lucide-vue-next'
import { useRouter } from 'vue-router'
import AppLayout from '@/components/layout/AppLayout.vue'
import Badge from '@/components/ui/Badge.vue'
import Button from '@/components/ui/Button.vue'
import Avatar from '@/components/ui/Avatar.vue'

import ClientModal from '@/components/clients/ClientModal.vue'

import { clientService, type Client } from '@/services/clientService'
import { getErrorMessage } from '@/lib/errors'
import { useFeedback } from '@/lib/feedback'
import { z } from 'zod'

const search = ref('')
const clients = ref<Client[]>([])
const loading = ref(true)
const error = ref('')
const selectedClient = ref<Client | null>(null)
const feedback = useFeedback()
const router = useRouter()

const currentPage = ref(1)
const itemsPerPage = 9

watch(search, () => {
  currentPage.value = 1
})

const isModalOpen = ref(false)
const isEditModalOpen = ref(false)
const clientToEdit = ref<Client | null>(null)
const isSubmitting = ref(false)
const fieldErrors = ref<Record<string, string>>({})
const editFieldErrors = ref<Record<string, string>>({})

const newClient = ref<Partial<Client>>({
  name: '',
  email: '',
  number: '',
  niche: '',
  driveLink: '',
  voiceTone: '',
  monthlyValue: 0,
})

const editClient = ref<Partial<Client>>({
  name: '',
  email: '',
  number: '',
  niche: '',
  driveLink: '',
  voiceTone: '',
  monthlyValue: 0,
})

const clientSchema = z.object({
  name: z.string().min(3, 'O nome deve ter pelo menos 3 caracteres'),
  email: z.string().email('E-mail inválido'),
  number: z.string().min(12, 'Telefone incompleto. Digite o DDD e o número').max(13, 'Telefone muito longo'),
  driveLink: z.string().min(1, 'O link do Google Drive é obrigatório'),
  voiceTone: z.string().optional(),
  niche: z.string().optional()
})

function openModal() {
  newClient.value = {
    name: '',
    email: '',
    number: '',
    niche: '',
    driveLink: '',
    voiceTone: '',
    monthlyValue: 0,
  }
  fieldErrors.value = {}
  isModalOpen.value = true
}

function openEditModal(client: Client) {
  clientToEdit.value = client
  editClient.value = {
    name: client.name,
    email: client.email,
    number: client.number || '',
    niche: client.niche || '',
    driveLink: client.driveLink || '',
    voiceTone: client.voiceTone || '',
    monthlyValue: client.monthlyValue ?? 0,
  }
  editFieldErrors.value = {}
  isEditModalOpen.value = true
}

async function handleCreateClient() {
  fieldErrors.value = {}

  const payload = {
    name: newClient.value.name?.trim(),
    email: newClient.value.email?.trim(),
    number: newClient.value.number?.replace(/\D/g, ''),
    driveLink: newClient.value.driveLink?.trim(),
    voiceTone: newClient.value.voiceTone?.trim() || '',
    niche: newClient.value.niche?.trim() || '',
    monthlyValue: newClient.value.monthlyValue ?? 0
  }

  const result = clientSchema.safeParse(payload)

  if (!result.success) {
    const errors: Record<string, string> = {}
    result.error.issues.forEach((issue) => {
      const key = issue.path[0]
      if (typeof key === 'string') {
        errors[key] = issue.message
      }
    })
    fieldErrors.value = errors
    return
  }

  isSubmitting.value = true
  try {
    await clientService.create(payload as Client)
    await fetchClients()
    isModalOpen.value = false
    feedback.success('Cliente cadastrado com sucesso.')
  } catch (e: unknown) {
    feedback.error(`Erro ao cadastrar: ${getErrorMessage(e)}`)
  } finally {
    isSubmitting.value = false
  }
}

async function handleEditClient() {
  editFieldErrors.value = {}
  const payload = {
    name: editClient.value.name?.trim(),
    email: editClient.value.email?.trim(),
    number: editClient.value.number?.replace(/\D/g, ''),
    driveLink: editClient.value.driveLink?.trim(),
    voiceTone: editClient.value.voiceTone?.trim() || '',
    niche: editClient.value.niche?.trim() || '',
    monthlyValue: editClient.value.monthlyValue ?? 0
  }

  const result = clientSchema.safeParse(payload)
  if (!result.success) {
    const errors: Record<string, string> = {}
    result.error.issues.forEach((issue) => {
      const key = issue.path[0]
      if (typeof key === 'string') errors[key] = issue.message
    })
    editFieldErrors.value = errors
    return
  }

  isSubmitting.value = true
  try {
    await clientService.update(clientToEdit.value!.id!, payload as Client)
    await fetchClients()
    const updated = clients.value.find(c => String(c.id) === String(clientToEdit.value!.id))
    if (updated) selectedClient.value = updated
    isEditModalOpen.value = false
    feedback.success('Cliente atualizado com sucesso.')
  } catch (e: unknown) {
    feedback.error(`Erro ao atualizar: ${getErrorMessage(e)}`)
  } finally {
    isSubmitting.value = false
  }
}

async function fetchClients() {
  loading.value = true
  error.value = ''
  try {
    const data = await clientService.getAll()

    if (Array.isArray(data)) {
      clients.value = data
    } else if (data && typeof data === 'object' && 'data' in data && Array.isArray((data as { data: Client[] }).data)) {
      clients.value = (data as { data: Client[] }).data
    } else {
      clients.value = []
    }
  } catch (e: unknown) {
    error.value = `Erro ao carregar clientes: ${getErrorMessage(e)}`
  } finally {
    loading.value = false
  }
}

onMounted(() => {
  fetchClients()
})

const filtered = computed<Client[]>(() =>
  clients.value.filter(c =>
    c.name.toLowerCase().includes(search.value.toLowerCase()) ||
    c.niche?.toLowerCase().includes(search.value.toLowerCase())
  )
)

const totalPages = computed(() => Math.ceil(filtered.value.length / itemsPerPage))

const paginatedClients = computed(() => {
  const start = (currentPage.value - 1) * itemsPerPage
  const end = start + itemsPerPage
  return filtered.value.slice(start, end)
})

function selectClient(c: Client) {
  selectedClient.value = c
}

function nextPage() {
  if (currentPage.value < totalPages.value) currentPage.value++
}

function prevPage() {
  if (currentPage.value > 1) currentPage.value--
}

const isTogglingStatus = ref(false)

async function handleToggleStatus(client: Client) {
  const newStatus = client.status === 'ACTIVE' ? 'INACTIVE' : 'ACTIVE'
  const actionLabel = newStatus === 'INACTIVE' ? 'inativar' : 'ativar'
  const confirmed = await feedback.confirm({
    title: `${newStatus === 'INACTIVE' ? 'Inativar' : 'Ativar'} cliente`,
    message: `Deseja ${actionLabel} o cliente "${client.name}"? ${newStatus === 'INACTIVE' ? 'As mensalidades futuras pendentes serão removidas.' : 'As mensalidades dos próximos 12 meses serão geradas.'}`,
    confirmText: newStatus === 'INACTIVE' ? 'Inativar' : 'Ativar',
    tone: newStatus === 'INACTIVE' ? 'danger' : undefined,
  })
  if (!confirmed) return

  isTogglingStatus.value = true
  try {
    const updated = await clientService.updateStatus(client.id!, newStatus)
    await fetchClients()
    if (selectedClient.value?.id === client.id) {
      selectedClient.value = clients.value.find(c => String(c.id) === String(client.id)) || null
    }
    feedback.success(`Cliente ${newStatus === 'ACTIVE' ? 'ativado' : 'inativado'} com sucesso.`)
    return updated
  } catch (e: unknown) {
    feedback.error(`Erro ao alterar status: ${getErrorMessage(e)}`)
  } finally {
    isTogglingStatus.value = false
  }
}

async function handleDelete(id: string | number) {
  const confirmed = await feedback.confirm({
    title: 'Excluir cliente',
    message: 'Tem certeza que deseja excluir este cliente? Esta ação não pode ser desfeita.',
    confirmText: 'Excluir',
    tone: 'danger',
  })
  if (!confirmed) return

  try {
    await clientService.delete(id)
    await fetchClients()
    selectedClient.value = null
    feedback.success('Cliente excluído com sucesso.')
  } catch (e: unknown) {
    feedback.error(`Erro ao excluir cliente: ${getErrorMessage(e)}`)
  }
}

function formatCurrency(value: number): string {
  return new Intl.NumberFormat('pt-BR', { style: 'currency', currency: 'BRL' }).format(value)
}

function openClientWorkspace(client: Client) {
  if (!client.id) return
  router.push({
    path: `/clients/${client.id}/workspace`,
    query: {
      name: client.name,
      email: client.email,
      niche: client.niche || '',
      status: client.status || '',
      number: client.number || '',
      driveLink: client.driveLink || '',
      voiceTone: client.voiceTone || '',
    },
  })
}
</script>

<template>
  <AppLayout v-model:search="search" topbar-placeholder="Buscar clientes por nome ou nicho...">
    <div class="flex gap-6 h-[calc(100vh-140px)]">

      <!-- Table -->
      <div class="flex-1 bg-white rounded-xl border border-gray-100 shadow-sm overflow-hidden flex flex-col min-h-0">
        <div class="p-5 border-b border-gray-100 flex items-center justify-between">
          <div>
            <h1 class="text-2xl font-bold text-gray-900">Clientes</h1>
            <p class="text-gray-500 text-sm mt-0.5">Gerencie sua carteira de parceiros criativos.</p>
          </div>
          <div class="flex items-center gap-3">
            <Button class="gap-2" @click="openModal">
              <span class="text-base leading-none">+</span>
              Novo Cliente
            </Button>
          </div>
        </div>

        <div v-if="loading" class="flex-1 flex items-center justify-center">
          <div class="animate-spin rounded-full h-8 w-8 border-b-2 border-primary"></div>
        </div>

        <div v-else-if="error" class="flex-1 flex flex-col items-center justify-center p-6 text-center">
          <p class="text-red-500 mb-4">{{ error }}</p>
          <Button variant="outline" @click="fetchClients">Tentar Novamente</Button>
        </div>

        <div v-else-if="paginatedClients.length === 0" class="flex-1 flex flex-col items-center justify-center p-6 text-center">
          <p class="text-gray-500">Nenhum cliente encontrado.</p>
        </div>

        <div v-else class="flex-1">
          <table class="w-full">
            <thead>
              <tr class="border-b border-gray-100">
                <th class="text-left px-5 py-3 text-xs font-semibold text-gray-400 uppercase tracking-wide">Cliente</th>
                <th class="text-left px-5 py-3 text-xs font-semibold text-gray-400 uppercase tracking-wide">Nicho</th>
                <th class="text-left px-5 py-3 text-xs font-semibold text-gray-400 uppercase tracking-wide">Telefone</th>
                <th class="text-left px-5 py-3 text-xs font-semibold text-gray-400 uppercase tracking-wide">Mensalidade</th>
                <th class="text-left px-5 py-3 text-xs font-semibold text-gray-400 uppercase tracking-wide">Status</th>
              </tr>
            </thead>
            <tbody>
              <tr
                v-for="client in paginatedClients"
                :key="client.id"
                :class="[
                  'border-b border-gray-50 cursor-pointer transition-colors hover:bg-purple-50',
                  selectedClient?.id === client.id ? 'bg-purple-50' : ''
                ]"
                @click="selectClient(client)"
              >
                <td class="px-5 py-4">
                  <div class="flex items-center gap-3">
                    <Avatar :name="client.name" size="md" />
                    <div>
                      <p class="text-sm font-semibold text-gray-800">{{ client.name }}</p>
                      <p class="text-xs text-gray-400">{{ client.email }}</p>
                    </div>
                  </div>
                </td>
                <td class="px-5 py-4">
                  <Badge variant="secondary" class="text-xs">{{ client.niche || 'Sem nicho' }}</Badge>
                </td>
                <td class="px-5 py-4 text-sm text-gray-700">{{ client.number || '-' }}</td>
                <td class="px-5 py-4 text-sm font-semibold text-gray-700">
                  {{ client.monthlyValue ? formatCurrency(client.monthlyValue) : '-' }}
                </td>
                <td class="px-5 py-4">
                  <Badge :variant="client.status === 'ACTIVE' ? 'success' : 'warning'">{{ client.status }}</Badge>
                </td>
              </tr>
            </tbody>
          </table>
        </div>

        <!-- Pagination Footer -->
        <div v-if="filtered.length > itemsPerPage" class="p-4 border-t border-gray-100 flex items-center justify-between bg-gray-50/50">
          <p class="text-xs text-gray-500">
            Mostrando <span class="font-semibold">{{ (currentPage - 1) * itemsPerPage + 1 }}</span> a 
            <span class="font-semibold">{{ Math.min(currentPage * itemsPerPage, filtered.length) }}</span> de 
            <span class="font-semibold">{{ filtered.length }}</span> clientes
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
      </div>

      <!-- Detail sidebar -->
      <transition name="slide">
        <div v-if="selectedClient" class="w-80 bg-white rounded-xl border border-gray-100 shadow-sm flex flex-col shrink-0 overflow-hidden">
          <div class="flex items-center justify-end gap-2 p-3 border-b border-gray-100">
            <button
              class="p-1.5 hover:bg-gray-100 rounded-lg text-gray-400 hover:text-gray-600"
              title="Editar cliente"
              @click="openEditModal(selectedClient!)"
            >
              <Pencil class="w-4 h-4" />
            </button>
            <button
              :class="['p-1.5 rounded-lg text-xs font-semibold px-2 transition-colors', selectedClient.status === 'ACTIVE' ? 'hover:bg-yellow-50 text-yellow-600 hover:text-yellow-700' : 'hover:bg-green-50 text-green-600 hover:text-green-700']"
              :title="selectedClient.status === 'ACTIVE' ? 'Inativar cliente' : 'Ativar cliente'"
              :disabled="isTogglingStatus"
              @click="handleToggleStatus(selectedClient!)"
            >
              {{ selectedClient.status === 'ACTIVE' ? 'Inativar' : 'Ativar' }}
            </button>
            <button
              class="p-1.5 hover:bg-red-50 rounded-lg text-gray-400 hover:text-red-500"
              @click="handleDelete(selectedClient.id!)"
            >
              <Trash2 class="w-4 h-4" />
            </button>
            <button class="p-1.5 hover:bg-gray-100 rounded-lg text-gray-400" @click="selectedClient = null">
              <X class="w-4 h-4" />
            </button>
          </div>

          <div class="flex-1 overflow-y-auto">
            <div class="flex flex-col items-center py-6 px-5 border-b border-gray-100">
              <Avatar :name="selectedClient.name" size="lg" class="w-16 h-16 text-2xl mb-3" />
              <h2 class="text-lg font-bold text-gray-900">{{ selectedClient.name }}</h2>
              <p class="text-sm text-gray-500">{{ selectedClient.niche }}</p>
              <div class="flex items-center gap-1.5 mt-2">
                <span class="w-2 h-2 bg-green-500 rounded-full" />
                <span class="text-xs font-semibold text-green-600 uppercase">Status: {{ selectedClient.status }}</span>
              </div>
            </div>

            <div class="p-4 space-y-4">
              <div>
                <p class="text-xs font-semibold text-gray-400 uppercase tracking-wide mb-2">Identidade de Marca</p>
                <div class="bg-gray-50 rounded-lg p-3 space-y-3">
                  <div>
                    <p class="text-xs font-semibold text-gray-500 uppercase">Tom de Voz</p>
                    <p class="text-sm text-gray-700 mt-1">{{ selectedClient.voiceTone }}</p>
                  </div>
                  <div>
                    <p class="text-xs font-semibold text-gray-500 uppercase">Telefone</p>
                    <p class="text-sm text-gray-700 mt-1">{{ selectedClient.number || 'Não informado' }}</p>
                  </div>
                  <div>
                    <p class="text-xs font-semibold text-gray-500 uppercase">Valor Mensal</p>
                    <p class="text-sm font-semibold text-gray-700 mt-1">{{ selectedClient.monthlyValue ? formatCurrency(selectedClient.monthlyValue) : 'Não informado' }}</p>
                  </div>
                </div>
              </div>

              <div>
                <p class="text-xs font-semibold text-gray-400 uppercase tracking-wide mb-2">Acessos Rápidos</p>
                <div class="grid grid-cols-2 gap-2">
                  <a 
                    :href="(() => {
                      const clean = selectedClient.number?.replace(/\D/g, '') || '';
                      const withCountry = clean.startsWith('55') ? clean : '55' + clean;
                      return 'https://wa.me/' + withCountry;
                    })()" 
                    target="_blank" 
                    class="flex flex-col items-center gap-1.5 p-3 bg-gray-50 rounded-lg hover:bg-green-50 transition-colors"
                  >
                    <MessageCircle class="w-5 h-5 text-green-600" />
                    <span class="text-xs font-medium text-gray-600">WhatsApp</span>
                  </a>
                  <a :href="selectedClient.driveLink" target="_blank" class="flex flex-col items-center gap-1.5 p-3 bg-gray-50 rounded-lg hover:bg-blue-50 transition-colors">
                    <HardDrive class="w-5 h-5 text-blue-600" />
                    <span class="text-xs font-medium text-gray-600">Google Drive</span>
                  </a>
                </div>
                <button
                  class="mt-2 w-full rounded-lg border border-primary/20 bg-primary/5 px-3 py-2 text-xs font-semibold text-primary hover:bg-primary/10 transition-colors"
                  @click="openClientWorkspace(selectedClient)"
                >
                  Abrir Workspace do Cliente
                </button>
              </div>

              <div class="bg-purple-50 rounded-xl p-3 flex items-center gap-3">
                <Sparkles class="w-4 h-4 text-purple-600 shrink-0" />
                <div class="flex-1">
                  <p class="text-xs font-semibold text-purple-600 uppercase">Criar Post para {{ selectedClient.name }}</p>
                  <p class="text-xs text-gray-600 mt-0.5">Ir para o board e criar nova demanda.</p>
                </div>
                <button
                  class="w-6 h-6 bg-purple-600 text-white rounded-full flex items-center justify-center hover:bg-purple-700"
                  @click="$router.push(`/board?clientId=${selectedClient.id}`)"
                >
                  <span class="text-sm leading-none">+</span>
                </button>
              </div>
            </div>
          </div>
        </div>
      </transition>
    </div>

    <ClientModal
      :is-open="isModalOpen"
      :is-edit="false"
      v-model:client="newClient"
      :is-submitting="isSubmitting"
      :field-errors="fieldErrors"
      @close="isModalOpen = false"
      @save="handleCreateClient"
    />

    <ClientModal
      :is-open="isEditModalOpen"
      :is-edit="true"
      v-model:client="editClient"
      :is-submitting="isSubmitting"
      :field-errors="editFieldErrors"
      @close="isEditModalOpen = false"
      @save="handleEditClient"
    />
  </AppLayout>
</template>

<style scoped>
.slide-enter-active, .slide-leave-active { transition: all 0.2s ease; }
.slide-enter-from, .slide-leave-to { opacity: 0; transform: translateX(20px); }
</style>
