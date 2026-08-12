<script setup lang="ts">
import { computed } from 'vue'
import { useRouter } from 'vue-router'
import { CalendarDays, ChevronLeft, ChevronRight, Clock3 } from 'lucide-vue-next'
import Card from '@/components/ui/Card.vue'
import Badge from '@/components/ui/Badge.vue'
import { type Post } from '@/services/postService'
import { type TaskRecord } from '@/services/taskService'
import { formatTime } from '@/lib/dateTime'

const props = defineProps<{
  selectedWeekStart: Date
  selectedWeekDate: Date
  today: Date
  posts: Post[]
  tasks: TaskRecord[]
}>()

defineEmits<{
  (e: 'prevWeek'): void
  (e: 'nextWeek'): void
  (e: 'selectDay', date: Date): void
}>()

const router = useRouter()
const weekDays = ['SEG', 'TER', 'QUA', 'QUI', 'SEX', 'SÁB', 'DOM']

const visibleWeekDates = computed(() => Array.from({ length: 7 }, (_, index) => {
  const date = new Date(props.selectedWeekStart)
  date.setDate(date.getDate() + index)
  return date
}))

const weekNavLabel = computed(() => {
  const end = new Date(props.selectedWeekStart)
  end.setDate(end.getDate() + 6)
  const startLabel = props.selectedWeekStart.toLocaleDateString('pt-BR', { day: '2-digit', month: 'short' })
  const endLabel = end.toLocaleDateString('pt-BR', { day: '2-digit', month: 'short', year: 'numeric' })
  return `${startLabel} — ${endLabel}`
})

const selectedDayItems = computed(() => {
  const posts = props.posts
    .filter(post => post.scheduledAt && isSameDay(new Date(post.scheduledAt), props.selectedWeekDate))
    .map(post => ({
      id: `post-${post.id}`,
      title: post.title,
      subtitle: 'Post',
      time: formatTime(post.scheduledAt) ?? '--:--',
      status: statusLabel[post.status] || post.status,
      variant: post.status === 'PUBLISHED' ? 'success' as const : 'purple' as const,
      route: '/calendar',
    }))

  const tasks = props.tasks
    .filter(task => task.dateExpires === toDateKey(props.selectedWeekDate))
    .map(task => ({
      id: `task-${task.id}`,
      title: task.title,
      subtitle: task.clientName || 'Tarefa',
      time: formatTime(task.timeExpires) ?? '--:--',
      status: task.priority === 'URGENT' ? 'Urgente' : 'Tarefa',
      variant: task.priority === 'URGENT' || task.priority === 'HIGH' ? 'warning' as const : 'outline' as const,
      route: '/tasks',
    }))

  return [...posts, ...tasks].sort((a, b) => a.time.localeCompare(b.time))
})

const selectedDayLabel = computed(() => {
  if (isSameDay(props.selectedWeekDate, props.today)) return 'Agenda de hoje'
  return props.selectedWeekDate.toLocaleDateString('pt-BR', {
    weekday: 'long', day: '2-digit', month: 'long',
  })
})

const statusLabel: Record<string, string> = {
  DEMAND: 'Demanda',
  IN_PRODUCTION: 'Em produção',
  WAITING_APPROVAL: 'Em aprovação',
  FINISHED: 'Finalizado',
  SCHEDULE: 'Agendado',
  PUBLISHED: 'Publicado',
  REJECTED: 'Revisão',
}

function isSameDay(first: Date, second: Date) {
  return toDateKey(first) === toDateKey(second)
}

function toDateKey(date: Date) {
  return `${date.getFullYear()}-${String(date.getMonth() + 1).padStart(2, '0')}-${String(date.getDate()).padStart(2, '0')}`
}

function dayItemCount(date: Date) {
  const postCount = props.posts.filter(post => post.scheduledAt && isSameDay(new Date(post.scheduledAt), date)).length
  const taskCount = props.tasks.filter(task => task.dateExpires === toDateKey(date)).length
  return postCount + taskCount
}
</script>

