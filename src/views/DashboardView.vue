<script setup lang="ts">
import { ref, onMounted, computed } from 'vue'
import { useRouter } from 'vue-router'
import {
  Users, FileText, Clock, CheckCircle2,
  UserPlus, Plus, ChevronLeft, ChevronRight,
} from 'lucide-vue-next'
import AppLayout from '@/components/layout/AppLayout.vue'
import Button from '@/components/ui/Button.vue'
import Card from '@/components/ui/Card.vue'
import Badge from '@/components/ui/Badge.vue'
import { clientService, type Client } from '@/services/clientService'
import { postService, type Post } from '@/services/postService'

const router = useRouter()
const today = new Date()

const totalClients = ref(0)
const postsThisMonth = ref(0)
const pendingPosts = ref(0)
const publishedPosts = ref(0)
const allPosts = ref<Post[]>([])
const allClients = ref<Client[]>([])
const recentPosts = ref<Post[]>([])

// Week navigation
const selectedWeekStart = ref(
  (() => { const d = new Date(today); d.setDate(d.getDate() - (d.getDay() === 0 ? 6 : d.getDay() - 1)); return d })()
)

const visibleWeekDates = computed(() => {
  const dates: Date[] = []
  for (let i = 0; i < 7; i++) {
    const d = new Date(selectedWeekStart.value)
    d.setDate(d.getDate() + i)
    dates.push(d)
  }
  return dates
})

const weekNavLabel = computed(() => {
  const start = selectedWeekStart.value
  const end = new Date(selectedWeekStart.value)
  end.setDate(end.getDate() + 6)
  const m1 = start.toLocaleDateString('pt-BR', { day: '2-digit', month: 'short' })
  const m2 = end.toLocaleDateString('pt-BR', { day: '2-digit', month: 'long', year: 'numeric' })
  return `${m1} — ${m2}`
})

// Dia selecionado no cronograma (inicia com hoje)
const selectedWeekDate = ref(new Date(today))

const weekDays = ['SEG', 'TER', 'QUA', 'QUI', 'SEX', 'SÁB', 'DOM']

function isSameDay(a: Date, b: Date) {
  return a.getDate() === b.getDate() &&
    a.getMonth() === b.getMonth() &&
    a.getFullYear() === b.getFullYear()
}

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

const selectedDayLabel = computed(() => {
  if (isSameDay(selectedWeekDate.value, today)) return 'Publicações de Hoje'
  return `Publicações de ${selectedWeekDate.value.toLocaleDateString('pt-BR', { weekday: 'short', day: '2-digit', month: 'short' })}`
})

const selectedDayPosts = computed(() =>
  allPosts.value.filter(p => {
    if (!p.scheduledAt) return false
    return isSameDay(new Date(p.scheduledAt), selectedWeekDate.value)
  })
)

function getClientName(clientId: string | number | undefined) {
  if (!clientId) return '—'
  const client = allClients.value.find(c => String(c.id) === String(clientId))
  return client ? client.name : `ID: ${clientId}`
}

async function fetchDashboardData() {
  try {
    const [clientsData, postsData] = await Promise.all([
      clientService.getAll(),
      postService.getAll()
    ])

    const clients = Array.isArray(clientsData) ? clientsData : ((clientsData as { data: Client[] }).data || [])
    const posts = Array.isArray(postsData) ? postsData : ((postsData as { data: Post[] }).data || [])

    allClients.value = clients
    allPosts.value = posts

    totalClients.value = clients.filter((c: Client) => c.status === 'ACTIVE').length

    const monthPosts = posts.filter((p: Post) => {
      if (!p.scheduledAt) return false
      const d = new Date(p.scheduledAt)
      return d.getMonth() === today.getMonth() && d.getFullYear() === today.getFullYear()
    })

    postsThisMonth.value = monthPosts.length
    pendingPosts.value = posts.filter((p: Post) =>
      p.status !== 'PUBLISHED' && p.status !== 'FINISHED'
    ).length
    publishedPosts.value = posts.filter((p: Post) => p.status === 'PUBLISHED').length

    recentPosts.value = [...posts]
      .sort((a, b) => new Date(b.scheduledAt).getTime() - new Date(a.scheduledAt).getTime())
      .slice(0, 8)

  } catch (error) {
    console.error('Error fetching dashboard data:', error)
  }
}

const statusLabel: Record<string, string> = {
  DEMAND: 'Demanda',
  IN_PRODUCTION: 'Em Produção',
  WAITING_APPROVAL: 'Aguard. Aprovação',
  FINISHED: 'Finalizado',
  PUBLISHED: 'Publicado',
}

