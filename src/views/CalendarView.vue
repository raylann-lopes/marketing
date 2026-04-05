<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { useRoute } from 'vue-router'
import { ChevronLeft, ChevronRight, Plus, X, Pencil, ChevronDown } from 'lucide-vue-next'
import AppLayout from '@/components/layout/AppLayout.vue'
import Button from '@/components/ui/Button.vue'
import { postService, type Post } from '@/services/postService'
import { clientService, type Client } from '@/services/clientService'
import { getCurrentUserId } from '@/lib/api'
import { z } from 'zod'

const route = useRoute()
const today = new Date()

// Inicializa com a data passada via query param (ex: ?date=2026-04-07) ou hoje
const initialDate = (() => {
  const q = route.query.date
  if (q && typeof q === 'string') {
    const d = new Date(q + 'T12:00:00')
    if (!isNaN(d.getTime())) return d
  }
  return new Date(today)
})()

const currentDate = ref(initialDate)
const selectedDay = ref(initialDate.getDate())
const sidebarOpen = ref(true)

const allPosts = ref<Post[]>([])
const clients = ref<Client[]>([])
const isModalOpen = ref(false)
const isSubmitting = ref(false)
const fieldErrors = ref<Record<string, string>>({})

const newPost = ref({
  clientId: '',
  title: '',
  theme: '',
  objective: '',
  status: 'DEMAND',
  scheduledAt: new Date().toISOString().slice(0, 16)
})

const postSchema = z.object({
  clientId: z.union([z.string(), z.number()]).refine(v => String(v).length > 0, 'Selecione um cliente'),
  title: z.string().min(3, 'Título muito curto'),
  theme: z.string().min(3, 'Tema obrigatório'),
  objective: z.string().min(5, 'Descreva o objetivo'),
  scheduledAt: z.string().min(10, 'Data inválida')
})

const monthNames = ['Janeiro','Fevereiro','Março','Abril','Maio','Junho','Julho','Agosto','Setembro','Outubro','Novembro','Dezembro']
const dayNames = ['DOM', 'SEG', 'TER', 'QUA', 'QUI', 'SEX', 'SÁB']

const currentMonth = computed(() => monthNames[currentDate.value.getMonth()])
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
  
  newPost.value = {
    clientId: '',
    title: '',
    theme: '',
    objective: '',
    status: 'DEMAND',
    scheduledAt: date.toISOString().slice(0, 16)
  }
  fieldErrors.value = {}
  isModalOpen.value = true
}

