<script setup lang="ts">
import { computed, onMounted, ref, watch } from 'vue'
import {
  AlertTriangle,
  CalendarDays,
  CheckCircle2,
  ChevronLeft,
  ChevronRight,
  Clock3,
  ListChecks,
  Loader2,
  Plus,
  RefreshCw,
  RotateCcw,
  Search,
  Trash2,
} from 'lucide-vue-next'
import AppLayout from '@/components/layout/AppLayout.vue'
import Badge from '@/components/ui/Badge.vue'
import Button from '@/components/ui/Button.vue'
import Card from '@/components/ui/Card.vue'
import TaskDetailsModal from '@/components/tasks/TaskDetailsModal.vue'
import TaskModal, { type TaskForm } from '@/components/tasks/TaskModal.vue'
import { clientService, type Client } from '@/services/clientService'
import {
  taskService,
  type TaskPayload,
  type TaskPriority,
  type TaskRecord,
  type TaskStatus,
  type TaskType,
} from '@/services/taskService'
import { getErrorMessage } from '@/lib/errors'
import { useFeedback } from '@/lib/feedback'
import { formatDate, formatTime } from '@/lib/dateTime'

const feedback = useFeedback()
const clients = ref<Client[]>([])
const tasks = ref<TaskRecord[]>([])
const loading = ref(false)
const submitting = ref(false)
const loadingClients = ref(false)
const busyTaskId = ref<number | null>(null)
const deletingTaskId = ref<number | null>(null)
const isTaskModalOpen = ref(false)
const selectedTask = ref<TaskRecord | null>(null)
const error = ref('')
const search = ref('')
const statusFilter = ref<'ALL' | TaskStatus>('PENDING')
const startDate = ref(todayInput())
const endDate = ref(todayInput())
const priorityFilter = ref<'ALL' | TaskPriority>('ALL')
const page = ref(0)
const size = ref(10)
const totalPages = ref(0)
const totalElements = ref(0)
const form = ref<TaskForm>(initialForm())

const taskTypeOptions: Array<{ value: TaskType; label: string }> = [
  { value: 'TAREFA', label: 'Tarefa' },
  { value: 'REUNIAO', label: 'Reunião' },
  { value: 'LEMBRETE', label: 'Lembrete' },
  { value: 'COBRANCA', label: 'Cobrança' },
  { value: 'PRAZO', label: 'Prazo' },
  { value: 'FOLLOW_UP', label: 'Follow-up' },
]

const priorityOptions: Array<{ value: TaskPriority; label: string }> = [
  { value: 'LOW', label: 'Baixa' },
  { value: 'NORMAL', label: 'Normal' },
  { value: 'HIGH', label: 'Alta' },
  { value: 'URGENT', label: 'Urgente' },
]

const filteredTasks = computed(() => {
  const term = search.value.trim().toLowerCase()

  return tasks.value.filter(task => {
    const matchesSearch =
      !term ||
      task.title.toLowerCase().includes(term) ||
      (task.description ?? '').toLowerCase().includes(term) ||
      (task.clientName ?? '').toLowerCase().includes(term)

    const matchesPriority = priorityFilter.value === 'ALL' || task.priority === priorityFilter.value

    return matchesSearch && matchesPriority
  })
})

const todayTasks = computed(() => tasks.value.filter(task => task.dateExpires === todayInput()).length)
const pendingTasks = computed(() => tasks.value.filter(task => task.status === 'PENDING').length)
const urgentTasks = computed(() => tasks.value.filter(task => task.priority === 'URGENT' || task.priority === 'HIGH').length)
const hasInvalidDateRange = computed(() => Boolean(startDate.value && endDate.value && startDate.value > endDate.value))
const dateRangeLabel = computed(() => {
  if (!startDate.value && !endDate.value) return 'em todas as datas'
  if (startDate.value === endDate.value) return `em ${formatDate(startDate.value)}`
  if (startDate.value && endDate.value) {
    return `de ${formatDate(startDate.value)} até ${formatDate(endDate.value)}`
  }
  return startDate.value
    ? `a partir de ${formatDate(startDate.value)}`
    : `até ${formatDate(endDate.value)}`
})

function initialForm(): TaskForm {
  return {
    title: '',
    description: '',
    clientId: '',
    clientName: '',
    dateExpires: todayInput(),
    timeExpires: '',
    type: 'TAREFA',
    priority: 'NORMAL',
  }
}

