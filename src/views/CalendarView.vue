<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { useRoute } from 'vue-router'
import { ChevronLeft, ChevronRight, Plus } from 'lucide-vue-next'
import AppLayout from '@/components/layout/AppLayout.vue'
import { postService, type Post } from '@/services/postService'
import { clientService, type Client } from '@/services/clientService'
import { getCurrentUserId } from '@/lib/api'
import { getErrorMessage } from '@/lib/errors'
import { useFeedback } from '@/lib/feedback'
import { z } from 'zod'

import CalendarSidebar from '@/components/calendar/CalendarSidebar.vue'
import PostModal from '@/components/board/PostModal.vue'

const route = useRoute()
const today = new Date()
const feedback = useFeedback()

const initialDate = (() => {
  const q = route.query.date
  if (q && typeof q === 'string') {
    const d = new Date(q + 'T12:00:00')
    if (!isNaN(d.getTime())) return d
  }
  return new Date(today)
})()

const currentDate = ref(initialDate)
const selectedDay = ref<number | null>(initialDate.getDate())
const sidebarOpen = ref(true)

const allPosts = ref<Post[]>([])
const clients = ref<Client[]>([])
const isModalOpen = ref(false)
const isSubmitting = ref(false)
const fieldErrors = ref<Record<string, string>>({})
const postToEdit = ref<Post | null>(null)

const postSchema = z.object({
  clientId: z.union([z.string(), z.number()]).refine(v => String(v).length > 0, 'Selecione um cliente'),
  title: z.string().min(3, 'Título muito curto'),
  theme: z.string().min(3, 'Tema obrigatório'),
  objective: z.string().min(5, 'Descreva o objetivo'),
  scheduledAt: z.string().min(10, 'Data inválida')
})

const monthNames = ['Janeiro','Fevereiro','Março','Abril','Maio','Junho','Julho','Agosto','Setembro','Outubro','Novembro','Dezembro']
const dayNames = ['DOM', 'SEG', 'TER', 'QUA', 'QUI', 'SEX', 'SÁB']

const currentMonth = computed(() => monthNames[currentDate.value.getMonth()] || '')
const currentYear = computed(() => currentDate.value.getFullYear())

async function fetchInitialData() {
  try {
    const [postsData, clientsData] = await Promise.all([
      postService.getAll(),
      clientService.getAll()
    ])
    
    interface ApiResponse<T> { data?: T[] }
    allPosts.value = Array.isArray(postsData) ? postsData : (((postsData as unknown) as ApiResponse<Post>).data || [])
    clients.value = Array.isArray(clientsData) ? clientsData : (((clientsData as unknown) as ApiResponse<Client>).data || [])
  } catch (error) {
    console.error('Failed to fetch calendar data:', error)
  }
}

function openPostModal(day?: number | null) {
  const date = new Date(currentDate.value)
  if (day) date.setDate(day)
  
  postToEdit.value = {
    clientId: '',
    title: '',
    theme: '',
    objective: '',
    status: 'DEMAND',
    scheduledAt: date.toISOString()
  } as unknown as Post
  fieldErrors.value = {}
  isModalOpen.value = true
}

async function handleCreatePost(form: any) {
  fieldErrors.value = {}
  const result = postSchema.safeParse(form)
  
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
      title: form.title,
      theme: form.theme,
      objective: form.objective,
      status: form.status,
      scheduledAt: form.scheduledAt.length === 16 ? form.scheduledAt + ":00" : form.scheduledAt,
      client: { id: Number(form.clientId) },
      user: { id: getCurrentUserId() }
    }
    await postService.create(payload as unknown as Post)
    await fetchInitialData()
    isModalOpen.value = false
    feedback.success('Post criado e agendado com sucesso.')
  } catch (e: unknown) {
    feedback.error(`Erro ao criar post: ${getErrorMessage(e)}`)
  } finally {
    isSubmitting.value = false
  }
}

onMounted(() => {
  fetchInitialData()
})

function prevMonth() {
  currentDate.value = new Date(currentDate.value.getFullYear(), currentDate.value.getMonth() - 1)
}
function nextMonth() {
  currentDate.value = new Date(currentDate.value.getFullYear(), currentDate.value.getMonth() + 1)
}

const calendarDays = computed<(number | null)[]>(() => {
  const year = currentDate.value.getFullYear()
  const month = currentDate.value.getMonth()
  const firstDay = new Date(year, month, 1).getDay()
  const daysInMonth = new Date(year, month + 1, 0).getDate()
  const days: (number | null)[] = []
  for (let i = 0; i < firstDay; i++) days.push(null)
  for (let d = 1; d <= daysInMonth; d++) days.push(d)
  return days
})

type DayPost = {
  label: string
  color: string
}

const getStatusColor = (status: string) => {
  switch (status) {
    case 'DEMAND': return 'bg-gray-400'
    case 'IN_PRODUCTION': return 'bg-blue-500'
    case 'WAITING_APPROVAL': return 'bg-orange-400'
    case 'FINISHED': return 'bg-green-500'
    case 'PUBLISHED': return 'bg-purple-600'
    case 'SCHEDULE': return 'bg-indigo-500'
    default: return 'bg-purple-400'
  }
}