<template>
  <Card class="rounded-lg p-5">
    <div class="mb-4 flex items-start justify-between gap-3">
      <div>
        <h2 class="font-semibold text-gray-900">Agenda semanal</h2>
        <p class="mt-0.5 text-xs text-gray-500">Publicações e tarefas no mesmo calendário.</p>
      </div>
      <div class="flex shrink-0 items-center gap-1">
        <button title="Semana anterior" class="rounded p-1 hover:bg-gray-100" @click="$emit('prevWeek')">
          <ChevronLeft class="h-4 w-4 text-gray-500" />
        </button>
        <span class="px-1 text-[10px] font-medium text-gray-400">{{ weekNavLabel }}</span>
        <button title="Próxima semana" class="rounded p-1 hover:bg-gray-100" @click="$emit('nextWeek')">
          <ChevronRight class="h-4 w-4 text-gray-500" />
        </button>
      </div>
    </div>

    <div class="grid grid-cols-7 gap-1 text-center">
      <div v-for="(day, index) in weekDays" :key="day" class="space-y-1">
        <p class="text-[10px] text-gray-400">{{ day }}</p>
        <button
          :class="[
            'relative flex aspect-square w-full items-center justify-center rounded-full text-sm font-medium transition-colors',
            isSameDay(visibleWeekDates[index]!, selectedWeekDate)
              ? 'bg-primary text-white shadow-sm'
              : isSameDay(visibleWeekDates[index]!, today)
                ? 'ring-2 ring-primary text-primary'
                : 'text-gray-600 hover:bg-gray-100',
          ]"
          @click="$emit('selectDay', visibleWeekDates[index]!)"
        >
          {{ visibleWeekDates[index]!.getDate() }}
          <span
            v-if="dayItemCount(visibleWeekDates[index]!) > 0"
            :class="[
              'absolute -right-0.5 -top-0.5 flex h-4 min-w-4 items-center justify-center rounded-full px-1 text-[9px] font-bold',
              isSameDay(visibleWeekDates[index]!, selectedWeekDate)
                ? 'bg-white text-primary'
                : 'bg-violet-100 text-violet-700',
            ]"
          >
            {{ dayItemCount(visibleWeekDates[index]!) }}
          </span>
        </button>
      </div>
    </div>

    <div class="mt-5 border-t border-gray-100 pt-4">
      <p class="mb-3 text-xs font-semibold capitalize text-gray-500">{{ selectedDayLabel }}</p>
      <div v-if="selectedDayItems.length === 0" class="py-5 text-center">
        <CalendarDays class="mx-auto h-6 w-6 text-gray-300" />
        <p class="mt-2 text-xs text-gray-400">Nenhum compromisso neste dia.</p>
      </div>
      <div v-else class="max-h-[250px] space-y-2 overflow-y-auto pr-1">
        <button
          v-for="item in selectedDayItems"
          :key="item.id"
          class="flex w-full gap-3 rounded-md border-l-2 border-primary px-2 py-1.5 text-left hover:bg-gray-50"
          @click="router.push(item.route)"
        >
          <div class="flex w-12 shrink-0 items-center gap-1 text-[11px] font-semibold text-gray-500">
            <Clock3 class="h-3 w-3" /> {{ item.time }}
          </div>
          <div class="min-w-0 flex-1">
            <div class="flex items-start justify-between gap-2">
              <p class="truncate text-xs font-semibold text-gray-800">{{ item.title }}</p>
              <Badge :variant="item.variant" class="shrink-0 px-1.5 text-[9px]">{{ item.status }}</Badge>
            </div>
            <p class="mt-0.5 truncate text-[10px] text-gray-400">{{ item.subtitle }}</p>
          </div>
        </button>
      </div>
    </div>

    <button
      class="mt-4 w-full rounded-md border border-gray-200 py-2 text-sm text-gray-600 transition-colors hover:bg-gray-50"
      @click="router.push('/calendar')"
    >
      Ver calendário completo
    </button>
  </Card>
</template>