function todayInput() {
  const date = new Date()
  return `${date.getFullYear()}-${String(date.getMonth() + 1).padStart(2, '0')}-${String(date.getDate()).padStart(2, '0')}`
}

function typeLabel(type: TaskType) {
  return taskTypeOptions.find(option => option.value === type)?.label ?? type
}

function priorityLabel(priority: TaskPriority) {
  return priorityOptions.find(option => option.value === priority)?.label ?? priority
}

function priorityClass(priority: TaskPriority) {
  const classes: Record<TaskPriority, string> = {
    LOW: 'border-slate-200 bg-slate-50 text-slate-600',
    NORMAL: 'border-blue-100 bg-blue-50 text-blue-700',
    HIGH: 'border-amber-100 bg-amber-50 text-amber-700',
    URGENT: 'border-red-100 bg-red-50 text-red-700',
  }
  return classes[priority]
}

function priorityBorderClass(priority: TaskPriority) {
  const classes: Record<TaskPriority, string> = {
    LOW: 'border-l-slate-400',
    NORMAL: 'border-l-blue-500',
    HIGH: 'border-l-amber-500',
    URGENT: 'border-l-red-500',
  }
  return classes[priority]
}

function statusLabel(status: TaskStatus) {
  const labels: Record<TaskStatus, string> = {
    PENDING: 'Pendente',
    DONE: 'Concluída',
    CANCELED: 'Cancelada',
  }
  return labels[status]
}

function openTaskModal() {
  form.value = initialForm()
  isTaskModalOpen.value = true
}

async function fetchClients() {
  loadingClients.value = true
  try {
    const data = await clientService.getAll()
    clients.value = Array.isArray(data) ? data : []
  } catch (e: unknown) {
    feedback.error(`Erro ao carregar clientes: ${getErrorMessage(e)}`)
  } finally {
    loadingClients.value = false
  }
}

async function fetchTasks() {
  if (hasInvalidDateRange.value) {
    error.value = 'A data inicial não pode ser posterior à data final.'
    return
  }

  loading.value = true
  error.value = ''

  try {
    const response = await taskService.getMine(page.value, size.value, {
      status: statusFilter.value === 'ALL' ? undefined : statusFilter.value,
      startDate: startDate.value || undefined,
      endDate: endDate.value || undefined,
    })
    tasks.value = response.content ?? []
    totalPages.value = response.totalPages ?? 0
    totalElements.value = response.totalElements ?? tasks.value.length
  } catch (e: unknown) {
    error.value = getErrorMessage(e)
  } finally {
    loading.value = false
  }
}

async function submitTask() {
  if (!form.value.title.trim()) {
    feedback.error('Informe o título da tarefa.')
    return
  }

  if (!form.value.dateExpires) {
    feedback.error('Informe a data da tarefa.')
    return
  }

  submitting.value = true

  try {
    const selectedClient = clients.value.find(client => String(client.id) === form.value.clientId)
    const typedClientName = form.value.clientName.trim()
    const payload: TaskPayload = {
      title: form.value.title.trim(),
      description: form.value.description.trim() || null,
      clientId: selectedClient?.id != null ? Number(selectedClient.id) : null,
      clientName: selectedClient?.name ?? (typedClientName || null),
      dateExpires: form.value.dateExpires,
      timeExpires: form.value.timeExpires || null,
      type: form.value.type,
      priority: form.value.priority,
    }

    await taskService.create(payload)
    feedback.success('Tarefa criada com sucesso.')
    form.value = initialForm()
    isTaskModalOpen.value = false
    page.value = 0
    await fetchTasks()
  } catch (e: unknown) {
    feedback.error(`Erro ao criar tarefa: ${getErrorMessage(e)}`)
  } finally {
    submitting.value = false
  }
}

async function updateTaskStatus(task: TaskRecord, status: TaskStatus) {
  if (task.status === status) return

  busyTaskId.value = task.id
  try {
    const updatedTask = await taskService.updateStatus(task.id, { status })
    tasks.value = tasks.value.map(currentTask => (currentTask.id === task.id ? updatedTask : currentTask))

    const message =
      status === 'DONE'
        ? 'Tarefa concluída com sucesso.'
        : status === 'PENDING'
          ? 'Tarefa reaberta com sucesso.'
          : 'Tarefa cancelada com sucesso.'

    feedback.success(message)
    await fetchTasks()
  } catch (e: unknown) {
    feedback.error(`Erro ao atualizar tarefa: ${getErrorMessage(e)}`)
  } finally {
    busyTaskId.value = null
  }
}