const postsPerDay = computed<Record<number, DayPost[]>>(() => {
  const map: Record<number, DayPost[]> = {}
  const year = currentDate.value.getFullYear()
  const month = currentDate.value.getMonth()
  
  allPosts.value.forEach(post => {
    if (!post.scheduledAt) return
    const d = new Date(post.scheduledAt)
    if (d.getFullYear() === year && d.getMonth() === month) {
      const day = d.getDate()
      if (!map[day]) map[day] = []
      map[day].push({ label: post.title, color: getStatusColor(post.status) })
    }
  })
  return map
})

const selectedDayPosts = computed(() => {
  if (!selectedDay.value) return []
  const year = currentDate.value.getFullYear()
  const month = currentDate.value.getMonth()
  
  return allPosts.value
    .filter(p => {
      if (!p.scheduledAt) return false
      const d = new Date(p.scheduledAt)
      return d.getFullYear() === year && d.getMonth() === month && d.getDate() === selectedDay.value
    })
    .map(p => {
      const d = new Date(p.scheduledAt!)
      const time = `${String(d.getHours()).padStart(2, '0')}:${String(d.getMinutes()).padStart(2, '0')}`
      const rawClientId = (p as unknown as { client: { id: number } }).client?.id ?? p.clientId
      const clientObj = clients.value.find(c => String(c.id) === String(rawClientId))
      const clientName = clientObj ? clientObj.name : (rawClientId ? `ID: ${rawClientId}` : null)
      return {
        time,
        tag: p.status,
        tagColor: 'bg-purple-100 text-purple-700',
        title: p.title,
        description: p.objective,
        client: clientName,
        status: null,
      }
    })
})
</script>

<template>
  <AppLayout topbar-placeholder="Buscar no calendário...">
    <div class="flex gap-6 h-[calc(100vh-140px)]">

      <!-- Calendar -->
      <div class="flex-1 bg-white rounded-xl border border-gray-100 shadow-sm p-5 flex flex-col">
        <!-- Header -->
        <div class="flex items-center justify-between mb-5">
          <h1 class="text-2xl font-bold text-gray-900">{{ currentMonth }}, {{ currentYear }}</h1>
          <div class="flex items-center gap-2">
            <button 
              class="px-3 py-1 text-sm border border-purple-300 text-purple-700 rounded-full hover:bg-purple-50"
              @click="currentDate = new Date(); selectedDay = today.getDate()"
            >
              HOJE
            </button>
            <button class="p-1.5 hover:bg-gray-100 rounded-lg" @click="prevMonth"><ChevronLeft class="w-4 h-4" /></button>
            <button class="p-1.5 hover:bg-gray-100 rounded-lg" @click="nextMonth"><ChevronRight class="w-4 h-4" /></button>
          </div>
        </div>

        <!-- Day names -->
        <div class="grid grid-cols-7 text-center mb-2">
          <div v-for="d in dayNames" :key="d" class="text-xs font-semibold text-gray-400 py-1">{{ d }}</div>
        </div>

        <!-- Days grid -->
        <div class="grid grid-cols-7 gap-px bg-gray-200 border border-gray-200 rounded-lg overflow-hidden flex-1">
          <div
            v-for="(day, i) in calendarDays"
            :key="i"
            :class="[
              'bg-white p-1.5 cursor-pointer hover:bg-purple-50 transition-colors flex flex-col',
              day === selectedDay ? 'ring-2 ring-inset ring-primary' : ''
            ]"
            @click="if(day) { selectedDay = day; sidebarOpen = true }"
          >
            <div
              v-if="day"
              :class="[
                'w-7 h-7 flex items-center justify-center rounded-full text-sm font-medium mb-1',
                (day === today.getDate() && currentDate.getMonth() === today.getMonth()) ? 'bg-primary text-white' : 'text-gray-700'
              ]"
            >
              {{ day }}
            </div>
            <div v-if="day && postsPerDay[day]" class="space-y-0.5 overflow-y-auto custom-scrollbar">
              <div
                v-for="post in postsPerDay[day].slice(0,3)"
                :key="post.label"
                :class="['text-[10px] text-white px-1.5 py-0.5 rounded truncate leading-tight', post.color]"
              >
                {{ post.label }}
              </div>
              <div v-if="postsPerDay[day].length > 3" class="text-[10px] text-gray-400 pl-1">
                +{{ postsPerDay[day].length - 3 }} mais
              </div>
            </div>
          </div>
        </div>
      </div>

      <!-- Day sidebar -->
      <CalendarSidebar
        :is-open="sidebarOpen"
        :selected-day="selectedDay"
        :current-month="currentMonth"
        :posts="selectedDayPosts"
        @close="sidebarOpen = false"
        @open-post-modal="openPostModal"
      />
    </div>

    <!-- FAB -->
    <button
      @click="openPostModal(null)"
      class="fixed bottom-8 right-8 w-14 h-14 bg-primary text-white rounded-full shadow-lg flex items-center justify-center hover:bg-primary/90 hover:scale-110 transition-all z-40"
    >
      <Plus class="w-6 h-6" />
    </button>

    <PostModal
      :is-open="isModalOpen"
      :post-to-edit="postToEdit?.title ? postToEdit : null"
      :clients="clients"
      :is-submitting="isSubmitting"
      :field-errors="fieldErrors"
      @close="isModalOpen = false"
      @save="handleCreatePost"
    />
  </AppLayout>
</template>