async function handleCreatePost() {
  fieldErrors.value = {}
  const result = postSchema.safeParse(newPost.value)
  
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
      title: newPost.value.title,
      theme: newPost.value.theme,
      objective: newPost.value.objective,
      status: newPost.value.status,
      scheduledAt: newPost.value.scheduledAt + ":00",
      client: { id: Number(newPost.value.clientId) },
      user: { id: getCurrentUserId() }
    }
    await postService.create(payload as unknown as Post)
    await fetchInitialData()
    isModalOpen.value = false
  } catch (e: unknown) {
    alert('Erro ao criar post: ' + (e instanceof Error ? e.message : 'Erro desconhecido'))
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

type SelectedDayPost = {
  time: string | null
  tag: string
  tagColor: string
  title: string
  description: string
  client: string | null
  status: string | null
}

const selectedDayPosts = computed<SelectedDayPost[]>(() => {
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
        <div class="grid grid-cols-7 gap-px bg-gray-100 border border-gray-100 rounded-lg overflow-hidden flex-1">
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
      <transition name="slide">
        <div v-if="sidebarOpen && selectedDay" class="w-80 bg-white rounded-xl border border-gray-100 shadow-sm flex flex-col shrink-0">
          <div class="flex items-start justify-between p-5 border-b border-gray-100">
            <div>
              <p class="text-xs text-gray-400 uppercase tracking-wide">Agenda do Dia</p>
              <p class="text-4xl font-bold text-gray-900">{{ selectedDay }} {{ currentMonth?.substring(0,3) }}</p>
            </div>
            <button class="p-1.5 hover:bg-gray-100 rounded-lg" @click="sidebarOpen = false">
              <X class="w-4 h-4 text-gray-500" />
            </button>
          </div>

          <div class="flex-1 overflow-y-auto p-4 space-y-3">
            <div v-if="selectedDayPosts.length === 0" class="flex flex-col items-center justify-center h-40 text-center text-gray-400">
              <Plus class="w-8 h-8 mb-2 opacity-20" />
              <p class="text-sm">Nenhum post agendado para este dia.</p>
            </div>
            <div
              v-for="(post, i) in selectedDayPosts"
              :key="i"
              class="bg-gray-50 rounded-xl p-4 space-y-2 border border-transparent hover:border-primary/20 transition-all"
            >
              <div class="flex items-center justify-between">
                <span :class="['text-[10px] font-bold px-2 py-0.5 rounded-full uppercase', post.tagColor]">{{ post.tag }}</span>
                <span v-if="post.time" class="text-xs font-semibold text-primary">{{ post.time }}</span>
              </div>
              <p class="text-sm font-semibold text-gray-800 leading-snug">{{ post.title }}</p>
              <p class="text-xs text-gray-500 line-clamp-2">{{ post.description }}</p>
              <p v-if="post.client" class="text-[10px] text-gray-400 font-medium">● {{ post.client }}</p>
            </div>
          </div>

          <div class="p-4 border-t border-gray-100">
            <Button class="w-full gap-2" @click="openPostModal(selectedDay)">
              <Pencil class="w-4 h-4" />
              Adicionar Novo no Dia
            </Button>
          </div>
        </div>
      </transition>
    </div>

    <!-- FAB -->
    <button
      @click="openPostModal(null)"
      class="fixed bottom-8 right-8 w-14 h-14 bg-primary text-white rounded-full shadow-lg flex items-center justify-center hover:bg-primary/90 hover:scale-110 transition-all z-40"
    >
      <Plus class="w-6 h-6" />
    </button>

    <!-- Modal Novo Post -->
    <div v-if="isModalOpen" class="fixed inset-0 z-50 flex items-center justify-center p-4 bg-black/50 backdrop-blur-sm">
      <div class="bg-white rounded-2xl shadow-xl w-full max-w-lg overflow-hidden animate-in fade-in zoom-in duration-200">
        <div class="flex items-center justify-between p-6 border-b border-gray-100">
          <h2 class="text-xl font-bold text-gray-900">Novo Projeto / Post</h2>
          <button @click="isModalOpen = false" class="p-2 hover:bg-gray-100 rounded-lg text-gray-400"><X class="w-5 h-5" /></button>
        </div>
        
        <div class="p-6 space-y-4">
          <div class="space-y-1.5">
            <label class="text-xs font-semibold text-gray-500 uppercase">Cliente</label>
            <div class="relative">
              <select v-model="newPost.clientId" :class="['w-full p-2.5 pr-10 rounded-lg border bg-gray-50 text-sm focus:outline-none focus:ring-2 focus:ring-primary/20 focus:border-primary appearance-none cursor-pointer', fieldErrors.clientId ? 'border-red-500' : 'border-gray-200']">
                <option value="">Selecione o cliente</option>
                <option v-for="c in clients" :key="c.id" :value="c.id">{{ c.name }}</option>
              </select>
              <ChevronDown class="absolute right-3 top-1/2 -translate-y-1/2 w-4 h-4 text-gray-400 pointer-events-none" />
            </div>
            <p v-if="fieldErrors.clientId" class="text-[10px] text-red-500 font-medium">{{ fieldErrors.clientId }}</p>
          </div>

          <div class="space-y-1.5">
            <label class="text-xs font-semibold text-gray-500 uppercase">Título do Post</label>
            <input v-model="newPost.title" type="text" :class="['w-full p-2.5 rounded-lg border bg-gray-50 focus:outline-none focus:ring-2 focus:ring-primary/20 focus:border-primary text-sm', fieldErrors.title ? 'border-red-500' : 'border-gray-200']" placeholder="Ex: Lançamento Coleção Inverno" />
            <p v-if="fieldErrors.title" class="text-[10px] text-red-500 font-medium">{{ fieldErrors.title }}</p>
          </div>

          <div class="grid grid-cols-2 gap-4">
            <div class="space-y-1.5">
              <label class="text-xs font-semibold text-gray-500 uppercase">Tema</label>
              <input v-model="newPost.theme" type="text" :class="['w-full p-2.5 rounded-lg border bg-gray-50 focus:outline-none focus:ring-2 focus:ring-primary/20 focus:border-primary text-sm', fieldErrors.theme ? 'border-red-500' : 'border-gray-200']" placeholder="Ex: Moda / Lifestyle" />
              <p v-if="fieldErrors.theme" class="text-[10px] text-red-500 font-medium">{{ fieldErrors.theme }}</p>
            </div>
            <div class="space-y-1.5">
              <label class="text-xs font-semibold text-gray-500 uppercase">Data e Hora</label>
              <input v-model="newPost.scheduledAt" type="datetime-local" :class="['w-full p-2.5 rounded-lg border bg-gray-50 focus:outline-none focus:ring-2 focus:ring-primary/20 focus:border-primary text-sm', fieldErrors.scheduledAt ? 'border-red-500' : 'border-gray-200']" />
              <p v-if="fieldErrors.scheduledAt" class="text-[10px] text-red-500 font-medium">{{ fieldErrors.scheduledAt }}</p>
            </div>
          </div>

          <div class="space-y-1.5">
            <label class="text-xs font-semibold text-gray-500 uppercase">Objetivo / Descrição</label>
            <textarea v-model="newPost.objective" rows="3" :class="['w-full p-2.5 rounded-lg border bg-gray-50 focus:outline-none focus:ring-2 focus:ring-primary/20 focus:border-primary text-sm', fieldErrors.objective ? 'border-red-500' : 'border-gray-200']" placeholder="Descreva o objetivo criativo deste post..."></textarea>
            <p v-if="fieldErrors.objective" class="text-[10px] text-red-500 font-medium">{{ fieldErrors.objective }}</p>
          </div>
        </div>

        <div class="p-6 bg-gray-50 border-t border-gray-100 flex gap-3">
          <Button variant="outline" class="flex-1" @click="isModalOpen = false">Cancelar</Button>
          <Button class="flex-1" :disabled="isSubmitting" @click="handleCreatePost">
            {{ isSubmitting ? 'Agendando...' : 'Criar e Agendar' }}
          </Button>
        </div>
      </div>
    </div>
  </AppLayout>
</template>

<style scoped>
.slide-enter-active, .slide-leave-active { transition: all 0.2s ease; }
.slide-enter-from, .slide-leave-to { opacity: 0; transform: translateX(20px); }
</style>