async function deleteTask(task: TaskRecord) {
  const confirmed = await feedback.confirm({
    title: 'Excluir tarefa',
    message: `Deseja excluir a tarefa "${task.title}"? Esta ação não pode ser desfeita.`,
    confirmText: 'Excluir',
    tone: 'danger',
  })
  if (!confirmed) return

  deletingTaskId.value = task.id
  try {
    await taskService.remove(task.id)

    if (tasks.value.length === 1 && page.value > 0) {
      page.value -= 1
    }

    await fetchTasks()
    feedback.success('Tarefa excluída com sucesso.')
  } catch (e: unknown) {
    feedback.error(`Erro ao excluir tarefa: ${getErrorMessage(e)}`)
  } finally {
    deletingTaskId.value = null
  }
}

async function previousPage() {
  if (page.value === 0) return
  page.value -= 1
  await fetchTasks()
}

async function nextPage() {
  if (page.value >= totalPages.value - 1) return
  page.value += 1
  await fetchTasks()
}

onMounted(async () => {
  await Promise.all([fetchClients(), fetchTasks()])
})

watch([statusFilter, startDate, endDate], async () => {
  page.value = 0
  await fetchTasks()
})
</script>

<template>
  <AppLayout topbar-placeholder="Buscar tarefas, clientes ou compromissos...">
    <div class="space-y-6">
      <div class="flex flex-col gap-4 lg:flex-row lg:items-end lg:justify-between">
        <div>
          <h1 class="text-3xl font-bold tracking-tight text-gray-900">Tarefas</h1>
          <p class="mt-1 text-sm text-gray-500">Organize compromissos, cobranças e lembretes da operação.</p>
        </div>
        <div class="flex flex-wrap gap-2">
          <Button variant="outline" :disabled="loading" @click="fetchTasks">
            <RefreshCw :class="['h-4 w-4', loading ? 'animate-spin' : '']" />
            Atualizar
          </Button>
          <Button @click="openTaskModal">
            <Plus class="h-4 w-4" />
            Nova tarefa
          </Button>
        </div>
      </div>

      <div class="grid gap-4 md:grid-cols-3">
        <Card class="rounded-lg p-4">
          <div class="flex items-center justify-between">
            <div>
              <p class="text-xs font-semibold uppercase text-gray-400">Hoje</p>
              <p class="mt-1 text-2xl font-bold text-gray-900">{{ todayTasks }}</p>
            </div>
            <div class="flex h-9 w-9 items-center justify-center rounded-lg bg-blue-50 text-blue-600">
              <CalendarDays class="h-4 w-4" />
            </div>
          </div>
        </Card>

        <Card class="rounded-lg p-4">
          <div class="flex items-center justify-between">
            <div>
              <p class="text-xs font-semibold uppercase text-gray-400">Pendentes</p>
              <p class="mt-1 text-2xl font-bold text-gray-900">{{ pendingTasks }}</p>
            </div>
            <div class="flex h-9 w-9 items-center justify-center rounded-lg bg-violet-50 text-primary">
              <ListChecks class="h-4 w-4" />
            </div>
          </div>
        </Card>

        <Card class="rounded-lg p-4">
          <div class="flex items-center justify-between">
            <div>
              <p class="text-xs font-semibold uppercase text-gray-400">Alta prioridade</p>
              <p class="mt-1 text-2xl font-bold text-gray-900">{{ urgentTasks }}</p>
            </div>
            <div class="flex h-9 w-9 items-center justify-center rounded-lg bg-amber-50 text-amber-600">
              <AlertTriangle class="h-4 w-4" />
            </div>
          </div>
        </Card>
      </div>

      <section class="space-y-4">
          <Card class="rounded-lg p-4">
            <div class="grid gap-3 md:grid-cols-2 xl:grid-cols-[minmax(240px,1fr)_170px_170px_170px_170px]">
              <label class="relative block">
                <Search class="pointer-events-none absolute left-3 top-1/2 h-4 w-4 -translate-y-1/2 text-gray-400" />
                <input
                  v-model="search"
                  type="search"
                  class="h-10 w-full rounded-md border border-gray-200 bg-white pl-9 pr-3 text-sm outline-none transition focus:border-primary focus:ring-2 focus:ring-primary/10"
                  placeholder="Buscar por tarefa ou cliente"
                >
              </label>

              <label class="relative block">
                <span class="pointer-events-none absolute left-3 top-1/2 -translate-y-1/2 text-xs font-semibold text-gray-400">De</span>
                <input
                  v-model="startDate"
                  type="date"
                  :max="endDate || undefined"
                  aria-label="Data inicial"
                  title="Data inicial"
                  class="h-10 w-full rounded-md border border-gray-200 bg-white pl-10 pr-3 text-sm outline-none transition focus:border-primary focus:ring-2 focus:ring-primary/10"
                >
              </label>

              <label class="relative block">
                <span class="pointer-events-none absolute left-3 top-1/2 -translate-y-1/2 text-xs font-semibold text-gray-400">Até</span>
                <input
                  v-model="endDate"
                  type="date"
                  :min="startDate || undefined"
                  aria-label="Data final"
                  title="Data final"
                  class="h-10 w-full rounded-md border border-gray-200 bg-white pl-12 pr-3 text-sm outline-none transition focus:border-primary focus:ring-2 focus:ring-primary/10"
                >
              </label>

              <select
                v-model="statusFilter"
                class="h-10 rounded-md border border-gray-200 bg-white px-3 text-sm outline-none transition focus:border-primary focus:ring-2 focus:ring-primary/10"
              >
                <option value="ALL">Todos os status</option>
                <option value="PENDING">Pendentes</option>
                <option value="DONE">Concluídas</option>
                <option value="CANCELED">Canceladas</option>
              </select>

              <select
                v-model="priorityFilter"
                class="h-10 rounded-md border border-gray-200 bg-white px-3 text-sm outline-none transition focus:border-primary focus:ring-2 focus:ring-primary/10"
              >
                <option value="ALL">Todas prioridades</option>
                <option value="LOW">Baixa</option>
                <option value="NORMAL">Normal</option>
                <option value="HIGH">Alta</option>
                <option value="URGENT">Urgente</option>
              </select>
            </div>
          </Card>

          <Card class="rounded-lg overflow-hidden">
            <div class="flex items-center justify-between border-b border-gray-100 px-5 py-4">
              <div>
                <h2 class="text-base font-semibold text-gray-900">Minha agenda</h2>
                <p class="text-xs text-gray-500">
                  {{ totalElements }} tarefa{{ totalElements === 1 ? '' : 's' }} {{ dateRangeLabel }}
                </p>
              </div>
              <Badge variant="outline">Página {{ page + 1 }}</Badge>
            </div>

            <div v-if="loading" class="flex items-center justify-center py-16 text-sm text-gray-500">
              <Loader2 class="mr-2 h-4 w-4 animate-spin" />
              Carregando tarefas
            </div>

            <div v-else-if="error" class="px-5 py-12 text-center">
              <AlertTriangle class="mx-auto h-8 w-8 text-amber-500" />
              <p class="mt-3 text-sm font-medium text-gray-900">Não foi possível carregar as tarefas</p>
              <p class="mt-1 text-sm text-gray-500">{{ error }}</p>
            </div>

            <div v-else-if="filteredTasks.length === 0" class="px-5 py-14 text-center">
              <CheckCircle2 class="mx-auto h-9 w-9 text-gray-300" />
              <p class="mt-3 text-sm font-medium text-gray-900">Nenhuma tarefa encontrada</p>
              <p class="mt-1 text-sm text-gray-500">Não há tarefas para a data e os filtros selecionados.</p>
            </div>

            <div v-else class="px-5 py-4">
              <div class="grid grid-cols-[repeat(auto-fill,minmax(288px,288px))] gap-3">
                <article
                  v-for="task in filteredTasks"
                  :key="task.id"
                  role="button"
                  tabindex="0"
                  :aria-label="`Visualizar detalhes da tarefa ${task.title}`"
                  :class="[
                    'flex h-[180px] w-72 cursor-pointer flex-col overflow-hidden rounded-lg border border-l-4 border-gray-100 bg-white p-3 shadow-sm transition hover:-translate-y-0.5 hover:shadow-md focus:outline-none focus:ring-2 focus:ring-primary/30',
                    priorityBorderClass(task.priority),
                  ]"
                  @click="selectedTask = task"
                  @keydown.enter.self="selectedTask = task"
                  @keydown.space.self.prevent="selectedTask = task"
                >
	                  <div class="flex items-center justify-between gap-2">
	                    <span class="flex min-w-0 items-center gap-1.5 truncate text-xs font-semibold text-gray-600">
	                      <Clock3 class="h-3.5 w-3.5 shrink-0 text-gray-400" />
	                      {{ formatTime(task.timeExpires) || '--:--' }}
	                    </span>
	                    <div class="flex shrink-0 items-center gap-1">
	                      <Badge
	                        :variant="task.status === 'PENDING' ? 'warning' : task.status === 'DONE' ? 'success' : 'secondary'"
	                        class="px-2 py-0.5 text-[10px]"
	                      >
	                        {{ statusLabel(task.status) }}
	                      </Badge>
	                      <Button
	                        v-if="task.status === 'DONE'"
	                        variant="ghost"
	                        size="icon"
	                        class="h-7 w-7 text-blue-600 hover:bg-blue-50"
	                        title="Reabrir tarefa"
	                        :disabled="busyTaskId === task.id || deletingTaskId === task.id"
	                        @click.stop="updateTaskStatus(task, 'PENDING')"
	                      >
	                        <Loader2 v-if="busyTaskId === task.id" class="h-3.5 w-3.5 animate-spin" />
	                        <RotateCcw v-else class="h-3.5 w-3.5" />
	                      </Button>
	                      <Button
	                        v-else
	                        variant="ghost"
	                        size="icon"
	                        class="h-7 w-7 text-emerald-600 hover:bg-emerald-50"
	                        title="Concluir tarefa"
	                        :disabled="busyTaskId === task.id || deletingTaskId === task.id"
	                        @click.stop="updateTaskStatus(task, 'DONE')"
	                      >
	                        <Loader2 v-if="busyTaskId === task.id" class="h-3.5 w-3.5 animate-spin" />
	                        <CheckCircle2 v-else class="h-3.5 w-3.5" />
	                      </Button>
	                      <Button
	                        variant="ghost"
	                        size="icon"
	                        class="h-7 w-7 text-red-600 hover:bg-red-50"
	                        title="Excluir tarefa"
	                        :disabled="busyTaskId === task.id || deletingTaskId === task.id"
	                        @click.stop="deleteTask(task)"
	                      >
	                        <Loader2 v-if="deletingTaskId === task.id" class="h-3.5 w-3.5 animate-spin" />
	                        <Trash2 v-else class="h-3.5 w-3.5" />
	                      </Button>
	                    </div>
	                  </div>

	                  <div class="mt-3 min-h-0 flex-1">
	                    <h3
	                      :class="[
	                        'line-clamp-2 text-sm font-semibold leading-5',
	                        task.status === 'DONE' ? 'text-gray-500 line-through decoration-gray-300' : 'text-gray-900',
	                      ]"
	                    >
	                      {{ task.title }}
	                    </h3>
                    <p
                      v-if="task.description"
                      class="mt-1.5 line-clamp-2 break-words text-xs leading-5 text-gray-500"
                    >
                      {{ task.description }}
                    </p>
                  </div>

                  <div class="mt-3 flex items-end justify-between gap-2 border-t border-gray-100 pt-2">
                    <div class="min-w-0">
                      <p class="truncate text-xs font-semibold text-gray-600">
                        {{ task.clientName || 'Sem cliente' }}
                      </p>
                      <p class="truncate text-[11px] text-gray-400">
                        {{ formatDate(task.dateExpires) }} • {{ typeLabel(task.type) }}
                      </p>
                    </div>
                    <span :class="['shrink-0 rounded-full border px-2 py-0.5 text-[10px] font-semibold', priorityClass(task.priority)]">
                      {{ priorityLabel(task.priority) }}
                    </span>
                  </div>
                </article>
              </div>
            </div>

            <div class="flex items-center justify-between border-t border-gray-100 px-5 py-4">
              <Button variant="outline" size="sm" :disabled="page === 0 || loading" @click="previousPage">
                <ChevronLeft class="h-4 w-4" />
                Anterior
              </Button>
              <span class="text-xs text-gray-500">{{ page + 1 }} de {{ Math.max(totalPages, 1) }}</span>
              <Button variant="outline" size="sm" :disabled="page >= totalPages - 1 || loading" @click="nextPage">
                Próxima
                <ChevronRight class="h-4 w-4" />
              </Button>
            </div>
          </Card>
      </section>
    </div>

    <TaskModal
      v-model:form="form"
      :is-open="isTaskModalOpen"
      :clients="clients"
      :is-submitting="submitting"
      :loading-clients="loadingClients"
      @close="isTaskModalOpen = false"
      @save="submitTask"
    />

    <TaskDetailsModal :task="selectedTask" @close="selectedTask = null" />
  </AppLayout>
</template>