const statusVariant = computed(() => (status: string) => {
  const map: Record<string, string> = {
    DEMAND: 'secondary',
    IN_PRODUCTION: 'default',
    WAITING_APPROVAL: 'warning',
    FINISHED: 'success',
    PUBLISHED: 'purple',
  }
  return (map[status] || 'outline') as 'secondary' | 'default' | 'warning' | 'success' | 'purple' | 'outline'
})

onMounted(() => {
  fetchDashboardData()
})
</script>

<template>
  <AppLayout>
    <!-- Header -->
    <div class="flex items-start justify-between mb-6">
      <div>
        <h1 class="text-3xl font-bold text-gray-900">Bom dia, North.</h1>
        <p class="text-gray-500 mt-1">Aqui está o que está acontecendo no ateliê hoje.</p>
      </div>
      <div class="flex items-center gap-3">
        <Button variant="outline" class="gap-2" @click="router.push('/clients')">
          <UserPlus class="w-4 h-4" />
          Novo Cliente
        </Button>
        <Button class="gap-2" @click="router.push('/board')">
          <Plus class="w-4 h-4" />
          Novo Post
        </Button>
      </div>
    </div>

    <div class="grid grid-cols-3 gap-6">
      <!-- Left: stats + activity -->
      <div class="col-span-2 space-y-6">

        <!-- Stats -->
        <div class="grid grid-cols-4 gap-4">
          <Card class="p-5 transition-all hover:shadow-md cursor-default">
            <p class="text-xs font-semibold uppercase tracking-wide text-gray-400">Clientes Ativos</p>
            <div class="flex items-baseline gap-2 mt-2">
              <span class="text-3xl font-bold text-gray-900">{{ totalClients }}</span>
              <span class="text-sm font-medium text-green-600"><Users class="w-4 h-4 inline" /></span>
            </div>
          </Card>
          <Card class="p-5 transition-all hover:shadow-md cursor-default">
            <p class="text-xs font-semibold uppercase tracking-wide text-gray-400">Posts Este Mês</p>
            <div class="flex items-baseline gap-2 mt-2">
              <span class="text-3xl font-bold text-gray-900">{{ postsThisMonth }}</span>
              <span class="text-sm font-medium text-gray-400"><FileText class="w-4 h-4 inline" /></span>
            </div>
          </Card>
          <Card class="p-5 transition-all hover:shadow-md cursor-default border-primary ring-1 ring-primary/20">
            <p class="text-xs font-semibold uppercase tracking-wide text-gray-400">Em Andamento</p>
            <div class="flex items-baseline gap-2 mt-2">
              <span class="text-3xl font-bold text-primary">{{ pendingPosts }}</span>
              <span class="text-sm font-medium text-purple-600"><Clock class="w-4 h-4 inline" /></span>
            </div>
          </Card>
          <Card class="p-5 transition-all hover:shadow-md cursor-default">
            <p class="text-xs font-semibold uppercase tracking-wide text-gray-400">Publicados</p>
            <div class="flex items-baseline gap-2 mt-2">
              <span class="text-3xl font-bold text-gray-900">{{ publishedPosts }}</span>
              <span class="text-sm font-medium text-green-600"><CheckCircle2 class="w-4 h-4 inline" /></span>
            </div>
          </Card>
        </div>

        <!-- Recent Posts -->
        <Card class="overflow-hidden">
          <div class="px-5 py-4 border-b border-gray-100 flex items-center justify-between">
            <h2 class="font-semibold text-gray-900">Demandas Recentes</h2>
            <button class="text-xs text-primary hover:underline" @click="router.push('/board')">Ver board →</button>
          </div>
          <div v-if="recentPosts.length === 0" class="py-8 text-center text-sm text-gray-400">Nenhuma demanda cadastrada.</div>
          <table v-else class="w-full">
            <thead>
              <tr class="border-b border-gray-100">
                <th class="text-left px-5 py-3 text-xs font-semibold text-gray-400 uppercase">Título</th>
                <th class="text-left px-5 py-3 text-xs font-semibold text-gray-400 uppercase">Cliente</th>
                <th class="text-left px-5 py-3 text-xs font-semibold text-gray-400 uppercase">Status</th>
                <th class="text-left px-5 py-3 text-xs font-semibold text-gray-400 uppercase">Data</th>
              </tr>
            </thead>
            <tbody>
              <tr
                v-for="post in recentPosts"
                :key="post.id"
                class="border-b border-gray-50 hover:bg-gray-50 transition-colors cursor-pointer"
                @click="router.push('/board')"
              >
                <td class="px-5 py-3 text-sm font-medium text-gray-800 max-w-[180px] truncate">{{ post.title }}</td>
                <td class="px-5 py-3 text-sm text-gray-500">
                  {{ getClientName((post as unknown as { client: { id: number } }).client?.id || post.clientId) }}
                </td>
                <td class="px-5 py-3">
                  <Badge :variant="statusVariant(post.status)" class="text-[10px]">
                    {{ statusLabel[post.status] || post.status }}
                  </Badge>
                </td>
                <td class="px-5 py-3 text-xs text-gray-400">
                  {{ post.scheduledAt ? new Date(post.scheduledAt).toLocaleDateString('pt-BR', { day: '2-digit', month: 'short' }) : '—' }}
                </td>
              </tr>
            </tbody>
          </table>
        </Card>
      </div>

      <!-- Right: Schedule + Health -->
      <div class="space-y-6">

        <!-- Weekly Schedule -->
        <Card class="p-5">
          <div class="flex items-center justify-between mb-4">
            <h2 class="font-semibold text-gray-900">Cronograma Semanal</h2>
            <div class="flex items-center gap-1">
              <button class="p-1 hover:bg-gray-100 rounded" @click="prevWeek"><ChevronLeft class="w-4 h-4 text-gray-500" /></button>
              <span class="text-[10px] text-gray-400 font-medium px-1">{{ weekNavLabel }}</span>
              <button class="p-1 hover:bg-gray-100 rounded" @click="nextWeek"><ChevronRight class="w-4 h-4 text-gray-500" /></button>
            </div>
          </div>

          <!-- Week days -->
          <div class="grid grid-cols-7 gap-1 text-center mb-3">
            <div v-for="(day, i) in weekDays" :key="day" class="space-y-1">
              <p class="text-xs text-gray-400">{{ day }}</p>
              <button
                :class="[
                  'w-full aspect-square rounded-full text-sm font-medium flex items-center justify-center transition-colors relative',
                  isSameDay(visibleWeekDates[i]!, selectedWeekDate)
                    ? 'bg-primary text-white shadow-sm'
                    : isSameDay(visibleWeekDates[i]!, today)
                      ? 'ring-2 ring-primary text-primary'
                      : 'text-gray-600 hover:bg-gray-100'
                ]"
                @click="selectWeekDay(visibleWeekDates[i]!)"
              >
                {{ visibleWeekDates[i]!.getDate() }}
              </button>
            </div>
          </div>

          <!-- Posts do dia selecionado -->
          <div class="mt-4">
            <p class="text-xs font-semibold uppercase tracking-wide text-gray-400 mb-3">{{ selectedDayLabel }}</p>
            <div v-if="selectedDayPosts.length === 0" class="text-sm text-gray-500 text-center py-4">
              Nenhum post agendado para este dia.
            </div>
            <div class="space-y-3">
              <div v-for="post in selectedDayPosts" :key="post.id" class="flex gap-3">
                <div class="w-1 rounded-full bg-primary shrink-0" />
                <div class="flex-1">
                  <div class="flex items-center justify-between">
                    <span class="text-xs font-semibold text-primary">
                      {{ new Date(post.scheduledAt).toLocaleTimeString('pt-BR', { hour: '2-digit', minute: '2-digit' }) }}
                    </span>
                    <Badge variant="outline" class="text-[10px]">{{ statusLabel[post.status] || post.status }}</Badge>
                  </div>
                  <p class="text-sm font-medium text-gray-800 mt-0.5">{{ post.title }}</p>
                  <p class="text-xs mt-0.5 text-gray-500">{{ post.theme }}</p>
                </div>
              </div>
            </div>
          </div>

          <button
            class="w-full mt-4 py-2 text-sm text-gray-600 border border-gray-200 rounded-lg hover:bg-gray-50 transition-colors"
            @click="router.push('/calendar')"
          >
            Ver Calendário Completo
          </button>
        </Card>

        <!-- Client Health -->
        <Card class="p-5 bg-gradient-to-br from-primary to-north-purple-light text-white shadow-lg">
          <h2 class="font-semibold">Saúde dos Clientes</h2>
          <p v-if="allClients.length > 0" class="text-sm text-purple-100 mt-1">
            {{ allClients.filter(c => c.status === 'ACTIVE').length }} de {{ allClients.length }} clientes ativos.
          </p>
          <p v-else class="text-sm text-purple-100 mt-1">Nenhum cliente cadastrado ainda.</p>
          <div class="flex items-center gap-2 mt-4">
            <div class="flex -space-x-2">
              <div
                v-for="i in Math.min(allClients.length, 4)"
                :key="i"
                class="w-8 h-8 rounded-full bg-purple-200 border-2 border-primary flex items-center justify-center text-xs font-bold text-primary"
              >
                {{ allClients[i - 1]?.name?.charAt(0)?.toUpperCase() || '?' }}
              </div>
            </div>
            <span v-if="allClients.length > 4" class="text-sm font-medium bg-white/20 rounded-full px-2 py-0.5">+{{ allClients.length - 4 }}</span>
          </div>
        </Card>
      </div>
    </div>
  </AppLayout>
</template>
