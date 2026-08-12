<script setup lang="ts">
import { ref, onMounted, computed } from 'vue'
import { useRouter } from 'vue-router'
import { CalendarDays, Plus, RefreshCw, UserPlus } from 'lucide-vue-next'
import AppLayout from '@/components/layout/AppLayout.vue'
import Button from '@/components/ui/Button.vue'

import StatCards from '@/components/dashboard/StatCards.vue'
import RecentPosts from '@/components/dashboard/RecentPosts.vue'
import WeeklySchedule from '@/components/dashboard/WeeklySchedule.vue'

import { clientService, type Client } from '@/services/clientService'
import { postService, type Post } from '@/services/postService'
import { taskService, type TaskRecord } from '@/services/taskService'
import { userService } from '@/services/userService'

const router = useRouter()
const today = new Date()
const userName = ref('')
const role = localStorage.getItem('role') || sessionStorage.getItem('role')
const isAdmin = role === 'ADMIN'

const allPosts = ref<Post[]>([])
const allClients = ref<Client[]>([])
const allTasks = ref<TaskRecord[]>([])
const loading = ref(false)
const dashboardError = ref('')

type DashboardMetric = {
  label: string
  value: number
  detail: string
  icon: 'approval' | 'production' | 'schedule' | 'published' | 'clients' | 'tasks' | 'alert'
  tone: 'amber' | 'blue' | 'violet' | 'green' | 'slate' | 'red'
  route?: string
}

const selectedWeekStart = ref(
  (() => {
    const d = new Date(today)
    d.setDate(d.getDate() - (d.getDay() === 0 ? 6 : d.getDay() - 1))
    return d
  })(),
)

const selectedWeekDate = ref(new Date(today))

function selectWeekDay(date: Date) {
  selectedWeekDate.value = date
}

function prevWeek() {
  selectedWeekStart.value = new Date(selectedWeekStart.value)
  selectedWeekStart.value.setDate(selectedWeekStart.value.getDate() - 7)
}

function nextWeek() {
  selectedWeekStart.value = new Date(selectedWeekStart.value)
  selectedWeekStart.value.setDate(selectedWeekStart.value.getDate() + 7)
}

function toDateKey(date: Date) {
  return `${date.getFullYear()}-${String(date.getMonth() + 1).padStart(2, '0')}-${String(date.getDate()).padStart(2, '0')}`
}

const todayKey = toDateKey(today)

const todayLabel = computed(() => {
  const label = today.toLocaleDateString('pt-BR', {
    weekday: 'long',
    day: '2-digit',
    month: 'long',
    year: 'numeric',
  })
  return label.charAt(0).toUpperCase() + label.slice(1)
})

const activeClients = computed(() => allClients.value.filter(client => client.status === 'ACTIVE').length)

const waitingApproval = computed(() =>
  allPosts.value.filter(post => post.status === 'WAITING_APPROVAL').length,
)

const productionQueue = computed(() =>
  allPosts.value.filter(post => ['DEMAND', 'IN_PRODUCTION', 'REJECTED'].includes(post.status)).length,
)

const scheduledNextSevenDays = computed(() => {
  const start = new Date(today.getFullYear(), today.getMonth(), today.getDate())
  const end = new Date(start)
  end.setDate(end.getDate() + 7)

  return allPosts.value.filter(post => {
    if (post.status !== 'SCHEDULE' || !post.scheduledAt) return false
    const scheduledAt = new Date(post.scheduledAt)
    return scheduledAt >= start && scheduledAt < end
  }).length
})

const publishedThisMonth = computed(() =>
  allPosts.value.filter(post => {
    if (post.status !== 'PUBLISHED' || !post.scheduledAt) return false
    const scheduledAt = new Date(post.scheduledAt)
    return scheduledAt.getMonth() === today.getMonth() && scheduledAt.getFullYear() === today.getFullYear()
  }).length,
)

const postsThisMonth = computed(() =>
  allPosts.value.filter(post => {
    if (!post.scheduledAt) return false
    const scheduledAt = new Date(post.scheduledAt)
    return scheduledAt.getMonth() === today.getMonth() && scheduledAt.getFullYear() === today.getFullYear()
  }).length,
)

const tasksToday = computed(() =>
  allTasks.value.filter(task => task.status === 'PENDING' && task.dateExpires === todayKey).length,
)

const overdueTasks = computed(() =>
  allTasks.value.filter(task => task.status === 'PENDING' && task.dateExpires < todayKey).length,
)

const dashboardStats = computed<DashboardMetric[]>(() => {
  const metrics: DashboardMetric[] = [
    {
      label: 'Aguardando aprovação',
      value: waitingApproval.value,
      detail: 'Retorno pendente do cliente',
      icon: 'approval',
      tone: 'amber',
      route: '/approvals',
    },
    {
      label: 'Fila de produção',
      value: productionQueue.value,
      detail: 'Demandas, produção e ajustes',
      icon: 'production',
      tone: 'blue',
      route: '/board',
    },
    {
      label: 'Próximos 7 dias',
      value: scheduledNextSevenDays.value,
      detail: 'Publicações agendadas',
      icon: 'schedule',
      tone: 'violet',
      route: '/calendar',
    },
    {
      label: 'Publicados no mês',
      value: publishedThisMonth.value,
      detail: 'Conteúdos entregues',
      icon: 'published',
      tone: 'green',
      route: '/calendar',
    },
    {
      label: 'Clientes ativos',
      value: activeClients.value,
      detail: 'Contas em operação',
      icon: 'clients',
      tone: 'slate',
      route: isAdmin ? '/clients' : undefined,
    },
  ]

  metrics.push(isAdmin
    ? {
        label: 'Tarefas de hoje',
        value: tasksToday.value,
        detail: overdueTasks.value > 0
          ? `${overdueTasks.value} ${overdueTasks.value === 1 ? 'tarefa atrasada' : 'tarefas atrasadas'}`
          : 'Nenhuma tarefa atrasada',
        icon: overdueTasks.value > 0 ? 'alert' : 'tasks',
        tone: overdueTasks.value > 0 ? 'red' : 'slate',
        route: '/tasks',
      }
    : {
        label: 'Posts no mês',
        value: postsThisMonth.value,
        detail: 'Volume do calendário editorial',
        icon: 'schedule',
        tone: 'slate',
        route: '/calendar',
      })

  return metrics
})

async function fetchDashboardData() {
  loading.value = true
  dashboardError.value = ''
  try {
    const [clientsData, postsData, tasksData] = await Promise.all([
      clientService.getAll(),
      postService.getAll(),
      isAdmin
        ? taskService.getMine(0, 200, { status: 'PENDING' })
        : Promise.resolve(null),
    ])

    const clients = Array.isArray(clientsData)
      ? clientsData
      : (clientsData as { data: Client[] }).data || []
    const posts = Array.isArray(postsData) ? postsData : (postsData as { data: Post[] }).data || []

    allClients.value = clients
    allPosts.value = posts
    allTasks.value = tasksData?.content ?? []
  } catch (error) {
    console.error('Error fetching dashboard data:', error)
    dashboardError.value = 'Não foi possível atualizar todos os indicadores do dashboard.'
  } finally {
    loading.value = false
  }
}

onMounted(async () => {
  await fetchDashboardData()
  try {
    const me = await userService.getMe()
    userName.value = me.name?.split(' ')[0] || 'North'
  } catch {
    userName.value = 'North'
  }
})
</script>

<template>
  <AppLayout>
    <div class="mb-5 flex flex-col gap-4 md:flex-row md:items-start md:justify-between">
      <div class="min-w-0">
        <h1 class="text-2xl md:text-3xl font-bold text-gray-900 truncate">Olá {{ userName }}.</h1>
        <div class="mt-1 flex items-center gap-2 text-sm text-gray-500">
          <CalendarDays class="h-4 w-4 shrink-0" />
          <span>{{ todayLabel }}</span>
        </div>
      </div>
      <div class="flex shrink-0 items-center gap-2 md:gap-3">
        <Button
          variant="outline"
          size="icon"
          title="Atualizar indicadores"
          :disabled="loading"
          @click="fetchDashboardData"
        >
          <RefreshCw :class="['h-4 w-4', loading ? 'animate-spin' : '']" />
        </Button>
        <Button v-if="isAdmin" variant="outline" class="gap-2" @click="router.push('/clients')">
          <UserPlus class="w-4 h-4" />
          <span class="hidden md:inline">Novo Cliente</span>
        </Button>
        <Button class="gap-2" @click="router.push('/board')">
          <Plus class="w-4 h-4" />
          <span class="hidden md:inline">Novo Post</span>
        </Button>
      </div>
    </div>

    <div v-if="dashboardError" class="mb-4 rounded-lg border border-amber-200 bg-amber-50 px-4 py-3 text-sm text-amber-800">
      {{ dashboardError }}
    </div>

    <StatCards :stats="dashboardStats" />

    <div class="mt-6 grid grid-cols-1 gap-6 xl:grid-cols-12">
      <div class="xl:col-span-8">
        <RecentPosts
          :posts="allPosts"
          :tasks="isAdmin ? allTasks : []"
          :clients="allClients"
          :show-tasks="isAdmin"
        />
      </div>

      <div class="space-y-6 xl:col-span-4">
        <WeeklySchedule
          :selected-week-start="selectedWeekStart"
          :selected-week-date="selectedWeekDate"
          :today="today"
          :posts="allPosts"
          :tasks="isAdmin ? allTasks : []"
          @prev-week="prevWeek"
          @next-week="nextWeek"
          @select-day="selectWeekDay"
        />
      </div>
    </div>
  </AppLayout>
</template>
